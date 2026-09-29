from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0003_vehiculo_proxima_mantencion'),
    ]

    operations = [
        migrations.AddField(
            model_name='mantencion',
            name='descripcion',
            field=models.TextField(blank=True, default=''),
        ),
    ]
