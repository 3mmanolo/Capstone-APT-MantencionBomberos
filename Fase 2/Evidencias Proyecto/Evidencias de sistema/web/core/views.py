# core/views.py
import re
import logging
logger = logging.getLogger(__name__)
from decimal import Decimal, InvalidOperation
from django.db import transaction
from django.shortcuts import render, redirect, get_object_or_404
from django.contrib.auth.models import User
from django.db.models import Count
from django.contrib.auth import authenticate, login, logout, authenticate, get_user_model
from django.contrib.auth.decorators import login_required
from django.views.decorators.http import require_POST
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError
from django.core.validators import validate_email
from django.utils import timezone
from django.contrib import messages 
from .models import Vehiculo, Usuario, Compania, Mantencion, TipoMantencion, Insumo, MantencionInsu
import json
from django.utils.safestring import mark_safe
from django_ratelimit.decorators import ratelimit
from .permissions import gestiona_flota


from django.urls import path
#from . import api_views
from rest_framework.authtoken.views import obtain_auth_token

#imports api_mail y generador codigo login
import secrets
from django.core.mail import send_mail
from django.conf import settings



urlpatterns = [
    #path('api/elementos/', api_views.lista_elementos, name='api_lista'),
    path('api/login/', obtain_auth_token, name='api_token_auth'), # Devuelve el token al usuario móvil
]

import secrets
from django.core.mail import send_mail
from django.conf import settings
from django.utils import timezone


User = get_user_model()

