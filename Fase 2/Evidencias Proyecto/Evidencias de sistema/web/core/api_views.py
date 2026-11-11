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

#importaciones de permisos api
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.authtoken.models import Token
from rest_framework.response import Response
from rest_framework import status

from .models import Vehiculo, Usuario, Compania, Mantencion, TipoMantencion, Insumo, MantencionInsu
from .views import _estado_visual_vehiculo
from django_ratelimit.decorators import ratelimit

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

import random
from django.core.mail import send_mail
from django.conf import settings

@csrf_exempt
@require_http_methods(["POST"])
@ratelimit(key='ip', rate='5/m', method='POST', block=False)
def api_login(request):
    """POST /api/login/ - Autentica credenciales y envía código 2FA por correo."""
    if getattr(request, 'limited', False):
        return JsonResponse({'success': False, 'message': 'Demasiados intentos de inicio de sesión. Espera un minuto e inténtalo de nuevo.'}, status=429)
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
    if not perfil:
        return JsonResponse({'success': False, 'message': 'El usuario no tiene un perfil asociado.'}, status=400)

    # --- NUEVO: Generar y enviar código 2FA por correo ---
    codigo_aleatorio = f"{random.randint(100000, 999999)}"
    
    # Guardar el código y su expiración (5 minutos) en el perfil del usuario
    perfil.codigo_2fa = codigo_aleatorio
    perfil.expiracion_2fa = timezone.now() + timezone.timedelta(minutes=5)
    perfil.save()

    # Enviar correo electrónico
    correo_destino = user.email or username_input
    try:
        send_mail(
            subject='Código de Verificación - Sistema Bomberos',
            message=f'Hola {perfil.nombre or user.username}, tu código de verificación de doble factor es: {codigo_aleatorio}. Expira en 5 minutos.',
            from_email=settings.DEFAULT_FROM_EMAIL,
            recipient_list=[correo_destino],
            fail_silently=True,
        )
    except Exception as e:
        print(f"Advertencia al enviar correo 2FA a {correo_destino}: {e}")

    # Retornar señal de que falta el segundo paso
    return JsonResponse({
        'success': True,
        'requires_2fa': True,
        'message': f'Credenciales correctas. Código 2FA enviado (o generado en BD): {codigo_aleatorio}',
        'user_id': user.pk
    })


@csrf_exempt
@require_http_methods(["POST"])
def api_verificar_2fa(request):
    """POST /api/verify-2fa/ - Verifica el código de 6 dígitos enviado al correo."""
    data = _get_json_body(request)
    user_id = data.get('user_id')
    codigo_ingresado = str(data.get('codigo_2fa', '')).strip()

    if not user_id or not codigo_ingresado:
        return JsonResponse({'success': False, 'message': 'Proporcione el usuario y el código de verificación.'}, status=400)

    try:
        user = User.objects.get(pk=user_id)
        perfil = getattr(user, 'perfil', None)

        if perfil and perfil.codigo_2fa and perfil.codigo_2fa.strip() == codigo_ingresado:
            tiempo_actual = timezone.now()
            expiracion = perfil.expiracion_2fa
            if timezone.is_naive(expiracion):
                expiracion = timezone.make_aware(expiracion, timezone.get_current_timezone())

            if tiempo_actual <= expiracion:
                # Código correcto: limpiamos el código 2FA y retornamos el token de autenticación
                perfil.codigo_2fa = None
                perfil.expiracion_2fa = None
                perfil.save()

                token_obj, _ = Token.objects.get_or_create(user=user)
                nombre = perfil.nombre or (user.get_full_name() or user.username)
                rol = perfil.rol or ('Administrador' if user.is_superuser else 'Voluntario')
                compania_nombre = perfil.id_compania.nombre if perfil.id_compania else 'Sin Compañía'
                telefono = perfil.telefono or ''
                is_admin = user.is_superuser or (rol or '').lower() == 'administrador'
                initials = ''.join([w[0].upper() for w in nombre.split()[:2]]) if nombre else 'US'

                return JsonResponse({
                    'success': True,
                    'message': 'Autenticación exitosa.',
                    'token': token_obj.key,
                    'user': {
                        'id': user.pk,
                        'username': user.username,
                        'email': user.email,
                        'nombre': nombre,
                        'rol': rol,
                        'compania': compania_nombre,
                        'telefono': telefono,
                        'initials': initials,
                        'isAdmin': is_admin
                    }
                })
            else:
                return JsonResponse({'success': False, 'message': 'El código de verificación ha expirado. Reenvía uno nuevo.'}, status=400)
        else:
            return JsonResponse({'success': False, 'message': 'Código de verificación incorrecto.'}, status=400)
    except Exception as e:
        return JsonResponse({'success': False, 'message': f'Error al validar código: {str(e)}'}, status=500)


