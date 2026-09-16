# core/views.py
from django.shortcuts import render, redirect
from django.contrib.auth import authenticate, login

def index(request):
    error = None
    if request.method == 'POST':
        # Capturar los datos enviados por el formulario
        username_input = request.POST.get('username')
        password_input = request.POST.get('password')

        # Validar las credenciales contra la base de datos
        user = authenticate(request, username=username_input, password=password_input)

        if user is not None:
            login(request, user)  # Iniciar la sesión del usuario
            return redirect('dashboard')  # Redirigir a la vista de dashboard
        else:
            error = "Usuario o contraseña incorrectos."

    return render(request, 'core/index.html', {'error': error})

def dashboard(request):
# Asegura que solo usuarios autenticados entren al dashboard
 if not request.user.is_authenticated:
     return redirect('index')
        
 return render(request, 'core/dashboard.html')