@ratelimit(key='ip', rate='5/m', method='POST', block=False)
def index(request):
    # Si el usuario hace clic en "Volver al login", limpiamos el estado 2FA en sesión
    if request.GET.get('reset') == '1':
        if 'pre_auth_user_id' in request.session:
            del request.session['pre_auth_user_id']
        return redirect('index')

    if request.user.is_authenticated:
        return redirect('dashboard')




    if request.user.is_authenticated:
        return redirect('dashboard')

    error = None
    info_message = None
    
    user_id = request.session.get('pre_auth_user_id')
    esperando_2fa = bool(user_id)

    # Validar vigencia del código en sesión usando el perfil
    if user_id:
        try:
            user = User.objects.get(pk=user_id)
            perfil = getattr(user, 'perfil', None)
            if perfil and perfil.expiracion_2fa:
                tiempo_actual = timezone.now()
                expiracion = perfil.expiracion_2fa
                if timezone.is_naive(expiracion):
                    expiracion = timezone.make_aware(expiracion, timezone.get_current_timezone())

                if tiempo_actual > expiracion:
                    del request.session['pre_auth_user_id']
                    esperando_2fa = False
                    error = "El código de verificación ha expirado. Inicia sesión nuevamente."
            else:
                del request.session['pre_auth_user_id']
                esperando_2fa = False
        except Exception:
            if 'pre_auth_user_id' in request.session:
                del request.session['pre_auth_user_id']
            esperando_2fa = False

    if request.method == 'POST':
        if getattr(request, 'limited', False):
            return render(request, 'core/index.html', {
                'error': 'Demasiados intentos. Espera un minuto e inténtalo de nuevo.',
                'info_message': None,
                'esperando_2fa': esperando_2fa
            })

        accion = request.POST.get('accion')

        # CASO 1: Reenviar código
        if accion == 'reenviar_codigo' and user_id:
            try:
                user = User.objects.get(pk=user_id)
                perfil = getattr(user, 'perfil', None)
                if perfil:
                    codigo_aleatorio = f"{secrets.randbelow(900000) + 100000}"
                    perfil.codigo_2fa = codigo_aleatorio
                    perfil.expiracion_2fa = timezone.now() + timezone.timedelta(minutes=5)
                    perfil.save()
                    
                    # Usar el campo mail del perfil (o email si aplica como respaldo)
                    correo_destino = getattr(perfil, 'mail', None) or getattr(perfil, 'email', None) or user.email or user.username
                    send_mail(
                        subject='Nuevo Código de Verificación - Sistema Bomberos',
                        message=f'Tu nuevo código de verificación es: {codigo_aleatorio}. Expira en 5 minutos.',
                        from_email=settings.DEFAULT_FROM_EMAIL,
                        recipient_list=[correo_destino],
                        fail_silently=False,
                    )
                    esperando_2fa = True
                    info_message = "Se ha enviado un nuevo código a tu correo."
            except Exception as e:
                logger.exception('Error al reenviar código 2FA')
                error = "No se pudo reenviar el código. Inténtalo más tarde."
                esperando_2fa = True

        # CASO 2: Verificar el código ingresado
        elif accion == 'verificar_2fa' and user_id:
            codigo_ingresado = request.POST.get('codigo_2fa', '').strip()
            try:
                user = User.objects.get(pk=user_id)
                perfil = getattr(user, 'perfil', None)
                
                if perfil and perfil.codigo_2fa and perfil.codigo_2fa.strip() == codigo_ingresado:
                    tiempo_actual = timezone.now()
                    expiracion = perfil.expiracion_2fa
                    if timezone.is_naive(expiracion):
                        expiracion = timezone.make_aware(expiracion, timezone.get_current_timezone())

                    if tiempo_actual <= expiracion:
                        # ¡Código correcto! Limpiamos los campos del perfil y logueamos de verdad
                        perfil.codigo_2fa = None
                        perfil.expiracion_2fa = None
                        perfil.save()
                        
                        del request.session['pre_auth_user_id']
                        login(request, user)
                        return redirect('dashboard')
                    else:
                        error = "El código de verificación ha expirado."
                        esperando_2fa = True
                else:
                    error = "Código de verificación incorrecto."
                    esperando_2fa = True
            except Exception as e:
                logger.exception('Error al validar código 2FA')
                error = "No se pudo validar el código. Inicia sesión nuevamente."
                if 'pre_auth_user_id' in request.session:
                    del request.session['pre_auth_user_id']
                esperando_2fa = False

        # CASO 3: Login inicial (Usuario y Contraseña)
        elif not esperando_2fa:
            username_input = request.POST.get('username')
            password_input = request.POST.get('password')

            if not username_input or not password_input:
                error = "Proporcione usuario y contraseña."
            else:
                # Intentar autenticar por username o email
                user = authenticate(request, username=username_input, password=password_input)
                if user is None and '@' in username_input:
                    user_obj = User.objects.filter(email__iexact=username_input).first()
                    if user_obj:
                        user = authenticate(request, username=user_obj.username, password=password_input)

                if user is not None:
                    perfil = getattr(user, 'perfil', None)
                    if perfil:
                        codigo_aleatorio = f"{secrets.randbelow(900000) + 100000}"
                        perfil.codigo_2fa = codigo_aleatorio
                        perfil.expiracion_2fa = timezone.now() + timezone.timedelta(minutes=5)
                        perfil.save()

                        # Tomar el correo desde el campo mail del perfil
                        correo_destino = getattr(perfil, 'mail', None) or getattr(perfil, 'email', None) or user.email or username_input
                        try:
                            send_mail(
                                subject='Código de Verificación - Sistema Bomberos',
                                message=f'Hola {getattr(perfil, "nombre", None) or user.username}, tu código de verificación es: {codigo_aleatorio}. Expira en 5 minutos.',
                                from_email=settings.DEFAULT_FROM_EMAIL,
                                recipient_list=[correo_destino],
                                fail_silently=False,
                            )
                            request.session['pre_auth_user_id'] = user.pk
                            esperando_2fa = True
                        except Exception as e:
                            logger.exception('Error al enviar código 2FA')
                            error = "No se pudo enviar el código de verificación. Inténtalo más tarde."
                    else:
                        error = "El usuario no tiene un perfil asociado."
                else:
                    error = "Usuario o contraseña incorrectos."

    return render(request, 'core/index.html', {
        'error': error,
        'info_message': info_message,
        'esperando_2fa': esperando_2fa
    })


