import json
import re
import datetime
from decimal import Decimal, InvalidOperation
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods
from django.contrib.auth import authenticate
from django.contrib.auth.models import User
from django.utils import timezone
from django.db import transaction
from django.db.models import Count

from .models import Vehiculo, Usuario, Compania, Mantencion, TipoMantencion, Insumo, MantencionInsu
from .views import _estado_visual_vehiculo

def _get_json_body(request):
    try:
        if request.body:
            return json.loads(request.body.decode('utf-8'))
    except Exception:
        pass
    return {}

def _parse_date_safe(date_str):
    if not date_str:
        return timezone.localdate() + timezone.timedelta(days=90)
    date_str = str(date_str).strip()
    if not date_str or date_str.lower() in ['dd-mm-aaaa', 'yyyy-mm-dd', 'none', 'null', '']:
        return timezone.localdate() + timezone.timedelta(days=90)
    for fmt in ('%Y-%m-%d', '%d-%m-%Y', '%d/%m/%Y', '%Y/%m/%d'):
        try:
            return datetime.datetime.strptime(date_str, fmt).date()
        except ValueError:
            pass
    return timezone.localdate() + timezone.timedelta(days=90)

@csrf_exempt
@require_http_methods(["POST"])
def api_login(request):
    """POST /api/login/ - Autentica usuario y retorna perfil."""
    data = _get_json_body(request)
    username_input = data.get('username') or data.get('email', '')
    password_input = data.get('password', '')

    if not username_input or not password_input:
        return JsonResponse({'success': False, 'message': 'Proporcione usuario/correo y contraseña.'}, status=400)

    # Intentar autenticar por username o por email
    user = authenticate(request, username=username_input, password=password_input)
    if user is None and '@' in username_input:
        user_obj = User.objects.filter(email__iexact=username_input).first()
        if user_obj:
            user = authenticate(request, username=user_obj.username, password=password_input)

    if user is None:
        return JsonResponse({'success': False, 'message': 'Usuario o contraseña incorrectos.'}, status=401)

    perfil = getattr(user, 'perfil', None)
    nombre = perfil.nombre if perfil else (user.get_full_name() or user.username)
    rol = perfil.rol if perfil else ('Administrador' if user.is_superuser else 'Voluntario')
    compania_nombre = perfil.id_compania.nombre if (perfil and perfil.id_compania) else 'Sin Compañía'
    telefono = perfil.telefono if perfil else ''
    is_admin = user.is_superuser or (rol or '').lower() == 'administrador'

    initials = ''.join([w[0].upper() for w in nombre.split()[:2]]) if nombre else 'US'

    return JsonResponse({
        'success': True,
        'message': 'Inicio de sesión exitoso.',
        'user': {
            'id': user.pk,
            'username': user.username,
            'email': user.email or username_input,
            'nombre': nombre,
            'rol': rol,
            'compania': compania_nombre,
            'telefono': telefono,
            'initials': initials,
            'isAdmin': is_admin
        }
    })

@csrf_exempt
@require_http_methods(["GET"])
def api_dashboard(request):
    """GET /api/dashboard/ - Retorna métricas y lista de la flota para la app Android."""
    hoy = timezone.localdate()
    flota = []
    vehiculos_operativos = 0
    vehiculos_por_vencer = 0
    vehiculos_vencidos = 0

    for v in Vehiculo.objects.select_related('id_compania').all():
        badge = _estado_visual_vehiculo(v, hoy)
        if badge['status'] == 'green':
            vehiculos_operativos += 1
            estado_text = "Operativo"
            estado_detalle = f"Próxima mantención en {(v.proxima_mantencion - hoy).days} días" if v.proxima_mantencion else "Sin programar"
            color_hex = "#34C759"
        elif badge['status'] == 'amber':
            vehiculos_por_vencer += 1
            dias = (v.proxima_mantencion - hoy).days if v.proxima_mantencion else 0
            estado_text = f"Vence en {dias} días" if dias > 0 else "Vence hoy"
            estado_detalle = "Programar mantención"
            color_hex = "#FF9500"
        elif badge['status'] == 'danger':
            vehiculos_vencidos += 1
            dias = (hoy - v.proxima_mantencion).days if v.proxima_mantencion else 0
            estado_text = "Mantención vencida"
            estado_detalle = f"Venció hace {dias} días" if dias > 0 else "Venció recientemente"
            color_hex = "#FF3B30"
        else:
            estado_text = "En mantención"
            estado_detalle = "En taller / revisión"
            color_hex = "#8E8E93"

        flota.append({
            'id': f"V-{v.pk}",
            'real_id': v.pk,
            'nombre': v.modelo,
            'patente': v.patente or 'Sin patente',
            'compania': v.id_compania.nombre if v.id_compania else 'Sin compañía',
            'estadoText': estado_text,
            'estadoDetalle': estado_detalle,
            'colorHex': color_hex,
            'kilometraje': float(v.kilometraje or 0),
            'tipo_v': v.tipo_v
        })

    return JsonResponse({
        'success': True,
        'stats': {
            'totales': len(flota),
            'operativos': vehiculos_operativos,
            'por_vencer': vehiculos_por_vencer,
            'vencidos': vehiculos_vencidos
        },
        'flota': flota
    })

