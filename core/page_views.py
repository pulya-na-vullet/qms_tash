from django.contrib import messages
from django.contrib.auth import authenticate, login as auth_login, logout as auth_logout
from django.contrib.auth.decorators import login_required
from django.contrib.auth.models import User as DjangoUser
from django.shortcuts import get_object_or_404, redirect, render
from django.views.decorators.http import require_POST

from .models import Project, Section, TestRun, TestSuite, TraceabilityMatrix, User as CoreUser
from .serializers import TestRunSerializer, TestSuiteSerializer


def _ensure_demo_auth_users():
    for username in ("admin", "analyst", "tester"):
        user, _ = DjangoUser.objects.get_or_create(username=username)
        user.set_password(username)
        user.is_active = True
        user.save()


def login_page(request):
    _ensure_demo_auth_users()

    if request.user.is_authenticated:
        return redirect("/projects")

    if request.method == "POST":
        username = (request.POST.get("username") or "").strip()
        password = request.POST.get("password") or ""

        user = authenticate(request, username=username, password=password)
        if user is not None and user.is_active:
            auth_login(request, user)
            return redirect("/projects")

        return render(request, "login.html", {"login_error": True})

    return render(request, "login.html", {"logged_out": request.GET.get("logout") == "1"})


@login_required(login_url="/login")
def logout_page(request):
    if request.method in {"POST", "GET"}:
        auth_logout(request)
    return redirect("/login?logout=1")


@login_required(login_url="/login")
def projects_page(request):
    return render(request, "projects.html")


@login_required(login_url="/login")
def project_detail_page(request, id):
    project = Project.objects.filter(id=id).first()
    if not project:
        return redirect("/")
    return render(request, "project-detail.html", {"project": project})


@login_required(login_url="/login")
def section_detail_page(request, id):
    section = Section.objects.filter(id=id).first()
    if not section:
        return redirect("/")
    return render(request, "section-detail.html", {"section": section})


@login_required(login_url="/login")
def test_suite_detail_page(request, id):
    suite = TestSuite.objects.filter(id=id).first()
    if not suite:
        return redirect("/project-qa")
    return render(request, "test-suite-detail.html", {"testSuite": TestSuiteSerializer(suite).data})


@login_required(login_url="/login")
def project_qa_page(request):
    return render(request, "project-qa.html", {"projects": Project.objects.all()})


@login_required(login_url="/login")
def project_qa_detail_page(request, id):
    project = Project.objects.filter(id=id).first()
    if not project:
        return redirect("/project-qa")
    suites = TestSuite.objects.filter(project_id=id)
    return render(request, "project-qa-detail.html", {"project": project, "testSuites": TestSuiteSerializer(suites, many=True).data})


@login_required(login_url="/login")
def test_runs_page(request, project_id):
    project = Project.objects.filter(id=project_id).first()
    if not project:
        return redirect("/project-qa")
    test_runs = TestRunSerializer(TestRun.objects.filter(project_id=project_id), many=True).data
    return render(request, "test-runs.html", {"project": project, "testRuns": test_runs})


@login_required(login_url="/login")
def test_run_detail_page(request, id):
    test_run = TestRun.objects.filter(id=id).first()
    if not test_run:
        return redirect("/project-qa")
    return render(request, "test-run-detail.html", {"testRun": TestRunSerializer(test_run).data})


@login_required(login_url="/login")
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


@login_required(login_url="/login")
def admin_users_page(request):
    if request.method == "POST":
        username = (request.POST.get("username") or "").strip()
        password = request.POST.get("password") or ""
        full_name = (request.POST.get("fullName") or "").strip()
        email = (request.POST.get("email") or "").strip() or None
        role = (request.POST.get("role") or "").strip()
        if not username or not password or not full_name or not role:
            messages.error(request, "Заполните обязательные поля пользователя.")
            return redirect("/admin/users")
        if CoreUser.objects.filter(username=username).exists():
            messages.error(request, "Пользователь с таким именем уже существует.")
            return redirect("/admin/users")

        core_user = CoreUser.objects.create(
            username=username,
            password=password,
            full_name=full_name,
            email=email,
            roles=[role],
            enabled=True,
        )
        django_user, created = DjangoUser.objects.get_or_create(
            username=username,
            defaults={"email": email or "", "first_name": full_name},
        )
        django_user.set_password(password)
        django_user.is_active = True
        django_user.save()
        messages.success(request, f"Пользователь {core_user.username} создан.")
        return redirect("/admin/users")

    users = CoreUser.objects.all()
    admin_count = sum(1 for user in users if "ADMIN" in (user.roles or []))
    return render(
        request,
        "admin/users.html",
        {
            "users": users,
            "totalUsers": users.count(),
            "activeUsers": users.filter(enabled=True).count(),
            "adminUsers": admin_count,
            "roles": [choice for choice, _ in CoreUser.Role.choices],
        },
    )


@login_required(login_url="/login")
@require_POST
def admin_user_activate(request, id):
    user = get_object_or_404(CoreUser, id=id)
    user.enabled = True
    user.deactivation_reason = None
    user.save()
    django_user = DjangoUser.objects.filter(username=user.username).first()
    if django_user:
        django_user.is_active = True
        django_user.save()
    messages.success(request, f"Пользователь {user.username} активирован.")
    return redirect("/admin/users")


@login_required(login_url="/login")
@require_POST
def admin_user_deactivate(request, id):
    user = get_object_or_404(CoreUser, id=id)
    user.enabled = False
    user.deactivation_reason = "Деактивирован администратором"
    user.save()
    django_user = DjangoUser.objects.filter(username=user.username).first()
    if django_user:
        django_user.is_active = False
        django_user.save()
    messages.success(request, f"Пользователь {user.username} деактивирован.")
    return redirect("/admin/users")