def cerrar_sesion(request):
    logout(request)
    return redirect('index')  # Redirige a la página de login

def _datos_usuario_actual(request):
    """Devuelve (perfil, datos para la plantilla) del usuario en sesion.
    Tolera cuentas de acceso sin perfil (Usuario) asociado, que de otro modo
    provocan un error 500 al leer request.user.perfil."""
    perfil_usuario = getattr(request.user, 'perfil', None)
    is_admin = request.user.is_superuser or bool(
        perfil_usuario and (perfil_usuario.rol or '').lower() == 'administrador'
    )

    if perfil_usuario:
        nombre = perfil_usuario.nombre
        rol = perfil_usuario.rol
        comp = str(perfil_usuario.id_compania) if perfil_usuario.id_compania else 'Sin Compañía'
    else:
        nombre = request.user.get_full_name() or request.user.username
        rol = ''
        comp = 'Sin Compañía'

    return perfil_usuario, {
        'name': nombre,
        'rol': rol,
        'comp': comp,
        'initials': ''.join([palabra[0].upper() for palabra in nombre.split()[:2]]) or '??',
        'isAdmin': is_admin,
    }


@login_required(login_url='index')
def dashboard(request):
    perfil, current_user_data = _datos_usuario_actual(request)
    current_user_data['role'] = current_user_data['rol']

    context = {
        'current_user': current_user_data,
        'total_vehiculos': Vehiculo.objects.count(),
        'vehiculos_operativos': Vehiculo.objects.filter(estado__iexact='operativo').count(),
        'vehiculos_por_vencer': Vehiculo.objects.filter(estado__iexact='por vencer').count(),
        'vehiculos_vencidos': Vehiculo.objects.filter(estado__iexact='vencido').count(),
        'flota': Vehiculo.objects.select_related('id_compania').all(),
    }

    return render(request, 'core/dashboard.html', context)
    
@login_required(login_url='index')
def vehidetalle(request, vehiculo_id):
    vehiculo = get_object_or_404(Vehiculo, pk=vehiculo_id)
    mantenciones = Mantencion.objects.filter(id_vehi=vehiculo)
    
    context = {
        'vehiculo': vehiculo,
        'mantenciones': mantenciones,
    }
    return render(request, 'core/detalle.html', context)

@login_required(login_url='index')
@require_POST
def cambiar_estado(request, id_vehi):
    perfil_usuario, current_user_data = _datos_usuario_actual(request)
    
    # 🔒 Bloquear si es chofer
    if not gestiona_flota(request.user):
        messages.error(request, "No tienes permisos para realizar esta acción.")
        return redirect('dashboard')

    vehiculo = get_object_or_404(Vehiculo, pk=id_vehi)

    # Actualizar estado del vehículo a operativo sin modificar arbitrariamente la fecha
    vehiculo.estado = 'operativo'
    vehiculo.save()
    
    # 🔄 NUEVO: Actualizar el estado de todas las mantenciones ligadas a este vehículo a "completada"
    Mantencion.objects.filter(id_vehi=vehiculo).update(estado='completada')
    
    # Redirige usando el argumento posicional o vehiculo_id=id_vehi
    return redirect('vehidetalle', vehiculo_id=id_vehi)