@csrf_exempt
@require_http_methods(["POST"])
def api_reenviar_2fa(request):
    """POST /api/reenviar-2fa/ - Genera y reenvía un nuevo código de 6 dígitos."""
    data = _get_json_body(request)
    user_id = data.get('user_id')

    if not user_id:
        return JsonResponse({'success': False, 'message': 'Proporcione el identificador del usuario.'}, status=400)

    try:
        user = User.objects.get(pk=user_id)
        perfil = getattr(user, 'perfil', None)
        if perfil:
            codigo_aleatorio = f"{random.randint(100000, 999999)}"
            perfil.codigo_2fa = codigo_aleatorio
            perfil.expiracion_2fa = timezone.now() + timezone.timedelta(minutes=5)
            perfil.save()

            correo_destino = user.email or user.username
            send_mail(
                subject='Nuevo Código de Verificación - Sistema Bomberos',
                message=f'Hola {perfil.nombre or user.username}, tu nuevo código de verificación es: {codigo_aleatorio}. Expira en 5 minutos.',
                from_email=settings.DEFAULT_FROM_EMAIL,
                recipient_list=[correo_destino],
                fail_silently=False,
            )
            return JsonResponse({'success': True, 'message': 'Se ha enviado un nuevo código a tu correo.'})
        else:
            return JsonResponse({'success': False, 'message': 'El usuario no tiene perfil asociado.'}, status=400)
    except Exception as e:
        return JsonResponse({'success': False, 'message': f'Error al reenviar el correo: {str(e)}'}, status=500)



@api_view(["GET"])
@permission_classes([IsAuthenticated])
@csrf_exempt
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

@api_view(["GET", "POST"])
@permission_classes([IsAuthenticated])
@csrf_exempt
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
        data = request.data if hasattr(request, 'data') and request.data else _get_json_body(request)
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

@api_view(["DELETE"])
@permission_classes([IsAuthenticated])
@csrf_exempt
def api_vehiculo_eliminar(request, vehiculo_id):
    """DELETE /api/vehiculos/<id>/ - Eliminar vehículo."""
    v_obj = Vehiculo.objects.filter(pk=vehiculo_id).first()
    if not v_obj:
        return JsonResponse({'success': False, 'message': 'Vehículo no encontrado.'}, status=404)
    v_obj.delete()
    return JsonResponse({'success': True, 'message': 'Vehículo eliminado correctamente.'})

@api_view(["POST"])
@permission_classes([IsAuthenticated])
@csrf_exempt
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

@api_view(["GET"])
@permission_classes([IsAuthenticated])
@csrf_exempt
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

@api_view(["GET"])
@permission_classes([IsAuthenticated])
@csrf_exempt
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

@api_view(["GET"])
@permission_classes([IsAuthenticated])
@csrf_exempt
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

