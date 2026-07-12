from datetime import timedelta
from html import escape
import re

import requests
from django.db import transaction
from django.db.models import Prefetch
from django.utils import timezone

from .models import (
    AIAnalysis,
    AIProviderSettings,
    Comment,
    Project,
    TestCase,
    TestCaseReview,
    TestCaseUserStory,
    TestRun,
    TestRunTestCase,
    TraceabilityMatrix,
    TraceabilityAIReview,
    User,
    UserStory,
)


def build_api_response(success: bool, message: str = "", **payload):
    body = {"success": success}
    if message:
        body["message"] = message
    body.update(payload)
    return body


@transaction.atomic
def generate_and_store_matrix(project_id: int) -> TraceabilityMatrix:
    user_stories = UserStory.objects.filter(section__project_id=project_id).order_by("id")
    test_cases = TestCase.objects.filter(test_suite__project_id=project_id).order_by("id")
    review_scores = {
        row["test_case_id"]: row["overall_score"]
        for row in TestCaseReview.objects.filter(test_case_id__in=test_cases.values_list("id", flat=True)).values(
            "test_case_id",
            "overall_score",
        )
    }
    links = set(
        TestCaseUserStory.objects.filter(
            test_case_id__in=test_cases.values_list("id", flat=True),
            user_story_id__in=user_stories.values_list("id", flat=True),
        ).values_list("test_case_id", "user_story_id")
    )

    html = ["<table class=\"traceability-table\">"]
    html.append("<thead><tr><th class=\"user-story-header\">User Story \\ Test Case</th>")
    for tc in test_cases:
        safe_name = escape(tc.name or "", quote=True)
        tc_link = f"/test-suite/{tc.test_suite_id}?testCaseId={tc.id}"
        score = review_scores.get(tc.id)
        if score is None:
            score_badge = '<div class="tc-ai-score-badge no-score">AI: —</div>'
        elif score <= 4:
            score_badge = f'<div class="tc-ai-score-badge low-score">AI: {score}/10</div>'
        elif score <= 7:
            score_badge = f'<div class="tc-ai-score-badge medium-score">AI: {score}/10</div>'
        else:
            score_badge = f'<div class="tc-ai-score-badge high-score">AI: {score}/10</div>'
        html.append(
            f"<th class=\"test-case-header\">{score_badge}<a href=\"{tc_link}\" class=\"rotated-link\" "
            f"data-test-case-id=\"{tc.id}\" data-bs-toggle=\"popover\" data-bs-trigger=\"hover focus\" "
            f"data-bs-html=\"true\" data-bs-placement=\"auto\" data-bs-title=\"{safe_name}\" "
            f"data-bs-content=\"Загрузка...\">TC-{tc.id}</a></th>"
        )
    html.append("</tr></thead><tbody>")
    for us in user_stories:
        us_name = escape(us.name or "", quote=True)
        criticality = us.business_criticality
        us_label = f"US {us.id}"
        if criticality is None:
            us_cell_class = "user-story-cell"
            criticality_badge = ""
        elif criticality <= 4:
            us_cell_class = "user-story-cell criticality-low"
            criticality_badge = f"<span class=\"badge bg-success ms-2\">{criticality}</span>"
        elif criticality <= 7:
            us_cell_class = "user-story-cell criticality-medium"
            criticality_badge = f"<span class=\"badge bg-warning text-dark ms-2\">{criticality}</span>"
        else:
            us_cell_class = "user-story-cell criticality-high"
            criticality_badge = f"<span class=\"badge bg-danger ms-2\">{criticality}</span>"
        html.append(
            f"<tr class=\"user-story-row\"><td class=\"{us_cell_class}\" title=\"{us_name}\">"
            f"<strong>{us_label}</strong>{criticality_badge}<br>{us_name}</td>"
        )
        for tc in test_cases:
            if (tc.id, us.id) in links:
                html.append("<td class=\"linked-cell\"><span class=\"linked-indicator\">✓</span></td>")
            else:
                html.append("<td class=\"unlinked-cell\"></td>")
        html.append("</tr>")
    html.append("</tbody></table>")

    matrix, _ = TraceabilityMatrix.objects.update_or_create(
        project_id=project_id,
        defaults={"matrix_html": "".join(html), "created_at": timezone.now()},
    )
    return matrix