@login_required(login_url='index')
def registrar(request):
    vehiculos = Vehiculo.objects.all()
    vehiculo_id_preseleccionado = request.GET.get('id')
    tipos_mantencion = TipoMantencion.objects.all()
    insumos_disponibles = Insumo.objects.all()

    perfil_usuario, current_user_data = _datos_usuario_actual(request)
    current_user_data['email'] = request.user.email

    # 🔒 Bloquear si el rol es chofer e intenta enviar datos (POST)
    if not gestiona_flota(request.user):
        messages.error(request, "No tienes permisos para realizar esta acción.")
        return redirect('dashboard') # O redirigir con un mensaje de error

    if request.method == 'POST':
        vehiculo_id = request.POST.get('vehiculo')
        tipo_id = request.POST.get('tipo')
        descripcion = request.POST.get('descripcion', '').strip()
        costo_raw = request.POST.get('costo', '')
        respon = request.POST.get('respon', '').strip()
        insumo_ids = request.POST.getlist('insumo_id')
        insumo_cants = request.POST.getlist('insumo_cant')

        errores = []
        vehiculo_obj = Vehiculo.objects.filter(pk=vehiculo_id).first() if vehiculo_id else None
        if not vehiculo_obj:
            errores.append('Selecciona un vehículo válido.')

        tipo_obj = TipoMantencion.objects.filter(pk=tipo_id).first() if tipo_id else None
        if not tipo_obj:
            errores.append('Selecciona un tipo de mantención válido.')

        # El input llega como "$ 0", "$ 85.000", etc. Nos quedamos solo con los dígitos.
        costo_manual = Decimal(re.sub(r'[^\d]', '', costo_raw) or '0')

        # Insumos: solo se consideran las filas donde se eligió un insumo real
        # y se indicó una cantidad mayor a 0. Filas vacías o incompletas se
        # ignoran en vez de marcarse como error, ya que son opcionales.
        insumos_validos = []  # [(Insumo, Decimal cantidad), ...]
        for insu_id, cant_raw in zip(insumo_ids, insumo_cants):
            if not insu_id or not cant_raw:
                continue
            insumo_obj = Insumo.objects.filter(pk=insu_id).first()
            if not insumo_obj:
                continue
            try:
                cantidad = Decimal(cant_raw)
            except (InvalidOperation, ValueError):
                continue
            if cantidad <= 0:
                continue
            insumos_validos.append((insumo_obj, cantidad))

        subtotal_insumos = sum((i.costo_uni * cant for i, cant in insumos_validos), Decimal('0'))
        costo_total = costo_manual + subtotal_insumos

        if not errores:
            with transaction.atomic():
                mantencion_obj = Mantencion.objects.create(
                    id_vehi=vehiculo_obj,
                    id_tipo=tipo_obj,
                    descripcion=descripcion,
                    estado='pendiente',
                    costo_t=costo_total,
                    respon=respon,
                )
                for insumo_obj, cantidad in insumos_validos:
                    MantencionInsu.objects.create(
                        id_mant=mantencion_obj,
                        id_insu=insumo_obj,
                        cant=cantidad,
                        costo_aso=insumo_obj.costo_uni * cantidad,
                    )
                vehiculo_obj.estado = 'en mantención'
                vehiculo_obj.save(update_fields=['estado'])
            return redirect('dashboard')

        context = {
            'vehiculos': vehiculos,
            'vehiculo_id_preseleccionado': vehiculo_id,
            'current_user': current_user_data,
            'tipos_mantencion': tipos_mantencion,
            'insumos_disponibles': insumos_disponibles,
            'errores': errores,
            'descripcion_valor': descripcion,
        }
        return render(request, 'core/registrar.html', context)

    context = {
        'vehiculos': vehiculos,
        'vehiculo_id_preseleccionado': vehiculo_id_preseleccionado,
        'current_user': current_user_data,
        'tipos_mantencion': tipos_mantencion,
        'insumos_disponibles': insumos_disponibles,
    }

    return render(request, 'core/registrar.html', context)



from django.shortcuts import render, redirect
from django.contrib.auth.decorators import login_required

@login_required(login_url='index')
def perfil(request):
    perfil_usuario, current_user_data = _datos_usuario_actual(request)
    current_user_data['email'] = request.user.email
    current_user_data['telefono'] = perfil_usuario.telefono if perfil_usuario else ''

    return render(request, 'core/perfil.html', {'current_user': current_user_data})

TIPOS_VEHICULO = ['Carro bomba', 'Rescate', 'Forestal', 'Aljibe', 'Ambulancia', 'Otro']

DIAS_ALERTA_VEHICULO = 7


