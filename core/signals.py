from django.db.models.signals import post_migrate
from django.dispatch import receiver

from .services import ensure_default_admin


@receiver(post_migrate)
def create_default_admin(sender, **kwargs):
    if sender.name == "core":
        ensure_default_admin()
