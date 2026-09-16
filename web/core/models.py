from django.db import models
from django.contrib.auth.models import User  # Importamos el User de Django

class Compania(models.Model):
    id_comp = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=100)
    direccion = models.CharField(max_length=200, blank=True, db_column="dirección")
    telefono = models.CharField(max_length=20, blank=True, db_column="teléfono")

    class Meta:
        db_table = "compania"
        verbose_name_plural = "Compañías"

    def __str__(self):
        return self.nombre


class Usuario(models.Model):
    id_user = models.AutoField(primary_key=True)
    # Vinculación con el User nativo de Django
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name="perfil", null=True, blank=True)
    nombre = models.CharField(max_length=120)
    rol = models.CharField(max_length=50)
    id_compania = models.ForeignKey(
        Compania, on_delete=models.SET_NULL, null=True, blank=True,
        db_column="id_compania", related_name="usuarios",
    )

    class Meta:
        db_table = "usuario"

    def __str__(self):
        return self.nombre


class TipoMantencion(models.Model):
    id_tipo = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=100)
    categ = models.CharField(max_length=50, blank=True)
    perio = models.CharField(max_length=50, blank=True)

    class Meta:
        db_table = "tipomantencion"
        verbose_name = "Tipo de mantención"
        verbose_name_plural = "Tipos de mantención"

    def __str__(self):
        return self.nombre


class Insumo(models.Model):
    id_insu = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=150)
    tipo_i = models.CharField(max_length=80, blank=True)
    unidad_m = models.CharField(max_length=20, blank=True)
    costo_uni = models.DecimalField(max_digits=12, decimal_places=2, default=0)
    stock = models.IntegerField(default=0)

    class Meta:
        db_table = "insumo"

    def __str__(self):
        return self.nombre


class Empleados(models.Model):
    id_emp = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=120)
    especialidad = models.CharField(max_length=100, blank=True)
    tarifa_hora = models.DecimalField(max_digits=12, decimal_places=2, default=0)

    class Meta:
        db_table = "empleados"
        verbose_name_plural = "Empleados"

    def __str__(self):
        return self.nombre


class Vehiculo(models.Model):
    id_vehi = models.AutoField(primary_key=True)
    patente = models.CharField(max_length=10, unique=True, null=True, blank=True)
    tipo_v = models.CharField(max_length=50)
    modelo = models.CharField(max_length=80, blank=True)
    anio = models.IntegerField(null=True, blank=True, db_column="año")
    id_compania = models.ForeignKey(
        Compania, on_delete=models.PROTECT, db_column="id_compania", related_name="vehiculos",
    )
    estado = models.CharField(max_length=30, default="operativo")
    kilometraje = models.DecimalField(max_digits=10, decimal_places=2, default=0)

    class Meta:
        db_table = "vehiculo"

    def __str__(self):
        return self.patente or f"Vehículo {self.id_vehi}"


class Mantencion(models.Model):
    id_mant = models.AutoField(primary_key=True)
    id_vehi = models.ForeignKey(
        Vehiculo, on_delete=models.CASCADE, db_column="id_vehi", related_name="mantenciones",
    )
    id_tipo = models.ForeignKey(
        TipoMantencion, on_delete=models.PROTECT, db_column="id_tipo", related_name="mantenciones",
    )
    fecha_in = models.DateField(auto_now_add=True)
    fecha_ter = models.DateField(null=True, blank=True)
    estado = models.CharField(max_length=30, default="pendiente")
    costo_t = models.DecimalField(max_digits=12, decimal_places=2, default=0)
    respon = models.CharField(max_length=120, blank=True)

    class Meta:
        db_table = "mantencion"


class MantencionInsu(models.Model):
    id_mant = models.ForeignKey(
        Mantencion, on_delete=models.CASCADE, db_column="id_mant", related_name="insumos_usados",
    )
    id_insu = models.ForeignKey(
        Insumo, on_delete=models.PROTECT, db_column="id_insu", related_name="mantenciones",
    )
    cant = models.DecimalField(max_digits=10, decimal_places=2, default=1)
    costo_aso = models.DecimalField(max_digits=12, decimal_places=2, default=0)

    class Meta:
        db_table = "mantencion_insu"
        # Esto crea la llave compuesta a nivel de Base de Datos
        unique_together = (('id_mant', 'id_insu'),)

    def __str__(self):
        return f"Mantenimiento {self.id_mant_id} - Insumo {self.id_insu_id}"



class MantencionEmp(models.Model):
    id_mant = models.ForeignKey(
        Mantencion, on_delete=models.CASCADE, db_column="id_mant", related_name="empleados_asignados",
    )
    id_emp = models.ForeignKey(
        Empleados, on_delete=models.PROTECT, db_column="id_emp", related_name="mantenciones",
    )
    horas_tra = models.DecimalField(max_digits=6, decimal_places=2, default=0)
    costo_aso = models.DecimalField(max_digits=12, decimal_places=2, default=0)

    class Meta:
        db_table = "mantencion_emp"
        # Esto crea la llave compuesta a nivel de Base de Datos
        unique_together = (('id_mant', 'id_emp'),)

    def __str__(self):
        return f"Mantenimiento {self.id_mant_id} - Empleado {self.id_emp_id}"


class Notificacion(models.Model):
    id_noti = models.AutoField(primary_key=True)
    id_mant = models.ForeignKey(
        Mantencion, on_delete=models.CASCADE, db_column="id_mant", related_name="notificaciones",
    )
    tipo_n = models.CharField(max_length=50, blank=True)
    fecha_en = models.DateField(auto_now_add=True)
    desti = models.CharField(max_length=150, blank=True)

    class Meta:
        db_table = "notificacion"
