#!/bin/sh

# Salir inmediatamente si un comando falla
set -e

echo "=== Iniciando Backend de Bomberos en Docker ==="

# Ejecutar migraciones de la base de datos
echo "Aplicando migraciones..."
python manage.py migrate --noinput

# Ejecutar el script para poblar la base de datos con datos iniciales
echo "Cargando datos iniciales de prueba (seed_data.py)..."
python seed_data.py || true

echo "=== Servidor listo. Iniciando Django en http://0.0.0.0:8000 ==="
exec "$@"