def calculate_traceability_metrics(project_id: int):
    user_stories = list(UserStory.objects.filter(section__project_id=project_id).only("id", "business_criticality"))
    test_case_ids = list(TestCase.objects.filter(test_suite__project_id=project_id).values_list("id", flat=True))
    user_story_ids = [us.id for us in user_stories]
    linked_pairs = list(
        TestCaseUserStory.objects.filter(
            test_case_id__in=test_case_ids,
            user_story_id__in=user_story_ids,
        ).values_list("test_case_id", "user_story_id")
    )
    linked_user_story_ids = {pair[1] for pair in linked_pairs}
    linked_test_case_ids = {pair[0] for pair in linked_pairs}

    total_us = len(user_stories)
    covered_us = sum(1 for us in user_stories if us.id in linked_user_story_ids)
    uncovered_us = total_us - covered_us
    coverage_percent = round((covered_us * 100.0) / total_us, 2) if total_us else 0.0

    low_stories = [us for us in user_stories if us.business_criticality is not None and 1 <= us.business_criticality <= 4]
    medium_stories = [us for us in user_stories if us.business_criticality is not None and 5 <= us.business_criticality <= 7]
    high_stories = [us for us in user_stories if us.business_criticality is not None and 8 <= us.business_criticality <= 10]

    low_count = len(low_stories)
    medium_count = len(medium_stories)
    high_count = len(high_stories)

    low_covered = sum(1 for us in low_stories if us.id in linked_user_story_ids)
    medium_covered = sum(1 for us in medium_stories if us.id in linked_user_story_ids)
    high_covered = sum(1 for us in high_stories if us.id in linked_user_story_ids)

    orphan_test_cases_count = len(set(test_case_ids) - linked_test_case_ids)

    return {
        "coverage": {
            "total_us": total_us,
            "covered_us": covered_us,
            "uncovered_us": uncovered_us,
            "coverage_percent": coverage_percent,
        },
        "criticality": {
            "low_count": low_count,
            "medium_count": medium_count,
            "high_count": high_count,
            "total_us": total_us,
        },
        "criticality_coverage": {
            "high_covered": high_covered,
            "high_uncovered": high_count - high_covered,
            "medium_covered": medium_covered,
            "medium_uncovered": medium_count - medium_covered,
            "low_covered": low_covered,
            "low_uncovered": low_count - low_covered,
        },
        "orphan_test_cases": {
            "count": orphan_test_cases_count,
        },
    }


def bulk_refresh_matrices():
    for project_id in Project.objects.values_list("id", flat=True):
        generate_and_store_matrix(project_id)


def ensure_default_admin():
    if not User.objects.filter(username="admin").exists():
        User.objects.create(
            username="admin",
            password="admin",
            full_name="Administrator",
            email=None,
            roles=[User.Role.ADMIN],
            enabled=True,
        )


def run_ai_test_suite_analysis(test_suite_id: int):
    test_cases = TestCase.objects.filter(test_suite_id=test_suite_id).order_by("id")
    ai_settings = _get_enabled_yandex_settings()
    summary = None
    if ai_settings:
        prompt = _build_test_suite_analysis_prompt(test_suite_id, list(test_cases))
        summary, _ = _request_yandex_completion(ai_settings, prompt, max_tokens=1200)
    if not summary:
        summary = f"Auto analysis: found {test_cases.count()} test cases in suite {test_suite_id}."
    analysis = AIAnalysis.objects.create(
        test_suite_id=test_suite_id,
        prompt="System-generated analysis prompt",
        ai_response=summary,
    )
    return build_api_response(True, "AI analysis completed", analysis_id=analysis.id, response=summary)


