from django.shortcuts import redirect, render

from .models import Project, Section, TestRun, TestSuite, TraceabilityMatrix, User
from .serializers import TestRunSerializer, TestSuiteSerializer


def login_page(request):
    return render(request, "login.html")


def projects_page(request):
    return render(request, "projects.html")


def project_detail_page(request, id):
    project = Project.objects.filter(id=id).first()
    if not project:
        return redirect("/")
    return render(request, "project-detail.html", {"project": project})


def section_detail_page(request, id):
    section = Section.objects.filter(id=id).first()
    if not section:
        return redirect("/")
    return render(request, "section-detail.html", {"section": section})


def test_suite_detail_page(request, id):
    suite = TestSuite.objects.filter(id=id).first()
    if not suite:
        return redirect("/project-qa")
    return render(request, "test-suite-detail.html", {"testSuite": TestSuiteSerializer(suite).data})


def project_qa_page(request):
    return render(request, "project-qa.html", {"projects": Project.objects.all()})


def project_qa_detail_page(request, id):
    project = Project.objects.filter(id=id).first()
    if not project:
        return redirect("/project-qa")
    suites = TestSuite.objects.filter(project_id=id)
    return render(request, "project-qa-detail.html", {"project": project, "testSuites": TestSuiteSerializer(suites, many=True).data})


def test_runs_page(request, project_id):
    project = Project.objects.filter(id=project_id).first()
    if not project:
        return redirect("/project-qa")
    test_runs = TestRunSerializer(TestRun.objects.filter(project_id=project_id), many=True).data
    return render(request, "test-runs.html", {"project": project, "testRuns": test_runs})


def test_run_detail_page(request, id):
    test_run = TestRun.objects.filter(id=id).first()
    if not test_run:
        return redirect("/project-qa")
    return render(request, "test-run-detail.html", {"testRun": TestRunSerializer(test_run).data})


def traceability_matrix_page(request, project_id):
    project = Project.objects.filter(id=project_id).first()
    if not project:
        return redirect("/project-qa")
    matrix = TraceabilityMatrix.objects.filter(project_id=project_id).order_by("-created_at").first()
    matrix_html = matrix.matrix_html if matrix else ""
    user_role = "UNKNOWN"
    # Spring version derived this from authenticated authorities.
    # Here we keep compatibility with template expectations.
    if request.GET.get("role"):
        user_role = request.GET["role"]
    return render(
        request,
        "traceability-matrix.html",
        {"project": project, "matrixHtml": matrix_html, "userRole": user_role},
    )


def admin_users_page(request):
    users = User.objects.all()
    return render(
        request,
        "admin/users.html",
        {
            "users": users,
            "totalUsers": users.count(),
            "activeUsers": users.filter(enabled=True).count(),
            "adminUsers": users.filter(roles__contains=["ADMIN"]).count(),
            "roles": [choice for choice, _ in User.Role.choices],
        },
    )
