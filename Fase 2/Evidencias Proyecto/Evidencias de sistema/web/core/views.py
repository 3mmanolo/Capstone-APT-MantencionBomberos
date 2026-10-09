# core/views.py
import re
from decimal import Decimal, InvalidOperation
from django.db import transaction
from django.shortcuts import render, redirect, get_object_or_404
from django.contrib.auth.models import User
from django.db.models import Count
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.decorators import login_required
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError
from django.core.validators import validate_email
from django.utils import timezone
from .models import Vehiculo, Usuario, Compania, Mantencion, TipoMantencion, Insumo, MantencionInsu
import json
from django.utils.safestring import mark_safe


from django.urls import path
#from . import api_views
from rest_framework.authtoken.views import obtain_auth_token

urlpatterns = [
    #path('api/elementos/', api_views.lista_elementos, name='api_lista'),
    path('api/login/', obtain_auth_token, name='api_token_auth'), # Devuelve el token al usuario móvil
]

def index(request):
    # Si ya hay sesion activa, saltamos directo al dashboard en vez de
    # mostrar el formulario de login otra vez (esto es lo que generaba la
    # sensacion de que 'cerrar sesion' no hacia nada: el enlace del sidebar
    # apuntaba a esta misma vista en vez de a 'logout').
    if request.user.is_authenticated:
        return redirect('dashboard')

    error = None
    if request.method == 'POST':
        username_input = request.POST.get('username')
        password_input = request.POST.get('password')

        # Django autentica usando la tabla auth_user nativa
        user = authenticate(request, username=username_input, password=password_input)

        if user is not None:
            login(request, user)
            return redirect('dashboard')
        else:
            error = "Usuario o contraseña incorrectos."

    return render(request, 'core/index.html', {'error': error})

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


def cambiar_estado(request, id_vehi):
    vehiculo = get_object_or_404(Vehiculo, pk=id_vehi)
    hoy = timezone.localdate()
    vehiculo.estado = 'operativo'
    if not vehiculo.proxima_mantencion or vehiculo.proxima_mantencion <= hoy:
        vehiculo.proxima_mantencion = hoy + timezone.timedelta(days=90)
    vehiculo.save()
    
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

ROLES_USUARIO = ['Administrador', 'Encargado de flota', 'Voluntario']

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
                # Se elimina por la clave primaria sin importar la convención del ID
                Usuario.objects.filter(pk=user_id).delete()
            return redirect('usuarios')

        elif action == 'save':
            user_id = request.POST.get('edit_id')
            nombre = request.POST.get('nombre')
            rol = request.POST.get('rol')
            compania_id = request.POST.get('compania')

            if user_id:
                u = get_object_or_404(Usuario, pk=user_id)
                u.nombre = nombre
                u.rol = rol
                if compania_id:
                    u.id_compania_id = compania_id
                u.save()
                return redirect('usuarios')

            # Alta de un usuario nuevo: cuenta de acceso (auth_user) + perfil
            nombre = (nombre or '').strip()
            email = request.POST.get('email', '').strip().lower()
            password = request.POST.get('password', '')
            # El nombre de usuario para iniciar sesión es la parte del correo antes de la @
            username = email.split('@')[0]
            compania_obj = Compania.objects.filter(pk=compania_id).first() if compania_id else None

            if not nombre:
                errores.append('Ingresa el nombre completo.')
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

            # Se devuelve lo ingresado (menos la contraseña) para no perderlo
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