@api_view(["GET", "POST"])
@permission_classes([IsAuthenticated])
@csrf_exempt
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
        password = data.get('password', '').strip()

        if not nombre or not email:
            return JsonResponse({'success': False, 'message': 'Nombre y correo son requeridos.'}, status=400)

        compania_obj = Compania.objects.filter(pk=compania_id).first() if compania_id else None

        if edit_id:
            u_obj = Usuario.objects.filter(pk=edit_id).first()
            if not u_obj:
                return JsonResponse({'success': False, 'message': 'Usuario no encontrado.'}, status=404)
            if password and u_obj.user:
                u_obj.user.set_password(password)
                u_obj.user.save()
        else:
            username_clean = email.split('@')[0]
            user_auth, _ = User.objects.get_or_create(username=username_clean, defaults={'email': email})
            user_auth.set_password(password if password else 'admin123')
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

@api_view(["GET"])
@permission_classes([IsAuthenticated])
def api_perfil(request):
    """GET /api/perfil/ - Información de perfil del usuario autenticado."""
    user = request.user
    perfil = getattr(user, 'perfil', None)

    nombre = perfil.nombre if (perfil and perfil.nombre) else (user.get_full_name() or user.username)
    rol = perfil.rol if (perfil and perfil.rol) else ('Administrador' if user.is_superuser else 'Voluntario')
    compania_nombre = perfil.id_compania.nombre if (perfil and perfil.id_compania) else 'Sin Compañía'
    telefono = perfil.telefono if perfil else ''

    return JsonResponse({
        'success': True,
        'perfil': {
            'nombre': nombre,
            'email': user.email or '',
            'rol': rol,
            'compania': compania_nombre,
            'telefono': telefono,
            'initials': ''.join([w[0].upper() for w in nombre.split()[:2]]) if nombre else 'US'
        }
    })

@api_view(["POST"])
@permission_classes([IsAuthenticated])
@csrf_exempt
def api_vehiculo_operativo(request, vehiculo_id):
    """POST /api/vehiculos/<id>/operativo/ - Marca el vehículo como operativo."""
    v_obj = Vehiculo.objects.filter(pk=vehiculo_id).first()
    if not v_obj:
        return JsonResponse({'success': False, 'message': 'Vehículo no encontrado.'}, status=404)

    v_obj.estado = 'operativo'
    v_obj.save()

    # 🔄 Actualizar el estado de todas las mantenciones ligadas a este vehículo a "completada"
    Mantencion.objects.filter(id_vehi=v_obj).update(estado='completada')

    return JsonResponse({'success': True, 'message': 'Marcado como operativo exitosamente.'})