def create_or_update_review(test_case_id: int):
    test_case = TestCase.objects.filter(id=test_case_id).first()
    if not test_case:
        return None
    ai_settings = _get_enabled_yandex_settings()
    if ai_settings:
        result, error_message = _request_yandex_completion(
            ai_settings,
            _build_test_case_review_prompt(test_case),
            max_tokens=1200,
        )
        if not result:
            result = (
                "AI провайдер включен, но не удалось получить ответ. "
                f"{error_message or 'Проверьте настройки подключения.'}"
            )
        score = _extract_review_score(result)
        if score is None:
            score = _fallback_review_score(test_case)
    else:
        result = f"Auto review for test case {test_case_id}: name length={len(test_case.name or '')}"
        score = _fallback_review_score(test_case)
    review, _ = TestCaseReview.objects.update_or_create(
        test_case_id=test_case_id,
        defaults={"review_result": result, "overall_score": score},
    )
    return review


def create_test_run(project_id, payload):
    test_run = TestRun.objects.create(
        project_id=project_id,
        title=payload.get("title") or "New test run",
        description=payload.get("description"),
        executor_name=payload.get("executorName") or "unknown",
        creator_name=payload.get("creatorName") or "unknown",
    )
    test_case_ids = payload.get("testCaseIds") or []
    test_suite_ids = payload.get("testSuiteIds") or []
    if test_suite_ids:
        suite_case_ids = list(
            TestCase.objects.filter(test_suite_id__in=test_suite_ids).values_list("id", flat=True)
        )
        test_case_ids = list(set(test_case_ids + suite_case_ids))
    for tc in TestCase.objects.filter(id__in=test_case_ids):
        TestRunTestCase.objects.get_or_create(test_run=test_run, test_case=tc)
    return test_run


def normalize_status(value: str, allowed: list[str], default: str):
    if not value:
        return default
    normalized = value.strip().upper().replace(" ", "_")
    return normalized if normalized in allowed else default


def analyze_traceability_model_quality(project_id: int, force_refresh: bool = False):
    project = Project.objects.filter(id=project_id).first()
    if not project:
        return build_api_response(False, "Проект не найден")
    existing_review = TraceabilityAIReview.objects.filter(project_id=project_id).first()
    if existing_review and not force_refresh:
        return build_api_response(
            True,
            "Показан сохраненный результат AI-оценки",
            response=existing_review.response,
            reviewed_at=existing_review.reviewed_at,
            cached=True,
        )
    if not existing_review and not force_refresh:
        return build_api_response(False, "Сохраненная AI-оценка не найдена. Нажмите «Перезапросить».")

    user_stories = list(
        UserStory.objects.filter(section__project_id=project_id)
        .select_related("section")
        .order_by("section_id", "id")
    )
    test_cases = list(
        TestCase.objects.filter(test_suite__project_id=project_id)
        .prefetch_related("steps")
        .order_by("id")
    )
    test_case_map = {tc.id: tc for tc in test_cases}

    links = TestCaseUserStory.objects.filter(
        user_story_id__in=[us.id for us in user_stories],
        test_case_id__in=[tc.id for tc in test_cases],
    ).values("user_story_id", "test_case_id")
    us_to_tc_ids: dict[int, list[int]] = {}
    for link in links:
        us_to_tc_ids.setdefault(link["user_story_id"], []).append(link["test_case_id"])

    ai_settings = _get_enabled_yandex_settings()
    if ai_settings:
        prompt = _build_traceability_quality_prompt(project, user_stories, us_to_tc_ids, test_case_map)
        response_text, error_message = _request_yandex_completion(ai_settings, prompt, max_tokens=2200)
        if not response_text:
            return build_api_response(
                False,
                error_message or "Не удалось получить ответ от AI для оценки тестовой модели",
            )
        review, _ = TraceabilityAIReview.objects.update_or_create(
            project_id=project_id,
            defaults={"response": response_text, "reviewed_at": timezone.now()},
        )
        return build_api_response(
            True,
            "AI оценка качества модели сформирована",
            response=review.response,
            reviewed_at=review.reviewed_at,
            cached=False,
        )

    fallback = _build_traceability_quality_fallback(user_stories, us_to_tc_ids, test_case_map)
    review, _ = TraceabilityAIReview.objects.update_or_create(
        project_id=project_id,
        defaults={"response": fallback, "reviewed_at": timezone.now()},
    )
    return build_api_response(
        True,
        "AI провайдер не включен, показана локальная эвристическая оценка",
        response=review.response,
        reviewed_at=review.reviewed_at,
        cached=False,
    )


