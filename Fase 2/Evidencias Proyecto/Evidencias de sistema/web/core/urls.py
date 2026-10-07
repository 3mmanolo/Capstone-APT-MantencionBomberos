"""
URL configuration for core project.

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/4.2/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""
from django.contrib import admin
from django.urls import path
from core import views, api_views

urlpatterns = [
    path('admin/', admin.site.urls),
    path('', views.index, name='index'),
    path('dashboard/', views.dashboard, name='dashboard'),
    path('registrar/', views.registrar, name='registrar'),
    path('perfil/', views.perfil, name='perfil'),
    path('logout/', views.cerrar_sesion, name='logout'),
    path('vehiculos/', views.vehiculos, name='vehiculos'),
    path('historial/', views.historial, name='historial'),
    path('usuarios/', views.usuarios, name='usuarios'),
    path('detalle/<int:vehiculo_id>/', views.detalle, name='detalle'),
    path('vehidetalle/<int:vehiculo_id>/', views.vehidetalle, name='vehidetalle'),
    path('vehiculo/<int:id_vehi>/operativo/', views.cambiar_estado, name='cambiar_estado'),

    # Endpoints API REST para la aplicación móvil Android
    path('api/login/', api_views.api_login, name='api_login'),
    path('api/dashboard/', api_views.api_dashboard, name='api_dashboard'),
    path('api/vehiculos/', api_views.api_vehiculos, name='api_vehiculos'),
    path('api/vehiculos/<int:vehiculo_id>/eliminar/', api_views.api_vehiculo_eliminar, name='api_vehiculo_eliminar'),
    path('api/registrar-mantencion/', api_views.api_registrar_mantencion, name='api_registrar_mantencion'),
    path('api/historial/', api_views.api_historial, name='api_historial'),
    path('api/historial/<int:vehiculo_id>/', api_views.api_historial_detalle, name='api_historial_detalle'),
    path('api/notificaciones/', api_views.api_notificaciones, name='api_notificaciones'),
    path('api/usuarios/', api_views.api_usuarios, name='api_usuarios'),
    path('api/perfil/', api_views.api_perfil, name='api_perfil'),
]