@api_view(["GET"])
@permission_classes([IsAuthenticated])
@csrf_exempt
def api_vehiculo_pdf(request, vehiculo_id):
    """GET /api/vehiculos/<id>/pdf/ - Genera y descarga el informe PDF del vehículo."""
    import io
    from django.http import HttpResponse
    from django.shortcuts import get_object_or_404
    from reportlab.lib.pagesizes import letter
    from reportlab.lib import colors
    from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle
    from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle

    vehiculo = get_object_or_404(Vehiculo, pk=vehiculo_id)
    mantenciones = Mantencion.objects.filter(id_vehi=vehiculo).order_by('fecha_ter')

    response = HttpResponse(content_type='application/pdf')
    nombre_mod = getattr(vehiculo, 'modelo', 'Vehiculo')
    patente_val = getattr(vehiculo, 'patente', str(vehiculo_id))
    filename = f"Informe_{nombre_mod}_{patente_val}.pdf"
    response['Content-Disposition'] = f'attachment; filename="{filename}"'

    buffer = io.BytesIO()
    doc = SimpleDocTemplate(buffer, pagesize=letter, rightMargin=36, leftMargin=36, topMargin=36, bottomMargin=36)
    story = []

    styles = getSampleStyleSheet()
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Heading1'],
        fontSize=18,
        leading=22,
        textColor=colors.HexColor("#b91c1c"),
        fontName="Helvetica-Bold",
        spaceAfter=6
    )
    subtitle_style = ParagraphStyle(
        'DocSub',
        parent=styles['Normal'],
        fontSize=10,
        textColor=colors.HexColor("#666666"),
        spaceAfter=15
    )
    h2_style = ParagraphStyle(
        'Heading2',
        parent=styles['Heading2'],
        fontSize=13,
        textColor=colors.HexColor("#111827"),
        spaceBefore=12,
        spaceAfter=8,
        fontName="Helvetica-Bold"
    )

    nombre_vehiculo = getattr(vehiculo, 'modelo', str(vehiculo))
    patente = getattr(vehiculo, 'patente', 'Sin patente')
    story.append(Paragraph(f"Informe Técnico de Vehículo: {nombre_vehiculo}", title_style))
    story.append(Paragraph(f"Patente: {patente} | Ficha oficial de flota - Bomberos Talcahuano", subtitle_style))

    story.append(Paragraph("Resumen del Vehículo", h2_style))
    total_mantenciones = mantenciones.count()
    costo_total = sum(getattr(m, 'costo', 0) or 0 for m in mantenciones)

    datos_resumen = [
        ["Tipo de Vehículo:", getattr(vehiculo, 'tipo_v', 'Carro bomba'), "Año:", str(getattr(vehiculo, 'anio', '-'))],
        ["Kilometraje:", f"{getattr(vehiculo, 'kilometraje', 0)} km", "Estado:", getattr(vehiculo, 'estado', 'Operativo')],
        ["Total Mantenciones:", str(total_mantenciones), "Costo Total Acumulado:", f"${costo_total:,.0f}"]
    ]

    tabla_resumen = Table(datos_resumen, colWidths=[120, 150, 130, 140])
    tabla_resumen.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor("#f9fafb")),
        ('TEXTCOLOR', (0,0), (-1,-1), colors.HexColor("#374151")),
        ('FONTNAME', (0,0), (0,-1), 'Helvetica-Bold'),
        ('FONTNAME', (2,0), (2,-1), 'Helvetica-Bold'),
        ('BOTTOMPADDING', (0,0), (-1,-1), 6),
        ('TOPPADDING', (0,0), (-1,-1), 6),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#e5e7eb")),
    ]))
    story.append(tabla_resumen)
    story.append(Spacer(1, 15))

    story.append(Paragraph("Historial de Mantenciones", h2_style))
    encabezados = ["Fecha", "Trabajo / Tipo", "Taller", "Costo", "Estado"]
    filas_tabla = [encabezados]

    for m in mantenciones:
        fecha_ter = m.fecha_ter.strftime("%d/%m/%Y") if hasattr(m, 'fecha_ter') and m.fecha_ter else "-"
        descripcion = getattr(m, 'descripcion', getattr(m, 'observaciones', 'Mantención'))
        taller = getattr(m, 'taller', '-') or '-'
        costo = f"${getattr(m, 'costo', 0):,.0f}" if getattr(m, 'costo', None) else "$0"
        estado = getattr(m, 'estado', 'Pendiente')
        filas_tabla.append([fecha_ter, str(descripcion), str(taller), str(costo), str(estado)])

    if len(filas_tabla) == 1:
        filas_tabla.append(["-", "Sin mantenciones registradas", "-", "-", "-"])

    tabla_historial = Table(filas_tabla, colWidths=[70, 200, 110, 80, 80])
    tabla_historial.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor("#b91c1c")),
        ('TEXTCOLOR', (0,0), (-1,0), colors.whitesmoke),
        ('FONTNAME', (0,0), (-1,0), 'Helvetica-Bold'),
        ('FONTSIZE', (0,0), (-1,-1), 9),
        ('BOTTOMPADDING', (0,0), (-1,-1), 5),
        ('TOPPADDING', (0,0), (-1,-1), 5),
        ('ALIGN', (3,0), (3,-1), 'RIGHT'),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#e5e7eb")),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, colors.HexColor("#f9fafb")]),
    ]))
    story.append(tabla_historial)

    doc.build(story)
    pdf = buffer.getvalue()
    buffer.close()
    response.write(pdf)
    return response


