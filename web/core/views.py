# core/views.py
from django.shortcuts import render, redirect
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.decorators import login_required
from .models import Vehiculo  # Importa tu modelo Vehiculo
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

def usuarios(request):
    return render(request, 'core/usuarios.html', )