@csrf_exempt
@require_http_methods(["GET", "POST"])
def api_vehiculos(request):
    """GET/POST /api/vehiculos/ - Listado o creación/edición de vehículos."""
    if request.method == "GET":
        hoy = timezone.localdate()
        vehiculos_list = []
        for v in Vehiculo.objects.select_related('id_compania').all():
            badge = _estado_visual_vehiculo(v, hoy)
            vehiculos_list.append({
                'id': v.pk,
                'nombre': v.modelo,
                'patente': v.patente or '',
                'tipo': v.tipo_v,
                'anio': str(v.anio or ''),
                'compania': v.id_compania.nombre if v.id_compania else 'Sin compañía',
                'compania_id': v.id_compania_id,
                'kilometraje': float(v.kilometraje or 0),
                'proxima_mantencion': v.proxima_mantencion.isoformat() if v.proxima_mantencion else '',
                'estado': badge['label'],
                'status_badge': badge['status']
            })
        return JsonResponse({'success': True, 'vehiculos': vehiculos_list})

    elif request.method == "POST":
        data = _get_json_body(request)
        edit_id = data.get('id') or data.get('edit_id')
        modelo = data.get('modelo', '').strip() or data.get('nombre', '').strip()
        patente = data.get('patente', '').strip()
        tipo_v = data.get('tipo', '').strip() or data.get('tipo_v', '').strip()
        anio = data.get('anio')
        km = data.get('kilometraje', 0)
        compania_id = data.get('compania_id')
        proxima = data.get('proxima_mantencion')

        if not modelo or not tipo_v:
            return JsonResponse({'success': False, 'message': 'Modelo y Tipo son requeridos.'}, status=400)

        if patente:
            existing = Vehiculo.objects.filter(patente__iexact=patente)
            if edit_id:
                existing = existing.exclude(pk=edit_id)
            if existing.exists():
                return JsonResponse({'success': False, 'message': f'La patente {patente} ya está registrada en la base de datos.'}, status=400)

        compania_obj = Compania.objects.filter(pk=compania_id).first() if compania_id else Compania.objects.first()
        if not compania_obj:
            compania_obj, _ = Compania.objects.get_or_create(nombre="Compañía 1ª")

        if edit_id:
            v_obj = Vehiculo.objects.filter(pk=edit_id).first()
            if not v_obj:
                return JsonResponse({'success': False, 'message': 'Vehículo no encontrado.'}, status=404)
        else:
            v_obj = Vehiculo()
            v_obj.estado = 'operativo'

        v_obj.modelo = modelo
        v_obj.patente = patente or None
        v_obj.tipo_v = tipo_v
        v_obj.id_compania = compania_obj

        if anio:
            try:
                v_obj.anio = int(re.sub(r'[^\d]', '', str(anio)) or '2024')
            except Exception:
                v_obj.anio = 2024
        else:
            v_obj.anio = 2024

        v_obj.kilometraje = Decimal(str(km)) if km else Decimal(0)
        v_obj.proxima_mantencion = _parse_date_safe(proxima)

        try:
            v_obj.save()
            return JsonResponse({'success': True, 'message': 'Vehículo guardado correctamente.', 'id': v_obj.pk})
        except Exception as e:
            return JsonResponse({'success': False, 'message': f'Error al guardar vehículo: {str(e)}'}, status=400)

@csrf_exempt
@require_http_methods(["DELETE"])
def api_vehiculo_eliminar(request, vehiculo_id):
    """DELETE /api/vehiculos/<id>/ - Eliminar vehículo."""
    v_obj = Vehiculo.objects.filter(pk=vehiculo_id).first()
    if not v_obj:
        return JsonResponse({'success': False, 'message': 'Vehículo no encontrado.'}, status=404)
    v_obj.delete()
    return JsonResponse({'success': True, 'message': 'Vehículo eliminado correctamente.'})

