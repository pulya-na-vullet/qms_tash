from datetime import timedelta

from django.db import transaction
from django.db.models import Prefetch
from django.utils import timezone

from .models import (
    AIAnalysis,
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
            project_id=project_id,
            test_case_id__in=test_cases.values_list("id", flat=True),
            user_story_id__in=user_stories.values_list("id", flat=True),
        ).values_list("test_case_id", "user_story_id")
    )

    html = ["<table class=\"traceability-table\">"]
    html.append("<thead><tr><th class=\"user-story-header\">User Story \\ Test Case</th>")
    for tc in test_cases:
        safe_name = (tc.name or "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")
        html.append(
            f"<th class=\"test-case-header\"><a href=\"#\" class=\"rotated-link\" data-test-case-id=\"{tc.id}\" title=\"{safe_name}\">TC-{tc.id}</a></th>"
        )
    html.append("</tr></thead><tbody>")
    for us in user_stories:
        us_name = (us.name or "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")
        html.append(f"<tr class=\"user-story-row\"><td class=\"user-story-cell\" title=\"{us_name}\">{us_name}</td>")
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