def test_ai_provider_connection():
    settings = AIProviderSettings.objects.filter(provider=AIProviderSettings.Provider.YANDEX_GPT).first()
    if not settings:
        return build_api_response(False, "Настройки YandexGPT не найдены")
    if not settings.api_key or not settings.folder_id:
        return build_api_response(False, "Заполните API Key и Folder ID")
    response_text, error_message = _request_yandex_completion(
        settings,
        "Ответь ровно одной строкой: OK",
        max_tokens=20,
    )
    if not response_text:
        return build_api_response(False, error_message or "Нет ответа от AI провайдера")
    return build_api_response(True, "Подключение к YandexGPT успешно", response=response_text.strip())


def _get_enabled_yandex_settings():
    settings = AIProviderSettings.objects.filter(provider=AIProviderSettings.Provider.YANDEX_GPT, enabled=True).first()
    if not settings:
        return None
    if not settings.api_key or not settings.folder_id:
        return None
    return settings


def _request_yandex_completion(settings: AIProviderSettings, prompt: str, max_tokens: int = 1000) -> tuple[str | None, str | None]:
    model_name = (settings.model or "yandexgpt").strip()
    endpoint_url = (settings.endpoint_url or "").strip() or "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"
    model_uri_candidates = [f"gpt://{settings.folder_id}/{model_name}"]
    if "/" not in model_name:
        model_uri_candidates.append(f"gpt://{settings.folder_id}/{model_name}/latest")

    last_error = None
    for model_uri in model_uri_candidates:
        payload = {
            "modelUri": model_uri,
            "completionOptions": {
                "stream": False,
                "temperature": 0.2,
                "maxTokens": str(max_tokens),
            },
            "messages": [
                {"role": "system", "text": "Ты эксперт по тестированию ПО. Отвечай на русском языке."},
                {"role": "user", "text": prompt},
            ],
        }
        try:
            response = requests.post(
                endpoint_url,
                headers={
                    "Authorization": f"Api-Key {settings.api_key}",
                    "x-folder-id": settings.folder_id,
                    "Content-Type": "application/json",
                },
                json=payload,
                timeout=30,
            )
            if response.status_code >= 400:
                body_preview = (response.text or "")[:500]
                last_error = (
                    f"Ошибка AI провайдера ({response.status_code}) для modelUri={model_uri}. "
                    f"Ответ: {body_preview or 'пустой ответ'}"
                )
                continue
            data = response.json()
        except requests.RequestException as exc:
            last_error = f"Ошибка сети при запросе к AI провайдеру: {exc}"
            continue
        except ValueError:
            last_error = "AI провайдер вернул некорректный JSON"
            continue

        text = _extract_yandex_text(data)
        if text:
            return text, None
        last_error = (
            f"AI провайдер вернул ответ без текста для modelUri={model_uri}. "
            f"Проверьте model/folder/endpoint."
        )

    return None, last_error


def _extract_yandex_text(data: dict) -> str | None:
    try:
        text = (data.get("result") or {}).get("alternatives", [{}])[0].get("message", {}).get("text")
        if text:
            return text.strip()
    except (AttributeError, IndexError, TypeError):
        pass
    try:
        text = (data.get("result") or {}).get("alternatives", [{}])[0].get("text")
        if text:
            return text.strip()
    except (AttributeError, IndexError, TypeError):
        pass
    return None