def _estado_visual_vehiculo(vehiculo, hoy):
    """Calcula el estado visual (pill) de un vehiculo a partir de su estado
    operativo y de la fecha de proxima_mantencion real (no inventada)."""
    if (vehiculo.estado or '').strip().lower() == 'en mantención':
        return {'status': 'gray', 'label': 'En mantención'}
    if not vehiculo.proxima_mantencion:
        return {'status': 'gray', 'label': 'Sin programar'}
    dias = (vehiculo.proxima_mantencion - hoy).days
    if dias < 0:
        return {'status': 'danger', 'label': 'Mantención vencida'}
    if (vehiculo.estado or '').strip().lower() == 'operativo':
        return {'status': 'green', 'label': 'Operativo'}
    if dias <= DIAS_ALERTA_VEHICULO:
        label = 'Vence hoy' if dias == 0 else f"Vence en {dias} día{'s' if dias != 1 else ''}"
        return {'status': 'amber', 'label': label}
    return {'status': 'green', 'label': 'Operativo'}


@login_required(login_url='index')
def vehiculos(request):
    perfil_usuario, current_user_data = _datos_usuario_actual(request)
    is_admin = current_user_data['isAdmin']

    if not is_admin:
        return redirect('dashboard')

    errores = []

    if request.method == 'POST':
        action = request.POST.get('action')

        if action == 'delete':
            vehiculo_id = request.POST.get('vehiculo_id')
            if vehiculo_id:
                Vehiculo.objects.filter(pk=vehiculo_id).delete()
            return redirect('vehiculos')

        elif action == 'save':
            edit_id = request.POST.get('edit_id')
            patente = request.POST.get('patente', '').strip()
            modelo = request.POST.get('modelo', '').strip()
            tipo_v = request.POST.get('tipo_v', '').strip()
            anio_raw = request.POST.get('anio', '').strip()
            km_raw = request.POST.get('kilometraje', '').strip()
            compania_id = request.POST.get('compania')
            proxima_raw = request.POST.get('proxima_mantencion', '').strip()

            compania_obj = Compania.objects.filter(pk=compania_id).first() if compania_id else None
            if not compania_obj:
                errores.append('Selecciona una compañía válida.')
            if not modelo:
                errores.append('Ingresa el nombre del vehículo.')
            if not tipo_v:
                errores.append('Selecciona un tipo de vehículo.')

            anio = int(anio_raw) if anio_raw.isdigit() else None
            try:
                kilometraje = float(km_raw) if km_raw else 0
            except ValueError:
                kilometraje = 0

            # 🔒 Validación: El kilometraje no puede ser negativo
            if kilometraje < 0:
                errores.append('El kilometraje no puede ser menor a 0.')

            if not errores:
                vehiculo_obj = get_object_or_404(Vehiculo, pk=edit_id) if edit_id else Vehiculo()
                vehiculo_obj.patente = patente or None
                vehiculo_obj.modelo = modelo
                vehiculo_obj.tipo_v = tipo_v
                vehiculo_obj.anio = anio
                vehiculo_obj.kilometraje = kilometraje
                vehiculo_obj.id_compania = compania_obj
                vehiculo_obj.proxima_mantencion = proxima_raw or None
                if not edit_id:
                    vehiculo_obj.estado = 'operativo'
                try:
                    vehiculo_obj.save()
                    return redirect('vehiculos')
                except Exception:
                    errores.append('Ya existe un vehículo con esa patente.')

    hoy = timezone.localdate()
    vehiculos_data = []
    for v in Vehiculo.objects.select_related('id_compania').all():
        badge = _estado_visual_vehiculo(v, hoy)
        vehiculos_data.append({
            'id': v.pk,
            'modelo': v.modelo,
            'patente': v.patente or '',
            'compania_id': v.id_compania_id,
            'compania': v.id_compania.nombre if v.id_compania else 'Sin compañía',
            'tipo_v': v.tipo_v,
            'anio': v.anio or '',
            'kilometraje': str(v.kilometraje),
            'proxima_mantencion': v.proxima_mantencion.isoformat() if v.proxima_mantencion else '',
            'badge_status': badge['status'],
            'badge_label': badge['label'],
        })

    context = {
        'current_user': current_user_data,
        'vehiculos_json': vehiculos_data,
        'companias': Compania.objects.all(),
        'tipos_vehiculo': TIPOS_VEHICULO,
        'errores': errores,
    }
    return render(request, 'core/vehiculos.html', context)