@csrf_exempt
@require_http_methods(["POST"])
def api_registrar_mantencion(request):
    """POST /api/registrar-mantencion/ - Registrar una mantención desde la app."""
    data = _get_json_body(request)
    vehiculo_id = data.get('vehiculo_id') or data.get('vehiculo')
    tipo_nombre = data.get('tipo', 'Preventiva')
    descripcion = data.get('descripcion', '').strip()
    costo_raw = str(data.get('costo', 0))
    taller = data.get('taller', '').strip() or data.get('respon', '').strip()
    materiales = data.get('materiales', '').strip()
    proxima_fecha = data.get('proxima_mantencion')

    v_obj = Vehiculo.objects.filter(pk=vehiculo_id).first() if vehiculo_id else None
    if not v_obj:
        return JsonResponse({'success': False, 'message': 'Vehículo no encontrado.'}, status=400)

    tipo_obj = TipoMantencion.objects.filter(nombre__iexact=tipo_nombre).first()
    if not tipo_obj:
        tipo_obj = TipoMantencion.objects.first()

    costo_val = Decimal(re.sub(r'[^\d]', '', costo_raw) or '0')

    desc_completa = descripcion
    if materiales:
        desc_completa += f" (Materiales: {materiales})"

    with transaction.atomic():
        mantencion_obj = Mantencion.objects.create(
            id_vehi=v_obj,
            id_tipo=tipo_obj,
            descripcion=desc_completa,
            estado='pendiente',
            costo_t=costo_val,
            respon=taller
        )

        v_obj.estado = 'en mantención'
        if proxima_fecha:
            v_obj.proxima_mantencion = proxima_fecha
        v_obj.save()

    return JsonResponse({'success': True, 'message': 'Mantención registrada exitosamente.', 'id': mantencion_obj.pk})

@csrf_exempt
@require_http_methods(["GET"])
def api_historial(request):
    """GET /api/historial/ - Retorna resumen de registros por vehículo."""
    vehiculos_annotated = Vehiculo.objects.select_related('id_compania').annotate(
        num_registros=Count('mantenciones')
    )

    hoy = timezone.localdate()
    items = []
    for v in vehiculos_annotated:
        badge = _estado_visual_vehiculo(v, hoy)
        color_hex = "#34C759" if badge['status'] == 'green' else ("#FF9500" if badge['status'] == 'amber' else "#FF3B30")
        items.append({
            'id': f"V-{v.pk}",
            'real_id': v.pk,
            'nombre': v.modelo,
            'patente': v.patente or 'Sin patente',
            'compania': v.id_compania.nombre if v.id_compania else 'Sin compañía',
            'registros': v.num_registros,
            'colorHex': color_hex
        })

    return JsonResponse({'success': True, 'historial': items})

@csrf_exempt
@require_http_methods(["GET"])
def api_historial_detalle(request, vehiculo_id):
    """GET /api/historial/<id>/ - Retorna lista detallada de mantenciones de un vehículo."""
    v_obj = Vehiculo.objects.filter(pk=vehiculo_id).first()
    if not v_obj:
        return JsonResponse({'success': False, 'message': 'Vehículo no encontrado.'}, status=404)

    mantenciones = Mantencion.objects.filter(id_vehi=v_obj).select_related('id_tipo').order_by('-fecha_in')
    logs = []
    for m in mantenciones:
        logs.append({
            'id': m.pk,
            'fecha': m.fecha_in.strftime('%d %b %Y') if m.fecha_in else '',
            'tipo': m.id_tipo.nombre if m.id_tipo else 'Mantención',
            'responsable': m.respon or 'Sin especificación',
            'kilometraje': f"{v_obj.kilometraje} km",
            'costo': float(m.costo_t or 0),
            'observaciones': m.descripcion or 'Sin observaciones'
        })

    return JsonResponse({
        'success': True,
        'vehiculo': {
            'id': f"V-{v_obj.pk}",
            'real_id': v_obj.pk,
            'nombre': v_obj.modelo,
            'patente': v_obj.patente or ''
        },
        'logs': logs
    })

@csrf_exempt
@require_http_methods(["GET"])
def api_notificaciones(request):
    """GET /api/notificaciones/ - Retorna alertas y notificaciones del sistema."""
    hoy = timezone.localdate()
    notificaciones = []

    for v in Vehiculo.objects.select_related('id_compania').all():
        if v.proxima_mantencion:
            dias = (v.proxima_mantencion - hoy).days
            if dias < 0:
                notificaciones.append({
                    'id': f"notif-v-{v.pk}",
                    'titulo': f"Mantención Vencida: {v.modelo}",
                    'mensaje': f"La mantención del vehículo {v.patente or ''} venció hace {abs(dias)} días.",
                    'tipo': 'danger',
                    'fecha': v.proxima_mantencion.strftime('%d/%m/%Y')
                })
            elif dias <= 7:
                notificaciones.append({
                    'id': f"notif-a-{v.pk}",
                    'titulo': f"Mantención Próxima: {v.modelo}",
                    'mensaje': f"Atención: Quedan {dias} días para la mantención de {v.patente or ''}.",
                    'tipo': 'warning',
                    'fecha': v.proxima_mantencion.strftime('%d/%m/%Y')
                })

    return JsonResponse({'success': True, 'notificaciones': notificaciones})

