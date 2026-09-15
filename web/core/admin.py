from django.contrib import admin
# Importamos explícitamente todos tus modelos desde el archivo models.py de la carpeta actual
from .models import (
    Compania, Usuario, TipoMantencion, Insumo, 
    Empleados, Vehiculo, Mantencion, 
    MantencionInsu, MantencionEmp, Notificacion
)

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