@login_required(login_url='index')
def historial(request):
    # Cambiamos 'mantencion' por 'mantenciones'
    vehiculos = Vehiculo.objects.select_related('id_compania').annotate(
        num_registros=Count('mantenciones')
    ).all()

    perfil_usuario = getattr(request.user, 'perfil', None)
    is_admin = request.user.is_superuser or bool(
        perfil_usuario and (perfil_usuario.rol or '').lower() == 'administrador'
    )

    if perfil_usuario:
        nombre = perfil_usuario.nombre
        rol = perfil_usuario.rol
        comp = str(perfil_usuario.id_compania) if perfil_usuario.id_compania else 'Sin Compañía'
    else:
        nombre = request.user.get_full_name() or request.user.username
        rol = ''
        comp = 'Sin Compañía'

    current_user_data = {
        'name': nombre,
        'rol': rol,
        'comp': comp,
        'initials': ''.join([palabra[0].upper() for palabra in nombre.split()[:2]]) or '??',
        'isAdmin': is_admin,
    }

    context = {
        'vehiculos': vehiculos,
        'current_user': current_user_data,
        'current_user_json': {'isAdmin': is_admin},
    }
    return render(request, 'core/historial.html', context)

@login_required(login_url='index')
def detalle(request, vehiculo_id):
    # 1. Obtener datos del usuario actual igual que en el dashboard
    perfil, current_user_data = _datos_usuario_actual(request)
    current_user_data['role'] = current_user_data['rol']

    # 2. Obtener datos del vehículo y mantenciones
    vehiculo = get_object_or_404(Vehiculo, pk=vehiculo_id)
    mantenciones = Mantencion.objects.filter(id_vehi=vehiculo)

    context = {
        'current_user': current_user_data,  # <-- verifica si es admin el user
        'vehiculo': vehiculo,
        'mantenciones': mantenciones,
    }
    return render(request, 'core/historial-detalle.html', context)

from django.shortcuts import render, redirect
from django.contrib.auth.decorators import login_required
from .models import Usuario

# core/views.py

# core/views.py

ROLES_USUARIO = ['Administrador', 'Encargado de flota', 'Voluntario', 'Chofer']