def _build_test_case_review_prompt(test_case: TestCase) -> str:
    steps = list(test_case.steps.all().order_by("step_number"))
    steps_text = "\n".join(
        [
            f"{idx + 1}. Действие: {step.action or '-'}; Ожидаемый результат: {step.expected_result or '-'}"
            for idx, step in enumerate(steps)
        ]
    ) or "Шаги не указаны."
    return (
        "Выполни ревью тест-кейса. Дай краткий структурированный ответ:\n"
        "- Сильные стороны\n"
        "- Проблемы\n"
        "- Что улучшить\n"
        "- Итоговая оценка по шкале 1-10\n\n"
        f"Название: {test_case.name or '-'}\n"
        f"Описание: {test_case.description or '-'}\n"
        f"Предусловия: {test_case.preconditions or '-'}\n"
        f"Приоритет: {test_case.priority or '-'}\n"
        f"Шаги:\n{steps_text}"
    )


def _build_test_suite_analysis_prompt(test_suite_id: int, test_cases: list[TestCase]) -> str:
    lines = []
    for case in test_cases[:50]:
        lines.append(f"TC-{case.id}: {case.name or '-'} | priority={case.priority or '-'} | status={case.status or '-'}")
    cases_text = "\n".join(lines) or "Тест-кейсов нет."
    return (
        f"Проанализируй тест-сьют #{test_suite_id}. "
        "Дай краткий вывод о полноте покрытия, рисках и приоритетах доработки.\n\n"
        f"Список тест-кейсов:\n{cases_text}"
    )


def _build_traceability_quality_prompt(
    project: Project,
    user_stories: list[UserStory],
    us_to_tc_ids: dict[int, list[int]],
    test_case_map: dict[int, TestCase],
) -> str:
    blocks = []
    for us in user_stories:
        linked_tc_ids = us_to_tc_ids.get(us.id, [])
        if not linked_tc_ids:
            tc_block = "Нет связанных тест-кейсов."
        else:
            tc_lines = []
            for tc_id in linked_tc_ids[:20]:
                tc = test_case_map.get(tc_id)
                if not tc:
                    continue
                steps = list(tc.steps.all()[:10])
                steps_text = "; ".join(
                    [f"{idx + 1}) {step.action or '-'} -> {step.expected_result or '-'}" for idx, step in enumerate(steps)]
                ) or "шаги не описаны"
                tc_lines.append(
                    f"- TC-{tc.id}: {tc.name or '-'} | priority={tc.priority or '-'} | "
                    f"description={tc.description or '-'} | preconditions={tc.preconditions or '-'} | steps={steps_text}"
                )
            tc_block = "\n".join(tc_lines) if tc_lines else "Нет связанных тест-кейсов."
        blocks.append(
            f"US-{us.id}: {us.name or '-'}\n"
            f"Критичность бизнеса: {us.business_criticality if us.business_criticality is not None else 'не задана'}\n"
            f"Покрывающие тесты:\n{tc_block}"
        )

    return (
        f"Оцени качество тестовой модели проекта '{project.name}'.\n\n"
        "Для каждой User Story:\n"
        "1) оцени покрытие (насколько полно US перекрыта тестами);\n"
        "2) оцени качество тестов (ясность шагов, ожидаемые результаты, наличие негативных проверок);\n"
        "3) явно укажи, есть ли негативные проверки или их не хватает;\n"
        "4) дай оценку по шкале 1-10 для каждой US.\n\n"
        "В конце дай итоговую оценку всей тестовой модели по шкале 1-10 и приоритетный список улучшений.\n"
        "Отвечай на русском языке, структурированно и практично.\n\n"
        f"Данные по US и ТК:\n\n{chr(10).join(blocks)}"
    )


