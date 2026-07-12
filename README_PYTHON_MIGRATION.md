# QMS backend migrated to Django

This repository now includes a Python backend that replaces the previous Spring Boot service while keeping the same frontend files (`CKK-main/src/main/resources/templates` and `static`).

## Stack

- Django 6
- Django REST Framework
- PostgreSQL (`qms_projects` by default)
- APScheduler for periodic matrix rebuilds

## Key parity points

- API endpoints retained under `/api/...` and `/admin/users/api...`
- HTML routes retained (`/projects`, `/project-qa`, `/test-suite/{id}`, etc.)
- Existing templates and CSS are served directly from the original frontend folders
- Traceability matrix scheduler fixed to run every **15 minutes** (not every second)

## Run

```bash
python3 -m pip install -r requirements.txt
python3 manage.py makemigrations core
python3 manage.py migrate
python3 manage.py runserver 0.0.0.0:8080
```

## Environment

- `POSTGRES_DB` (default: `qms_projects`)
- `POSTGRES_USER` (default: `postgres`)
- `POSTGRES_PASSWORD` (default: `postgres`)
- `POSTGRES_HOST` (default: `localhost`)
- `POSTGRES_PORT` (default: `5432`)
- `DJANGO_DEBUG` (default: `true`)
- `DJANGO_ALLOWED_HOSTS` (default: `*`)
- `DISABLE_QMS_SCHEDULER` (set `true` to disable scheduler)
