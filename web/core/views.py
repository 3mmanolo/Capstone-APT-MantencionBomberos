# core/views.py
from django.shortcuts import render, redirect, get_object_or_404
from django.contrib.auth.models import User
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.decorators import login_required
from .models import Vehiculo, Usuario, Compania # Importa tu modelo Vehiculo
import json
from django.utils.safestring import mark_safe




def index(request):
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

@login_required(login_url='index')
def dashboard(request):
    perfil = request.user.perfil

    current_user_data = {
        'name': perfil.nombre,
        'role': perfil.rol,
        'comp': str(perfil.id_compania) if perfil.id_compania else 'Sin Compañía',
        'initials': ''.join([palabra[0].upper() for palabra in perfil.nombre.split()[:2]]),
        'isAdmin': request.user.is_superuser or perfil.rol.lower() == 'administrador'
    }

    # 1. Conteos directos con el ORM de Django
    # (Asegúrate de que 'estado' coincida con el nombre exacto de la columna en tu modelo Vehiculo)
    total_vehiculos = Vehiculo.objects.count()
    vehiculos_operativos = Vehiculo.objects.filter(estado__iexact='operativo').count()
    vehiculos_por_vencer = Vehiculo.objects.filter(estado__iexact='por vencer').count()
    vehiculos_vencidos = Vehiculo.objects.filter(estado__iexact='vencido').count()

    context = {
        'current_user': current_user_data,
        'total_vehiculos': total_vehiculos,
        'vehiculos_operativos': vehiculos_operativos,
        'vehiculos_por_vencer': vehiculos_por_vencer,
        'vehiculos_vencidos': vehiculos_vencidos,
    }

    return render(request, 'core/dashboard.html', context)


@login_required(login_url='index')
def registrar(request):
    perfil_usuario = request.user.perfil

    current_user_data = {
        'name': perfil_usuario.nombre,
        'rol': perfil_usuario.rol,
        'comp': str(perfil_usuario.id_compania) if perfil_usuario.id_compania else 'Sin Compañía',
        'initials': ''.join([palabra[0].upper() for palabra in perfil_usuario.nombre.split()[:2]]),
        'email': request.user.email,
    }

    return render(request, 'core/registrar.html', {'current_user': current_user_data})



from django.shortcuts import render, redirect
from django.contrib.auth.decorators import login_required

@login_required(login_url='index')
def perfil(request):
    perfil_usuario = request.user.perfil

    current_user_data = {
        'name': perfil_usuario.nombre,
        'rol': perfil_usuario.rol,
        'comp': str(perfil_usuario.id_compania) if perfil_usuario.id_compania else 'Sin Compañía',
        'initials': ''.join([palabra[0].upper() for palabra in perfil_usuario.nombre.split()[:2]]),
        'email': request.user.email,
    }

    return render(request, 'core/perfil.html', {'current_user': current_user_data})

def vehiculos(request):
    return render(request, 'core/vehiculos.html', )

def historial(request):
    return render(request, 'core/historial.html', )

from django.shortcuts import render, redirect
from django.contrib.auth.decorators import login_required
from .models import Usuario

# core/views.py

# core/views.py

@login_required(login_url='index')
def usuarios(request):
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

    users_data = []
    for u in Usuario.objects.select_related('id_compania', 'user').all():
        users_data.append({
            'id': u.id_user,
            'name': u.nombre,
            'email': u.user.email if u.user else 'Sin correo',
            'comp': u.id_compania.nombre if u.id_compania else 'Sin compañía',
            'role': u.rol,
            'initials': ''.join([w[0].upper() for w in u.nombre.split()[:2]]) if u.nombre else 'US'
        })

    is_admin = False
    if hasattr(request.user, 'perfil') and request.user.perfil:
        is_admin = request.user.is_superuser or (request.user.perfil.rol and request.user.perfil.rol.lower() == 'administrador')
    else:
        is_admin = request.user.is_superuser

    context = {
        'users_json': users_data,
        'current_user_json': {'isAdmin': is_admin},
        'companias': Compania.objects.all(),
    }
    
    return render(request, 'core/usuarios.html', context)