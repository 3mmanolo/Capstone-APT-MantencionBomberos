from django.contrib import admin

from django.contrib.auth.admin import UserAdmin as BaseUserAdmin
from django.contrib.auth.models import User
from .models import Usuario
# Importamos explícitamente todos tus modelos desde el archivo models.py de la carpeta actual
from .models import (
    Compania, Usuario, TipoMantencion, Insumo, 
    Empleados, Vehiculo, Mantencion, 
    MantencionInsu, MantencionEmp, Notificacion
)

# Definimos el perfil de Usuario como un bloque integrado
class UsuarioInline(admin.StackedInline):
    model = Usuario
    can_delete = False
    verbose_name_plural = 'Perfil de Usuario'

# Extendemos el UserAdmin nativo de Django
class UserAdmin(BaseUserAdmin):
    inlines = (UsuarioInline,)

# Re-registramos el modelo User
admin.site.unregister(User)
admin.site.register(User, UserAdmin)

# Registramos cada una en el panel de administración
admin.site.register(Compania)
admin.site.register(Usuario)
admin.site.register(TipoMantencion)
admin.site.register(Insumo)
admin.site.register(Empleados)
admin.site.register(Vehiculo)
admin.site.register(Mantencion)
admin.site.register(MantencionInsu)
admin.site.register(MantencionEmp)
admin.site.register(Notificacion)
