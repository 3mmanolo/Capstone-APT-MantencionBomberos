from django.db import models
from django.contrib.auth.models import AbstractUser


class Compania(models.Model):
    nombre = models.CharField(max_length=100)

    def __str__(self):
        return self.nombre


class Usuario(AbstractUser):
    nombre = models.CharField(max_length=150, blank=True)
    rol = models.CharField(
        max_length=50,
        choices=[
            ('Administrador', 'Administrador'),
            ('Encargado de flota', 'Encargado de flota'),
            ('Voluntario', 'Voluntario'),
            ('Maquinista', 'Maquinista'),
        ],
        default='Voluntario'
    )
    compania = models.ForeignKey(Compania, on_delete=models.SET_NULL, null=True, blank=True)
    telefono = models.CharField(max_length=20, blank=True)

    def __str__(self):
        return self.nombre or self.username


class Vehiculo(models.Model):
    ESTADO_CHOICES = [
        ('Operativo', 'Operativo'),
        ('Por Vencer', 'Por Vencer'),
        ('Vencido', 'Vencido'),
        ('En Mantención', 'En Mantención'),
    ]

    nombre = models.CharField(max_length=100)
    patente = models.CharField(max_length=20, unique=True)
    tipo = models.CharField(max_length=50)
    anio = models.CharField(max_length=4)
    kilometraje = models.FloatField(default=0.0)
    compania = models.ForeignKey(Compania, on_delete=models.CASCADE, related_name='vehiculos')
    proxima_mantencion = models.DateField(null=True, blank=True)
    estado = models.CharField(max_length=20, choices=ESTADO_CHOICES, default='Operativo')

    def __str__(self):
        return f"{self.nombre} ({self.patente})"


class Mantencion(models.Model):
    vehiculo = models.ForeignKey(Vehiculo, on_delete=models.CASCADE, related_name='mantenciones')
    tipo = models.CharField(max_length=50)
    descripcion = models.TextField(blank=True)
    costo = models.DecimalField(max_digits=10, decimal_places=2, default=0.00)
    taller = models.CharField(max_length=150, blank=True)
    materiales = models.TextField(blank=True)
    kilometraje = models.FloatField(default=0.0)
    fecha = models.DateTimeField(auto_now_add=True)
    proxima_mantencion = models.DateField(null=True, blank=True)
    responsable = models.ForeignKey(Usuario, on_delete=models.SET_NULL, null=True, blank=True)

    def __str__(self):
        return f"{self.tipo} - {self.vehiculo.nombre} ({self.fecha.strftime('%Y-%m-%d')})"


class Notificacion(models.Model):
    usuario = models.ForeignKey(Usuario, on_delete=models.CASCADE, related_name='notificaciones', null=True, blank=True)
    titulo = models.CharField(max_length=150)
    mensaje = models.TextField()
    tipo = models.CharField(max_length=20, default='info')  # warning, danger, info, success
    fecha = models.DateTimeField(auto_now_add=True)
    leida = models.BooleanField(default=False)

    def __str__(self):
        return f"{self.titulo} - {self.fecha.strftime('%Y-%m-%d')}"