def _build_traceability_quality_fallback(
    user_stories: list[UserStory],
    us_to_tc_ids: dict[int, list[int]],
    test_case_map: dict[int, TestCase],
) -> str:
    lines = ["Локальная оценка (без внешнего AI):", ""]
    per_scores = []
    for us in user_stories:
        linked_ids = us_to_tc_ids.get(us.id, [])
        if not linked_ids:
            score = 1
            lines.append(f"US-{us.id}: {us.name} -> 1/10 (нет покрывающих ТК)")
            per_scores.append(score)
            continue
        has_steps = 0
        has_preconditions = 0
        has_negative_signs = 0
        for tc_id in linked_ids:
            tc = test_case_map.get(tc_id)
            if not tc:
                continue
            if tc.steps.exists():
                has_steps += 1
            if tc.preconditions:
                has_preconditions += 1
            text = f"{tc.name or ''} {tc.description or ''}".lower()
            if any(marker in text for marker in ["негатив", "ошибк", "invalid", "невер", "отказ", "fail"]):
                has_negative_signs += 1
        total = max(len(linked_ids), 1)
        score = 3
        score += round((has_steps / total) * 3)
        score += round((has_preconditions / total) * 2)
        score += round((has_negative_signs / total) * 2)
        score = min(max(score, 1), 10)
        per_scores.append(score)
        neg_note = "есть признаки негативных проверок" if has_negative_signs else "негативных проверок не обнаружено"
        lines.append(f"US-{us.id}: {us.name} -> {score}/10; {neg_note}")
    overall = round(sum(per_scores) / len(per_scores), 1) if per_scores else 0.0
    lines.append("")
    lines.append(f"Итоговая оценка модели: {overall}/10")
    lines.append("Рекомендация: включите AI провайдер для более точной экспертной оценки.")
    return "\n".join(lines)


def _extract_review_score(review_text: str | None) -> int | None:
    if not review_text:
        return None

    # Normalize common markdown/noise artifacts and unicode dashes.
    normalized = review_text
    normalized = normalized.replace(",", ".")
    normalized = normalized.replace("*", " ")
    normalized = re.sub(r"[‐‑‒–—−]", "-", normalized)
    normalized = re.sub(r"\s+", " ", normalized).strip()

    candidate_lines = []
    for raw_line in review_text.splitlines():
        line = raw_line.replace("*", " ")
        line = re.sub(r"[‐‑‒–—−]", "-", line)
        compact = re.sub(r"\s+", " ", line).strip()
        if compact:
            candidate_lines.append(compact)

    # First pass: line-based detection for "Итоговая/Общая оценка".
    for line in candidate_lines:
        lower = line.lower()
        if "итоговая оценка" not in lower and "общая оценка" not in lower and "оценка по шкале" not in lower:
            continue
        numbers = re.findall(r"\d+(?:\.\d+)?", line)
        # Often line contains "1-10" and then final score, e.g. "Итоговая ... 1-10: 6"
        if not numbers:
            continue
        parsed = []
        for token in numbers:
            try:
                parsed.append(float(token))
            except ValueError:
                continue
        if not parsed:
            continue
        # Prefer the last value in score-like lines.
        candidate = parsed[-1]
        if 0 < candidate <= 10:
            return int(round(candidate))

    # Second pass: explicit patterns.
    patterns = [
        r"(?:итоговая|общая)\s+оценка[^\n\r]{0,80}?[:=]\s*(\d+(?:\.\d+)?)",
        r"оценка\s+по\s+шкале\s*1\s*-\s*10[^\n\r]{0,40}?[:=]\s*(\d+(?:\.\d+)?)",
        r"(\d+(?:\.\d+)?)\s*/\s*10",
    ]
    for pattern in patterns:
        match = re.search(pattern, normalized, flags=re.IGNORECASE)
        if not match:
            continue
        try:
            value = float(match.group(1))
        except ValueError:
            continue
        if 0 < value <= 10:
            return int(round(value))
    return None


def _fallback_review_score(test_case: TestCase) -> int:
    score = 1
    if test_case.description:
        score += 3
    if test_case.preconditions:
        score += 2
    if test_case.steps.exists():
        score += 3
    if test_case.priority in {TestCase.Priority.HIGH, TestCase.Priority.CRITICAL}:
        score += 1
    return min(score, 10)
