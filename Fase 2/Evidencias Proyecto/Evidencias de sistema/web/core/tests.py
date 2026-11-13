# core/tests.py
# Pruebas automatizadas de FireFleet (defecto 46).
# Ejecutar con:  python manage.py test core
# Django crea una base de datos temporal para las pruebas: db.sqlite3 no se toca.
import datetime
import json

from django.contrib.auth.models import User
from django.core import mail
from django.core.cache import cache
from django.test import TestCase
from django.utils import timezone
from rest_framework.authtoken.models import Token

from .models import Compania, Insumo, Mantencion, TipoMantencion, Usuario, Vehiculo
from .validaciones import normalizar_patente, parse_fecha, parse_monto, validar_anio
from .views import _estado_visual_vehiculo

CLAVE = 'Prueba.2026x'


class Base(TestCase):
    """Datos mínimos: una compañía, un tipo, un insumo, un admin, un voluntario y un vehículo."""

    @classmethod
    def setUpTestData(cls):
        cls.hoy = timezone.localdate()
        cls.comp = Compania.objects.create(nombre='Compañía 1ª')
        cls.tipo = TipoMantencion.objects.create(nombre='Preventiva')
        cls.insumo = Insumo.objects.create(nombre='Filtro', costo_uni=10000, stock=5)
        cls.admin = cls._usuario('admin.prueba', 'Administrador')
        cls.vol = cls._usuario('vol.prueba', 'Voluntario')
        cls.vehiculo = Vehiculo.objects.create(
            patente='BBCC-12', tipo_v='Carro bomba', modelo='Bomba 1', anio=2020,
            id_compania=cls.comp, proxima_mantencion=cls.hoy + datetime.timedelta(days=60))

    @classmethod
    def _usuario(cls, username, rol):
        user = User.objects.create_user(username=username, email=f'{username}@prueba.cl', password=CLAVE)
        Usuario.objects.create(user=user, nombre=username.replace('.', ' ').title(), rol=rol, id_compania=cls.comp)
        return user

    def setUp(self):
        cache.clear()  # reinicia los límites de intentos (django-ratelimit) entre pruebas

    def api(self, metodo, url, user=None, data=None):
        extra = {'HTTP_AUTHORIZATION': f'Token {Token.objects.get_or_create(user=user)[0].key}'} if user else {}
        cuerpo = json.dumps(data) if data is not None else None
        return getattr(self.client, metodo)(url, cuerpo, content_type='application/json', **extra) \
            if cuerpo is not None else getattr(self.client, metodo)(url, **extra)


class ValidacionesTests(TestCase):
    def test_patente(self):
        self.assertEqual(normalizar_patente('hxpl21'), ('HXPL-21', None))
        self.assertEqual(normalizar_patente('ab 1234'), ('AB-1234', None))
        self.assertEqual(normalizar_patente(''), (None, None))
        self.assertIsNotNone(normalizar_patente('123412')[1])

    def test_anio(self):
        self.assertEqual(validar_anio('2020'), (2020, None))
        self.assertEqual(validar_anio(''), (None, None))
        self.assertIsNotNone(validar_anio('3050')[1])
        self.assertIsNotNone(validar_anio('dosmil')[1])

    def test_fecha_y_monto(self):
        self.assertEqual(parse_fecha('31-12-2026')[0], datetime.date(2026, 12, 31))
        self.assertEqual(parse_fecha('dd-mm-aaaa'), (None, None))
        self.assertIsNotNone(parse_fecha('31-31-2026')[1])
        self.assertEqual(parse_monto('$ 85.000')[0], 85000)
        self.assertIsNotNone(parse_monto('-5000')[1])


class AlertasTests(Base):
    def _estado(self, dias, estado='operativo'):
        v = Vehiculo(estado=estado, proxima_mantencion=self.hoy + datetime.timedelta(days=dias))
        return _estado_visual_vehiculo(v, self.hoy)['status']

    def test_estado_por_fecha(self):
        self.assertEqual(self._estado(-1), 'danger')                 # vencida
        self.assertEqual(self._estado(5), 'amber')                   # por vencer (defecto 55)
        self.assertEqual(self._estado(30), 'green')                  # operativo
        self.assertEqual(self._estado(5, 'en mantención'), 'gray')   # en el taller


