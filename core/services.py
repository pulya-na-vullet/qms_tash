from datetime import timedelta
from html import escape

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
        html.append(
            f"<th class=\"test-case-header\"><a href=\"{tc_link}\" class=\"rotated-link\" "
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
    test_cases = TestCase.objects.filter(test_suite__project_id=project_id).values_list("id", flat=True)
    links = set(
        TestCaseUserStory.objects.filter(
            test_case_id__in=test_cases,
            user_story_id__in=[us.id for us in user_stories],
        ).values_list("user_story_id", flat=True)
    )

    total_us = len(user_stories)
    covered_us = sum(1 for us in user_stories if us.id in links)
    uncovered_us = total_us - covered_us
    coverage_percent = round((covered_us * 100.0) / total_us, 2) if total_us else 0.0

    low_count = sum(1 for us in user_stories if us.business_criticality is not None and 1 <= us.business_criticality <= 4)
    medium_count = sum(1 for us in user_stories if us.business_criticality is not None and 5 <= us.business_criticality <= 7)
    high_count = sum(1 for us in user_stories if us.business_criticality is not None and 8 <= us.business_criticality <= 10)

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
        summary = _request_yandex_completion(ai_settings, prompt, max_tokens=1200)
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
        result = _request_yandex_completion(ai_settings, _build_test_case_review_prompt(test_case), max_tokens=1200)
        if not result:
            result = "AI провайдер включен, но не удалось получить ответ. Проверьте настройки подключения."
    else:
        result = f"Auto review for test case {test_case_id}: name length={len(test_case.name or '')}"
    score = 50 + min(50, len((test_case.description or "")) // 10)
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


def test_ai_provider_connection():
    settings = AIProviderSettings.objects.filter(provider=AIProviderSettings.Provider.YANDEX_GPT).first()
    if not settings:
        return build_api_response(False, "Настройки YandexGPT не найдены")
    if not settings.api_key or not settings.folder_id:
        return build_api_response(False, "Заполните API Key и Folder ID")
    response_text = _request_yandex_completion(
        settings,
        "Ответь ровно одной строкой: OK",
        max_tokens=20,
    )
    if not response_text:
        return build_api_response(False, "Нет ответа от AI провайдера")
    return build_api_response(True, "Подключение к YandexGPT успешно", response=response_text.strip())


def _get_enabled_yandex_settings():
    settings = AIProviderSettings.objects.filter(provider=AIProviderSettings.Provider.YANDEX_GPT, enabled=True).first()
    if not settings:
        return None
    if not settings.api_key or not settings.folder_id:
        return None
    return settings


def _request_yandex_completion(settings: AIProviderSettings, prompt: str, max_tokens: int = 1000) -> str | None:
    model_name = (settings.model or "yandexgpt").strip()
    endpoint_url = (settings.endpoint_url or "").strip() or "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"
    payload = {
        "modelUri": f"gpt://{settings.folder_id}/{model_name}",
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
        response.raise_for_status()
        data = response.json()
    except (requests.RequestException, ValueError):
        return None

    try:
        return (data["result"]["alternatives"][0]["message"]["text"] or "").strip() or None
    except (KeyError, IndexError, TypeError):
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
