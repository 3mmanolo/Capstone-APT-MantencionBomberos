from rest_framework import serializers
from .models import Compania, Usuario, Vehiculo, Mantencion, Notificacion


# ==========================================
# 1. SERIALIZADOR DE COMPAÑÍA
# ==========================================
class CompaniaSerializer(serializers.ModelSerializer):
    class Meta:
        model = Compania
        fields = ['id', 'nombre']


# ==========================================
# 2. SERIALIZADOR DE USUARIO
# ==========================================
class UsuarioSerializer(serializers.ModelSerializer):
    compania_nombre = serializers.ReadOnlyField(source='compania.nombre')
    compania_id = serializers.PrimaryKeyRelatedField(
        queryset=Compania.objects.all(), source='compania', write_only=True, required=False, allow_null=True
    )
    initials = serializers.SerializerMethodField()
    isAdmin = serializers.SerializerMethodField()

    class Meta:
        model = Usuario
        fields = [
            'id',
            'username',
            'email',
            'nombre',
            'rol',
            'compania',
            'compania_nombre',
            'compania_id',
            'telefono',
            'initials',
            'isAdmin',
        ]
        extra_kwargs = {
            'password': {'write_only': True}
        }

    def get_compania(self, obj):
        return obj.compania.nombre if obj.compania else "Sin Compañía"

    def get_initials(self, obj):
        if not obj.nombre:
            return obj.username[:2].upper() if obj.username else "US"
        parts = obj.nombre.strip().split()
        if len(parts) >= 2:
            return f"{parts[0][0]}{parts[1][0]}".upper()
        return obj.nombre[:2].upper()

    def get_isAdmin(self, obj):
        return obj.rol in ['Administrador', 'Admin'] or getattr(obj, 'is_staff', False) or getattr(obj, 'is_superuser', False)


# ==========================================
# 3. SERIALIZADOR DE VEHÍCULO
# ==========================================
class VehiculoSerializer(serializers.ModelSerializer):
    compania_nombre = serializers.ReadOnlyField(source='compania.nombre')
    compania_id = serializers.PrimaryKeyRelatedField(
        queryset=Compania.objects.all(), source='compania', write_only=True, required=False, allow_null=True
    )
    compania = serializers.SerializerMethodField()
    real_id = serializers.IntegerField(source='id', read_only=True)
    estadoText = serializers.SerializerMethodField()
    estadoDetalle = serializers.SerializerMethodField()
    colorHex = serializers.SerializerMethodField()
    status_badge = serializers.SerializerMethodField()
    tipo_v = serializers.CharField(source='tipo', read_only=True)

    class Meta:
        model = Vehiculo
        fields = [
            'id',
            'real_id',
            'nombre',
            'patente',
            'tipo',
            'tipo_v',
            'anio',
            'kilometraje',
            'compania',
            'compania_nombre',
            'compania_id',
            'proxima_mantencion',
            'estado',
            'estadoText',
            'estadoDetalle',
            'colorHex',
            'status_badge',
        ]

    def get_compania(self, obj):
        return obj.compania.nombre if obj.compania else "Sin Compañía"

    def get_estadoText(self, obj):
        if obj.estado == 'Operativo':
            return 'Operativo'
        elif obj.estado == 'Por Vencer':
            return 'Mantención Próxima'
        elif obj.estado == 'Vencido':
            return 'Mantención Vencida'
        return obj.estado or 'Desconocido'

    def get_estadoDetalle(self, obj):
        if obj.proxima_mantencion:
            return f"Próxima mantención: {obj.proxima_mantencion.strftime('%d/%m/%Y')}"
        return "Sin fecha de mantención programada"

    def get_colorHex(self, obj):
        colores = {
            'Operativo': '#2E7D32',      # Verde
            'Por Vencer': '#ED6C02',     # Naranja/Amarillo
            'Vencido': '#D32F2F',        # Rojo
            'En Mantencion': '#0288D1',  # Azul
        }
        return colores.get(obj.estado, '#757575')

    def get_status_badge(self, obj):
        return (obj.estado or 'DESCONOCIDO').upper()


# ==========================================
# 4. SERIALIZADOR DE MANTENCIÓN / LOGS
# ==========================================
class MantencionSerializer(serializers.ModelSerializer):
    vehiculo_id = serializers.PrimaryKeyRelatedField(
        queryset=Vehiculo.objects.all(), source='vehiculo'
    )
    responsable_nombre = serializers.SerializerMethodField()
    observaciones = serializers.CharField(source='descripcion', read_only=True)

    class Meta:
        model = Mantencion
        fields = [
            'id',
            'vehiculo',
            'vehiculo_id',
            'tipo',
            'descripcion',
            'observaciones',
            'costo',
            'taller',
            'materiales',
            'kilometraje',
            'fecha',
            'proxima_mantencion',
            'responsable',
            'responsable_nombre',
        ]

    def get_responsable_nombre(self, obj):
        if obj.responsable:
            return getattr(obj.responsable, 'nombre', str(obj.responsable))
        return "Sistema"


# ==========================================
# 5. SERIALIZADOR DE NOTIFICACIONES
# ==========================================
class NotificacionSerializer(serializers.ModelSerializer):
    class Meta:
        model = Notificacion
        fields = ['id', 'titulo', 'mensaje', 'tipo', 'fecha', 'leida']


# ==========================================
# 6. SERIALIZADORES DE PETICIÓN (REQUESTS)
# ==========================================
class RegistrarMantencionRequestSerializer(serializers.Serializer):
    vehiculo_id = serializers.IntegerField()
    tipo = serializers.CharField(max_length=100)
    descripcion = serializers.CharField(required=False, allow_blank=True)
    costo = serializers.DecimalField(max_digits=12, decimal_places=2, required=False, default=0)
    taller = serializers.CharField(max_length=150, required=False, allow_blank=True)
    materiales = serializers.CharField(required=False, allow_blank=True)
    proxima_mantencion = serializers.DateField()


class GuardarVehiculoRequestSerializer(serializers.ModelSerializer):
    compania_id = serializers.IntegerField(required=False, allow_null=True)

    class Meta:
        model = Vehiculo
        fields = ['id', 'nombre', 'patente', 'tipo', 'anio', 'kilometraje', 'compania_id', 'proxima_mantencion']


class GuardarUsuarioRequestSerializer(serializers.ModelSerializer):
    compania_id = serializers.IntegerField(required=False, allow_null=True)

    class Meta:
        model = Usuario
        fields = ['id', 'nombre', 'email', 'rol', 'compania_id', 'telefono']