class AutenticacionTests(Base):
    def test_login_web_con_2fa(self):
        r = self.client.post('/', {'username': 'admin.prueba', 'password': CLAVE})
        self.assertEqual(r.status_code, 200)
        self.assertEqual(len(mail.outbox), 1)
        codigo = Usuario.objects.get(user=self.admin).codigo_2fa
        self.assertEqual(len(codigo), 6)
        r = self.client.post('/', {'accion': 'verificar_2fa', 'codigo_2fa': '000000' if codigo != '000000' else '111111'})
        self.assertContains(r, 'incorrecto')
        r = self.client.post('/', {'accion': 'verificar_2fa', 'codigo_2fa': codigo})
        self.assertRedirects(r, '/dashboard/', fetch_redirect_response=False)

    def test_api_login_no_expone_codigo(self):
        r = self.client.post('/api/login/', json.dumps({'username': 'admin.prueba', 'password': CLAVE}),
                             content_type='application/json')
        codigo = Usuario.objects.get(user=self.admin).codigo_2fa
        self.assertTrue(r.json()['requires_2fa'])
        self.assertNotIn(codigo, r.content.decode())          # defecto 30
        r = self.client.post('/api/verify-2fa/', json.dumps({'user_id': self.admin.pk, 'codigo_2fa': codigo}),
                             content_type='application/json')
        self.assertEqual(len(r.json()['token']), 40)

    def test_limite_intentos_verify_2fa(self):
        for _ in range(5):
            self.client.post('/api/verify-2fa/', json.dumps({'user_id': self.admin.pk, 'codigo_2fa': '000000'}),
                             content_type='application/json')
        r = self.client.post('/api/verify-2fa/', json.dumps({'user_id': self.admin.pk, 'codigo_2fa': '000000'}),
                             content_type='application/json')
        self.assertEqual(r.status_code, 403)                   # defecto 34


class PermisosApiTests(Base):
    def test_sin_token_401(self):
        self.assertEqual(self.client.get('/api/dashboard/').status_code, 401)

    def test_voluntario_solo_lectura(self):
        self.assertEqual(self.api('get', '/api/dashboard/', self.vol).status_code, 200)
        r = self.api('post', '/api/usuarios/', self.vol, {'nombre': 'X', 'email': 'x@x.cl', 'password': CLAVE, 'rol': 'Administrador'})
        self.assertEqual(r.status_code, 403)                   # defecto 32
        self.assertEqual(self.api('delete', f'/api/vehiculos/{self.vehiculo.pk}/eliminar/', self.vol).status_code, 403)
        self.assertTrue(Vehiculo.objects.filter(pk=self.vehiculo.pk).exists())

    def test_alta_no_toma_cuenta_existente(self):
        r = self.api('post', '/api/usuarios/', self.admin,
                     {'nombre': 'Atacante', 'email': 'vol.prueba@otro.cl', 'password': 'Hackeado.2026', 'rol': 'Administrador'})
        self.assertEqual(r.status_code, 400)                   # defecto 31
        self.assertTrue(User.objects.get(pk=self.vol.pk).check_password(CLAVE))

    def test_registrar_mantencion_api(self):
        r = self.api('post', '/api/registrar-mantencion/', self.admin,
                     {'vehiculo_id': self.vehiculo.pk, 'tipo': 'Preventiva', 'proxima_mantencion': '99-99-2026'})
        self.assertEqual(r.status_code, 400)                   # defecto 44: fecha inválida ya no da 500
        r = self.api('post', '/api/registrar-mantencion/', self.admin,
                     {'vehiculo_id': self.vehiculo.pk, 'tipo': 'Preventiva', 'costo': '15000'})
        self.assertEqual(r.status_code, 200)
        self.assertEqual(Mantencion.objects.get(pk=r.json()['id']).registrado_por, self.admin)  # defecto 27

    def test_vehiculo_api_valida_anio(self):
        r = self.api('post', '/api/vehiculos/', self.admin, {'modelo': 'Nuevo', 'tipo': 'Rescate', 'anio': '3050'})
        self.assertEqual(r.status_code, 400)                   # defecto 44: ya no se guarda como 2024


