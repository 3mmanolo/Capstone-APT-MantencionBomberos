from rest_framework import serializers
from .models import Vehiculo, Usuario, Compania, Mantencion, TipoMantencion, Insumo, MantencionInsu


class TuModeloSerializer(serializers.ModelSerializer):
    class Meta:
        model = TuModelo
        fields = '__all__' # o especifica los campos: ['id', 'nombre', 'fecha']