@login_required(login_url='index')
def usuarios(request):
    is_admin = False
    if hasattr(request.user, 'perfil') and request.user.perfil:
        is_admin = request.user.is_superuser or (request.user.perfil.rol and request.user.perfil.rol.lower() == 'administrador')
    else:
        is_admin = request.user.is_superuser

    # Solo los administradores pueden ver y modificar las cuentas
    if not is_admin:
        return redirect('dashboard')

    errores = []
    form_data = None

    if request.method == 'POST':
        action = request.POST.get('action')

        if action == 'delete':
            user_id = request.POST.get('user_id')
            if user_id:
                perfil = Usuario.objects.select_related('user').filter(pk=user_id).first()
                if perfil and perfil.user_id == request.user.pk:
                    messages.error(request, "No puedes eliminar tu propia cuenta.")
                elif perfil and perfil.user:
                    # Borra la cuenta de acceso: por CASCADE se borran también su perfil y su token de API
                    perfil.user.delete()
                elif perfil:
                    perfil.delete()
            return redirect('usuarios')

        elif action == 'save':
            user_id = request.POST.get('edit_id')
            nombre = request.POST.get('nombre')
            email = request.POST.get('email', '').strip().lower()
            rol = request.POST.get('rol')
            compania_id = request.POST.get('compania')
            nueva_password = request.POST.get('password', '').strip()

            # --- CASO 1: EDICIÓN DE UN USUARIO EXISTENTE ---
            if user_id:
                u = get_object_or_404(Usuario, pk=user_id)
                u.nombre = nombre
                u.rol = rol
                compania_obj = Compania.objects.filter(pk=compania_id).first() if compania_id else None
                if compania_id:
                    u.id_compania_id = compania_id
                
                # Actualizar y validar correo si fue modificado
                if email and u.user:
                    try:
                        validate_email(email)
                    except ValidationError:
                        errores.append('Ingresa un correo válido.')
                    else:
                        if User.objects.filter(email__iexact=email).exclude(pk=u.user.pk).exists():
                            errores.append('Ya existe otro usuario con ese correo.')
                        else:
                            u.user.email = email
                            u.user.username = email.split('@')[0]
                            u.user.save()

                # Actualizar contraseña opcionalmente de forma segura
                if u.user and nueva_password:
                    try:
                        validate_password(nueva_password, user=u.user)
                        u.user.set_password(nueva_password)
                        u.user.save()
                    except ValidationError as e:
                        errores.extend(e.messages)

                if not errores:
                    u.save()
                    return redirect('usuarios')

            # --- CASO 2: ALTA DE UN USUARIO NUEVO ---
            else:
                nombre = (nombre or '').strip()
                password = request.POST.get('password', '')
                username = email.split('@')[0] if email else ''
                compania_obj = Compania.objects.filter(pk=compania_id).first() if compania_id else None

                if not nombre:
                    errores.append('Ingresa el nombre completo.')
                elif nombre.isdigit():
                    errores.append('El nombre no puede contener solo números.')
                try:
                    validate_email(email)
                except ValidationError:
                    errores.append('Ingresa un correo válido.')
                else:
                    if User.objects.filter(email__iexact=email).exists() or User.objects.filter(username__iexact=username).exists():
                        errores.append('Ya existe un usuario con ese correo.')
                if rol not in ROLES_USUARIO:
                    errores.append('Selecciona un rol válido.')
                if compania_id and not compania_obj:
                    errores.append('Selecciona una compañía válida.')
                try:
                    validate_password(password, user=User(username=username, email=email))
                except ValidationError:
                    errores.append('La contraseña debe tener al menos 8 caracteres, no ser solo números, '
                                   'no ser una clave común ni parecerse al correo.')

                if not errores:
                    with transaction.atomic():
                        nuevo = User.objects.create_user(username=username, email=email, password=password)
                        Usuario.objects.create(user=nuevo, nombre=nombre, rol=rol, id_compania=compania_obj)
                    return redirect('usuarios')

            # Si hay errores, guardamos los datos para no perderlos en el formulario
            form_data = {'name': nombre, 'email': email, 'role': rol, 'comp_id': compania_id or ''}

    users_data = []
    for u in Usuario.objects.select_related('id_compania', 'user').all():
        users_data.append({
            'id': u.id_user,
            'name': u.nombre,
            'email': (u.user.email if u.user else '') or 'Sin correo',
            'comp': u.id_compania.nombre if u.id_compania else 'Sin compañía',
            'comp_id': u.id_compania_id or '',
            'role': u.rol,
            'initials': ''.join([w[0].upper() for w in u.nombre.split()[:2]]) if u.nombre else 'US'
        })

    perfil_usuario = getattr(request.user, 'perfil', None)
    if perfil_usuario:
        nombre = perfil_usuario.nombre
        rol = perfil_usuario.rol
        comp = str(perfil_usuario.id_compania) if perfil_usuario.id_compania else 'Sin Compañía'
    else:
        nombre = request.user.get_full_name() or request.user.username
        rol = ''
        comp = 'Sin Compañía'

    current_user_data = {
        'name': nombre,
        'rol': rol,
        'comp': comp,
        'initials': ''.join([palabra[0].upper() for palabra in nombre.split()[:2]]) or '??',
        'isAdmin': is_admin,
    }

    context = {
        'users_json': users_data,
        'current_user': current_user_data,
        'current_user_json': {'isAdmin': is_admin},
        'companias': Compania.objects.all(),
        'errores': errores,
        'form_data': form_data,
    }

    return render(request, 'core/usuarios.html', context)