class WebTests(Base):
    def setUp(self):
        super().setUp()
        self.client.force_login(self.admin)

    def test_paginas_cargan_con_csp(self):
        for url in ['/dashboard/', '/vehiculos/', '/usuarios/', '/registrar/', '/historial/', '/perfil/',
                    f'/detalle/{self.vehiculo.pk}/', f'/vehidetalle/{self.vehiculo.pk}/']:
            r = self.client.get(url)
            self.assertEqual(r.status_code, 200, url)
            self.assertIn('Content-Security-Policy', r.headers)
            self.assertEqual(r.headers.get('Cache-Control'), 'no-store')

    def test_operativo_solo_post(self):
        url = f'/vehiculo/{self.vehiculo.pk}/operativo/'
        self.assertEqual(self.client.get(url).status_code, 405)   # defecto 12
        self.assertEqual(self.client.post(url).status_code, 302)

    def _guardar_vehiculo(self, **datos):
        base = {'action': 'save', 'modelo': 'Rescate 2', 'tipo_v': 'Rescate', 'compania': self.comp.pk,
                'patente': '', 'anio': '', 'kilometraje': '0', 'proxima_mantencion': ''}
        base.update(datos)
        return self.client.post('/vehiculos/', base)

    def test_vehiculo_validaciones(self):
        self.assertContains(self._guardar_vehiculo(patente='bbcc12'), 'Ya existe un vehículo con la patente BBCC-12')  # defecto 21
        self.assertContains(self._guardar_vehiculo(anio='3050'), 'El año debe estar entre')                          # defecto 26
        self.assertContains(self._guardar_vehiculo(patente='123412'), 'Patente inválida')                           # defecto 19
        self.assertEqual(self._guardar_vehiculo(patente='kkll34', anio='2021').status_code, 302)
        self.assertTrue(Vehiculo.objects.filter(patente='KKLL-34').exists())

    def test_registrar_mantencion_validaciones(self):
        base = {'vehiculo': self.vehiculo.pk, 'tipo': self.tipo.pk, 'descripcion': 'x', 'respon': 'Taller'}
        r = self.client.post('/registrar/', {**base, 'costo': '-5000'})
        self.assertContains(r, 'no puede ser negativo')                                   # defecto 18
        r = self.client.post('/registrar/', {**base, 'costo': '0', 'insumo_id': [self.insumo.pk], 'insumo_cant': ['0']})
        self.assertContains(r, 'debe ser mayor a 0')                                      # defecto 18
        r = self.client.post('/registrar/', {**base, 'costo': '$ 5.000', 'insumo_id': [self.insumo.pk], 'insumo_cant': ['2']})
        self.assertEqual(r.status_code, 302)
        m = Mantencion.objects.latest('id_mant')
        self.assertEqual(m.costo_t, 25000)
        self.assertEqual(m.registrado_por, self.admin)                                    # defecto 27

    def test_editar_usuario_no_guarda_si_hay_error(self):
        perfil = Usuario.objects.get(user=self.vol)
        r = self.client.post('/usuarios/', {'action': 'save', 'edit_id': perfil.pk, 'nombre': 'Nuevo Nombre',
                                            'email': 'cambiado@prueba.cl', 'rol': 'SuperAdmin', 'compania': self.comp.pk})
        self.assertContains(r, 'Selecciona un rol válido')                               # defecto 43
        self.assertEqual(User.objects.get(pk=self.vol.pk).email, 'vol.prueba@prueba.cl')

    def test_editar_usuario_chofer(self):
        chofer = self._usuario('chofer.prueba', 'Chofer')
        perfil = Usuario.objects.get(user=chofer)
        r = self.client.get('/usuarios/')
        self.assertContains(r, '<option value="Chofer">Chofer</option>', html=True)   # el rol Chofer está en el formulario
        r = self.client.post('/usuarios/', {'action': 'save', 'edit_id': perfil.pk, 'nombre': 'Chofer Editado',
                                            'email': '', 'rol': 'Chofer', 'compania': self.comp.pk})
        self.assertEqual(r.status_code, 302)
        perfil.refresh_from_db()
        self.assertEqual((perfil.nombre, perfil.rol), ('Chofer Editado', 'Chofer'))

    def test_error_al_editar_conserva_modo_edicion(self):
        perfil = Usuario.objects.get(user=self.vol)
        r = self.client.post('/usuarios/', {'action': 'save', 'edit_id': perfil.pk, 'nombre': '',
                                            'email': '', 'rol': 'Voluntario', 'compania': self.comp.pk})
        self.assertEqual(r.context['form_data']['edit_id'], str(perfil.pk))

    def test_perfil_editar_contacto(self):
        r = self.client.post('/perfil/', {'nombre': 'Admin Prueba', 'telefono': '+56 9 1234 5678'})
        self.assertEqual(r.status_code, 302)                                               # defecto 25
        self.assertEqual(Usuario.objects.get(user=self.admin).telefono, '+56 9 1234 5678')
        self.assertContains(self.client.post('/perfil/', {'nombre': 'Admin', 'telefono': 'abc'}), 'Teléfono inválido')

    def test_pdf(self):
        Mantencion.objects.create(id_vehi=self.vehiculo, id_tipo=self.tipo, costo_t=45000, respon='Taller Sur')
        for url in [f'/vehiculo/{self.vehiculo.pk}/pdf/']:
            r = self.client.get(url)
            self.assertEqual(r['Content-Type'], 'application/pdf')
            self.assertTrue(r.content.startswith(b'%PDF'))                                 # defecto 41
        r = self.api('get', f'/api/vehiculos/{self.vehiculo.pk}/pdf/', self.admin)
        self.assertTrue(r.content.startswith(b'%PDF'))
