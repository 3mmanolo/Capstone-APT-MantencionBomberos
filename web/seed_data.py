import os
import sys
import django
from datetime import date, timedelta

# Configurar entorno Django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings')
django.setup()

from django.contrib.auth.models import User
from core.models import Compania, Usuario, TipoMantencion, Insumo, Vehiculo, Mantencion, MantencionInsu

def run_seed():
    print("Iniciando sembrado de datos iniciales en la base de datos...")

    # 1. Compañías
    companias_data = [
        {"nombre": "Compañía 1ª", "direccion": "Av. Brasil 123, Melipilla", "telefono": "+56 2 2831 1111"},
        {"nombre": "Compañía 2ª", "direccion": "Los Cerros 456, Melipilla", "telefono": "+56 2 2831 2222"},
        {"nombre": "Compañía 3ª", "direccion": "Calle Central 789, Melipilla", "telefono": "+56 2 2831 3333"},
        {"nombre": "Compañía 4ª", "direccion": "Av. Vicuña Mackenna 101, Melipilla", "telefono": "+56 2 2831 4444"},
    ]

    companias_objs = {}
    for cdata in companias_data:
        comp, created = Compania.objects.get_or_create(
            nombre=cdata["nombre"],
            defaults={"direccion": cdata["direccion"], "telefono": cdata["telefono"]}
        )
        companias_objs[cdata["nombre"]] = comp
        print(f"  [Compañía] {'Creada' if created else 'Existente'}: {comp.nombre}")

    # 2. Usuarios
    usuarios_seed = [
        {
            "username": "juan.perez",
            "email": "juan.perez@bomberosmelipilla.cl",
            "nombre": "Juan Pérez Soto",
            "rol": "Administrador",
            "compania": "Compañía 1ª",
            "telefono": "+56 9 1234 5678",
            "is_superuser": True
        },
        {
            "username": "camila.rojas",
            "email": "camila.rojas@bomberosmelipilla.cl",
            "nombre": "Camila Rojas Vega",
            "rol": "Encargado de flota",
            "compania": "Compañía 2ª",
            "telefono": "+56 9 8765 4321",
            "is_superuser": False
        },
        {
            "username": "pedro.soto",
            "email": "pedro.soto@bomberosmelipilla.cl",
            "nombre": "Pedro Soto Álvarez",
            "rol": "Voluntario",
            "compania": "Compañía 3ª",
            "telefono": "+56 9 5555 4444",
            "is_superuser": False
        }
    ]

    for udata in usuarios_seed:
        user_obj, user_created = User.objects.get_or_create(username=udata["username"])
        if user_created:
            user_obj.email = udata["email"]
            user_obj.set_password("admin123")
            user_obj.is_superuser = udata["is_superuser"]
            user_obj.is_staff = udata["is_superuser"]
            user_obj.save()

        comp_obj = companias_objs.get(udata["compania"])
        perfil, perfil_created = Usuario.objects.get_or_create(user=user_obj)
        perfil.nombre = udata["nombre"]
        perfil.rol = udata["rol"]
        perfil.id_compania = comp_obj
        perfil.telefono = udata["telefono"]
        perfil.save()
        print(f"  [Usuario] {udata['nombre']} ({udata['email']}) listo.")

    # 3. Tipos de Mantención
    tipos_data = [
        {"nombre": "Preventiva", "categ": "General", "perio": "Trimestral"},
        {"nombre": "Correctiva", "categ": "Mecánica", "perio": "Según demanda"},
        {"nombre": "Urgencia", "categ": "Emergencia", "perio": "Inmediata"},
    ]

    tipos_objs = {}
    for tdata in tipos_data:
        t_obj, _ = TipoMantencion.objects.get_or_create(
            nombre=tdata["nombre"],
            defaults={"categ": tdata["categ"], "perio": tdata["perio"]}
        )
        tipos_objs[tdata["nombre"]] = t_obj

    # 4. Insumos
    insumos_data = [
        {"nombre": "Filtro de Aceite Heavy Duty", "tipo_i": "Filtro", "unidad_m": "Unidad", "costo_uni": 25000, "stock": 20},
        {"nombre": "Aceite Sintético 15W40 (Bidón 5L)", "tipo_i": "Lubricante", "unidad_m": "Bidón", "costo_uni": 45000, "stock": 15},
        {"nombre": "Pastillas de Freno Delanteras", "tipo_i": "Repuesto", "unidad_m": "Juego", "costo_uni": 60000, "stock": 8},
        {"nombre": "Ampolleta LED Baliza 12V", "tipo_i": "Eléctrico", "unidad_m": "Unidad", "costo_uni": 15000, "stock": 30},
    ]

    for idata in insumos_data:
        Insumo.objects.get_or_create(
            nombre=idata["nombre"],
            defaults={
                "tipo_i": idata["tipo_i"],
                "unidad_m": idata["unidad_m"],
                "costo_uni": idata["costo_uni"],
                "stock": idata["stock"]
            }
        )

    # 5. Vehículos
    hoy = date.today()
    vehiculos_data = [
        {
            "patente": "HXPL-21",
            "modelo": "Bomba Melipilla (B-1)",
            "tipo_v": "Carro bomba",
            "anio": 2016,
            "compania": "Compañía 1ª",
            "estado": "operativo",
            "kilometraje": 45200,
            "proxima_mantencion": hoy - timedelta(days=6)  # Vencida
        },
        {
            "patente": "FRWZ-88",
            "modelo": "Bomba Los Cerros (B-2)",
            "tipo_v": "Carro bomba",
            "anio": 2019,
            "compania": "Compañía 2ª",
            "estado": "operativo",
            "kilometraje": 38100,
            "proxima_mantencion": hoy + timedelta(days=5)  # Por vencer
        },
        {
            "patente": "KTLM-05",
            "modelo": "Rescate Vehicular (R-1)",
            "tipo_v": "Rescate",
            "anio": 2021,
            "compania": "Compañía 3ª",
            "estado": "operativo",
            "kilometraje": 29400,
            "proxima_mantencion": hoy + timedelta(days=4)  # Por vencer
        },
        {
            "patente": "JNPX-47",
            "modelo": "Bomba Centro (B-3)",
            "tipo_v": "Carro bomba",
            "anio": 2022,
            "compania": "Compañía 4ª",
            "estado": "operativo",
            "kilometraje": 18200,
            "proxima_mantencion": hoy + timedelta(days=79) # Operativo
        },
        {
            "patente": "DGRT-63",
            "modelo": "Bomba Forestal (B-4)",
            "tipo_v": "Forestal",
            "anio": 2020,
            "compania": "Compañía 1ª",
            "estado": "operativo",
            "kilometraje": 31000,
            "proxima_mantencion": hoy + timedelta(days=95) # Operativo
        },
    ]

    for vdata in vehiculos_data:
        comp_obj = companias_objs.get(vdata["compania"])
        v_obj, v_created = Vehiculo.objects.get_or_create(
            patente=vdata["patente"],
            defaults={
                "modelo": vdata["modelo"],
                "tipo_v": vdata["tipo_v"],
                "anio": vdata["anio"],
                "id_compania": comp_obj,
                "estado": vdata["estado"],
                "kilometraje": vdata["kilometraje"],
                "proxima_mantencion": vdata["proxima_mantencion"]
            }
        )
        print(f"  [Vehículo] {'Creado' if v_created else 'Existente'}: {vdata['modelo']} ({vdata['patente']})")

        # Crear al menos un registro de mantención previo para cada vehículo
        if v_created:
            m_obj = Mantencion.objects.create(
                id_vehi=v_obj,
                id_tipo=tipos_objs["Preventiva"],
                descripcion="Mantención general preventiva y cambio de filtros",
                estado="completada",
                costo_t=70000,
                respon="Juan Pérez Soto"
            )

    print("Sembrado de datos finalizado con éxito.")

if __name__ == "__main__":
    run_seed()
