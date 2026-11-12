# core/permissions.py
# Reglas de autorización por rol, usadas por la API (DRF) y por la web.
from rest_framework.permissions import BasePermission, SAFE_METHODS

# Roles que pueden registrar mantenciones y marcar vehículos como operativos.
# Si el equipo decide otra cosa, se cambia SOLO esta línea.
ROLES_GESTION_FLOTA = {'administrador', 'encargado de flota'}


def rol_de(user):
    perfil = getattr(user, 'perfil', None)
    return (perfil.rol if perfil and perfil.rol else '').strip().lower()


def es_admin(user):
    return bool(user and user.is_authenticated and (user.is_superuser or rol_de(user) == 'administrador'))


def gestiona_flota(user):
    return bool(user and user.is_authenticated and (es_admin(user) or rol_de(user) in ROLES_GESTION_FLOTA))


class EsAdministrador(BasePermission):
    message = 'No tienes permisos para realizar esta acción.'

    def has_permission(self, request, view):
        return es_admin(request.user)


class GestionaFlota(BasePermission):
    message = 'No tienes permisos para realizar esta acción.'

    def has_permission(self, request, view):
        return gestiona_flota(request.user)


class LecturaOAdministrador(BasePermission):
    """GET para cualquier usuario autenticado; crear/editar solo administrador."""
    message = 'No tienes permisos para realizar esta acción.'

    def has_permission(self, request, view):
        if request.method in SAFE_METHODS:
            return bool(request.user and request.user.is_authenticated)
        return es_admin(request.user)