@csrf_exempt
@require_http_methods(["GET", "POST"])
def api_usuarios(request):
    """GET/POST /api/usuarios/ - Lista y gestión de usuarios."""
    if request.method == "GET":
        usuarios_list = []
        for u in Usuario.objects.select_related('id_compania', 'user').all():
            usuarios_list.append({
                'id': u.pk,
                'nombre': u.nombre,
                'email': u.user.email if (u.user and u.user.email) else '',
                'compania': u.id_compania.nombre if u.id_compania else 'Sin compañía',
                'compania_id': u.id_compania_id,
                'rol': u.rol,
                'telefono': u.telefono,
                'initials': ''.join([w[0].upper() for w in u.nombre.split()[:2]]) if u.nombre else 'US'
            })
        return JsonResponse({'success': True, 'usuarios': usuarios_list})

    elif request.method == "POST":
        data = _get_json_body(request)
        edit_id = data.get('id')
        nombre = data.get('nombre', '').strip()
        email = data.get('email', '').strip()
        rol = data.get('rol', '').strip()
        compania_id = data.get('compania_id')
        telefono = data.get('telefono', '').strip()

        if not nombre or not email:
            return JsonResponse({'success': False, 'message': 'Nombre y correo son requeridos.'}, status=400)

        compania_obj = Compania.objects.filter(pk=compania_id).first() if compania_id else None

        if edit_id:
            u_obj = Usuario.objects.filter(pk=edit_id).first()
            if not u_obj:
                return JsonResponse({'success': False, 'message': 'Usuario no encontrado.'}, status=404)
        else:
            username_clean = email.split('@')[0]
            user_auth, _ = User.objects.get_or_create(username=username_clean, defaults={'email': email})
            user_auth.set_password('admin123')
            user_auth.save()
            u_obj = Usuario(user=user_auth)

        u_obj.nombre = nombre
        u_obj.rol = rol or 'Voluntario'
        if compania_obj:
            u_obj.id_compania = compania_obj
        u_obj.telefono = telefono
        u_obj.save()

        if u_obj.user:
            u_obj.user.email = email
            u_obj.user.save()

        return JsonResponse({'success': True, 'message': 'Usuario guardado con éxito.', 'id': u_obj.pk})

@csrf_exempt
@require_http_methods(["GET"])
def api_perfil(request):
    """GET /api/perfil/ - Información de perfil."""
    user = request.user if request.user.is_authenticated else User.objects.first()
    perfil = getattr(user, 'perfil', None)

    nombre = perfil.nombre if perfil else (user.get_full_name() or user.username)
    rol = perfil.rol if perfil else ('Administrador' if user.is_superuser else 'Voluntario')
    compania_nombre = perfil.id_compania.nombre if (perfil and perfil.id_compania) else 'Sin Compañía'
    telefono = perfil.telefono if perfil else ''

    return JsonResponse({
        'success': True,
        'perfil': {
            'nombre': nombre,
            'email': user.email if user else '',
            'rol': rol,
            'compania': compania_nombre,
            'telefono': telefono,
            'initials': ''.join([w[0].upper() for w in nombre.split()[:2]]) if nombre else 'US'
        }
    })

@csrf_exempt
@require_http_methods(["POST"])
def api_vehiculo_operativo(request, vehiculo_id):
    """POST /api/vehiculos/<id>/operativo/ - Marca el vehículo como operativo."""
    v_obj = Vehiculo.objects.filter(pk=vehiculo_id).first()
    if not v_obj:
        return JsonResponse({'success': False, 'message': 'Vehículo no encontrado.'}, status=404)

    hoy = timezone.localdate()
    v_obj.estado = 'operativo'
    # Extender la fecha de próxima mantención a 90 días en el futuro para asegurar estado verde (Operativo)
    if not v_obj.proxima_mantencion or v_obj.proxima_mantencion <= hoy:
        v_obj.proxima_mantencion = hoy + timezone.timedelta(days=90)

    v_obj.save()
    return JsonResponse({'success': True, 'message': 'Marcado como operativo exitosamente.'})


