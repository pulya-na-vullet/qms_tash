from django.apps import AppConfig


class CoreConfig(AppConfig):
    name = 'core'

    def ready(self):
        import sys

        from .scheduler import start_scheduler
        from . import signals  # noqa: F401

        command = sys.argv[1] if len(sys.argv) > 1 else ""
        if command in {"makemigrations", "migrate", "collectstatic", "shell", "check"}:
            return
        start_scheduler()
