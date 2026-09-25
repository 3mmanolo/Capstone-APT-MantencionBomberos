import re

from django.db import migrations, models


REEMPLAZOS_ACENTOS = {
    'á': 'a', 'é': 'e', 'í': 'i', 'ó': 'o', 'ú': 'u', 'ñ': 'n', 'ü': 'u',
}


def slugify_nombre(nombre):
    s = (nombre or '').lower().strip()
    for a, b in REEMPLAZOS_ACENTOS.items():
        s = s.replace(a, b)
    s = re.sub(r'[^a-z0-9\s]', '', s)
    s = re.sub(r'\s+', '.', s.strip())
    return s or 'usuario'


def poblar_contacto_placeholder(apps, schema_editor):
    """Rellena correo y telefono SOLO para los usuarios que los tengan
    vacios, con datos de relleno explicitamente pedidos por el equipo
    para no dejar esos campos en blanco mientras cargan los reales."""
    Usuario = apps.get_model('core', 'Usuario')

    contador = 0
    for perfil in Usuario.objects.all().order_by('id_user'):
        contador += 1

        if perfil.user_id:
            user_obj = perfil.user
            if not user_obj.email:
                base = slugify_nombre(perfil.nombre)
                user_obj.email = f"{base}@bomberosmelipilla.cl"
                user_obj.save(update_fields=['email'])

        if not perfil.telefono:
            numero = 10000000 + contador
            perfil.telefono = f"+56 9 {numero // 10000:04d} {numero % 10000:04d}"
            perfil.save(update_fields=['telefono'])


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0004_mantencion_descripcion'),
    ]

    operations = [
        migrations.AddField(
            model_name='usuario',
            name='telefono',
            field=models.CharField(blank=True, default='', max_length=30),
        ),
        migrations.RunPython(poblar_contacto_placeholder, reverse_code=migrations.RunPython.noop),
    ]