import io
from django.http import HttpResponse
from django.shortcuts import get_object_or_404
from django.contrib.auth.decorators import login_required
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from .models import Vehiculo, Mantencion  # Ajusta según el nombre exacto de tus modelos

@login_required(login_url='index')
def descargar_pdf_vehiculo(request, id_vehi):
    # 1. Obtener vehículo y sus mantenciones
    vehiculo = get_object_or_404(Vehiculo, pk=id_vehi)
    mantenciones = Mantencion.objects.filter(id_vehi=vehiculo).order_by('fecha_ter')  # Ajusta la relación según tu modelo

    # 2. Configurar la respuesta HTTP como PDF
    response = HttpResponse(content_type='application/pdf')
    filename = f"Informe_{vehiculo.modelo if hasattr(vehiculo, 'modelo') else vehiculo_modelo}_{vehiculo.patente if hasattr(vehiculo, 'patente') else vehiculo_id}.pdf"
    response['Content-Disposition'] = f'attachment; filename="{filename}"'

    # 3. Crear buffer y documento ReportLab
    buffer = io.BytesIO()
    doc = SimpleDocTemplate(buffer, pagesize=letter, rightMargin=36, leftMargin=36, topMargin=36, bottomMargin=36)
    story = []

    # Estilos
    styles = getSampleStyleSheet()
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Heading1'],
        fontSize=18,
        leading=22,
        textColor=colors.HexColor("#b91c1c"), # Color rojo institucional
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

    # Header / Título del PDF
    nombre_vehiculo = getattr(vehiculo, 'nombre', str(vehiculo))
    patente = getattr(vehiculo, 'patente', 'Sin patente')
    story.append(Paragraph(f"Informe Técnico de Vehículo: {nombre_vehiculo}", title_style))
    story.append(Paragraph(f"Patente: {patente} | Ficha oficial de flota - Bomberos Talcahuano", subtitle_style))

    # --- RESUMEN Y DATOS DEL VEHÍCULO ---
    story.append(Paragraph("Resumen del Vehículo", h2_style))
    
    # Calcular métricas simples de resumen
    total_mantenciones = mantenciones.count()
    costo_total = sum(getattr(m, 'costo', 0) or 0 for m in mantenciones)

    datos_resumen = [
        ["Tipo de Vehículo:", getattr(vehiculo, 'tipo', 'Carro bomba'), "Año:", str(getattr(vehiculo, 'anio', '-'))],
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

    # --- HISTORIAL COMPLETO DE MANTENCIONES ---
    story.append(Paragraph("Historial de Mantenciones", h2_style))

    encabezados = ["Fecha", "Trabajo / Tipo", "Taller", "Costo", "Estado"]
    filas_tabla = [encabezados]

    for m in mantenciones:
        fecha_ter = m.fecha_ter.strftime("%d/%m/%Y") if hasattr(m, 'fecha_ter') and m.fecha_ter else "-"
        descripcion = getattr(m, 'descripcion', getattr(m, 'tipo', 'Mantención'))
        taller = getattr(m, 'taller', '-') or '-'
        costo = f"${getattr(m, 'costo', 0):,.0f}" if getattr(m, 'costo', None) else "$0"
        estado = getattr(m, 'estado', 'Pendiente')

        filas_tabla.append([fecha_ter, descripcion, taller, costo, estado])

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
        ('ALIGN', (3,0), (3,-1), 'RIGHT'), # Alineación de costo a la derecha
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#e5e7eb")),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, colors.HexColor("#f9fafb")]),
    ]))
    story.append(tabla_historial)

    # Construir PDF
    doc.build(story)
    pdf = buffer.getvalue()
    buffer.close()
    response.write(pdf)
    return response