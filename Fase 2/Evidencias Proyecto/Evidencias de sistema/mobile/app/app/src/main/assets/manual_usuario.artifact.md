# Manual de Usuario - Sistema de Mantención de Vehículos de Emergencia

* **Nombre del proyecto:** Sistema de Mantención de Vehículos de Emergencia - Cuerpo de Bomberos
* **Nombre de la aplicación:** Mantención Bomberos (App Móvil Android)
* **Logotipo:** Escudo institucional de Bomberos / Logotipo oficial integrado en la interfaz
* **Integrantes o responsables:** Equipo de Desarrollo Capstone
* **Institución:** Cuerpo de Bomberos de Talcahuano
* **Fecha y versión del manual:** Noviembre 2026 - Versión 1.0.4 (v4)

---

## 1. Introducción

### Presentación de la aplicación
**Mantención Bomberos** es una aplicación móvil desarrollada para dispositivos Android diseñada para agilizar y digitalizar el control operativo de la flota vehicular de emergencia (carros bomba, unidades de rescate, vehículos forestales y de apoyo).

### Propósito del manual
Este manual tiene como propósito guiar al usuario (voluntario, operador o administrador) paso a paso en el uso correcto de cada una de las funcionalidades de la aplicación móvil.

### A quién está dirigido
Está dirigido a todo el personal del Cuerpo de Bomberos que requiera consultar el estado de la flota, registrar mantenciones, cambiar estados operativos o descargar informes técnicos.

---

## 2. Descripción general de la aplicación

### ¿Qué es la aplicación?
Es una herramienta digital multiplataforma (cliente móvil Android y servidor backend web) que centraliza la bitácora de mantenimiento institucional.

### ¿Para qué sirve?
Sirve para mantener un registro transparente, oportuno y accesible de todas las revisiones mecánicas, preventivas y correctivas de los vehículos de emergencia.

### Principales funcionalidades
* Visualización en tiempo real del estado operativo de cada unidad (Operativo, Próximo a vencer, Vencido).
* Cambio de estado operativo de vehículos.
* Registro de nuevas mantenciones y costos asociados.
* Generación y descarga directa de informes técnicos en formato **PDF**.
* Autenticación segura con doble factor (2FA).

### Beneficios para Bomberos
* Reducción de tiempos administrativos.
* Garantía de disponibilidad operativa de la flota ante emergencias.
* Historial digital centralizado e inalterable.

---

## 3. Requisitos para utilizar la aplicación

### Dispositivo Android compatible
Cualquier teléfono inteligente o tablet con sistema operativo Android.

### Versión mínima de Android
* Android 7.0 (API 24) o superior.

### Conexión a Internet
* Conectividad a la red **Wi-Fi personal** institucional donde se encuentra alojado el servidor backend (IP local asignada, ej. `http://10.161.209.203:8000/`).

### Credenciales de acceso
* Usuario / Correo institucional y contraseña facilitados por el administrador del sistema (ej. `juan.perez` / `admin123`).

---

## 4. Instalación de la aplicación

### Cómo obtener el archivo APK
* Descargar el archivo de instalación ejecutable desde el Escritorio del computador: `app-bomberos-talcahuano-v4-debug.apk`.

### Cómo instalarlo en el celular
1. Transfiera el archivo APK a su dispositivo Android (vía cable USB, WhatsApp, Google Drive o correo).
2. Abra el archivo APK desde su administrador de archivos.
3. Si el sistema lo solicita, autorice la instalación de aplicaciones de "Fuentes desconocidas".
4. Presione **Instalar** y espere a que finalice el proceso.

### Permisos necesarios
* **Internet y Estado de Red:** Para comunicarse con el servidor backend.
* **Almacenamiento (en versiones anteriores de Android):** Para guardar el informe PDF en la carpeta Descargas.
* **Notificaciones:** Para recibir alertas de mantenciones próximas a vencer.

### Primer inicio de la aplicación
Al abrir la aplicación por primera vez, se mostrará la pantalla de bienvenida y el formulario de inicio de sesión.

---

## 5. Inicio de sesión

### Cómo ingresar a la aplicación
Abra la aplicación buscando el icono de **Mantención Bomberos** en su cajón de aplicaciones.

### Cómo introducir usuario y contraseña
1. En el campo **Usuario o correo institucional**, escriba su nombre de usuario (ej. `juan.perez`).
2. En el campo **Contraseña**, introduzca su clave de acceso.
3. Presione el botón rojo **Iniciar Sesión**.

### Qué hacer si las credenciales son incorrectas
Si el sistema muestra *"Usuario o contraseña incorrectos"*, verifique que esté escribiendo correctamente los datos o solicite un restablecimiento de clave al administrador.

### Cómo solucionar problemas de conexión
Si aparece *"Error de conexión"*:
* Compruebe que su celular esté conectado a la misma red Wi-Fi que el servidor.
* Verifique que la dirección IP configurada en la app sea la correcta.

---

## 6. Pantalla principal

### Descripción de la pantalla de inicio (Dashboard)
Muestra un saludo personalizado, las estadísticas generales de la flota (vehículos totales, operativos, por vencer) y tarjetas detalladas de cada unidad de emergencia con su código de color (Verde, Naranja, Rojo).

### Menú de navegación
Barra inferior con accesos directos a **Inicio**, **Registrar**, **Historial**, **Panel Admin** y **Perfil**.

### Botones y opciones disponibles
* Tarjetas de vehículos desplegables.
* Botón de actualización (deslizar hacia abajo).

### Acceso a los diferentes módulos
Tocar cualquier vehículo de la lista abre su pantalla de detalle con opciones de gestión avanzadas.

---

## 7. Uso de los módulos de la aplicación

### Gestión de vehículos
* Permite dar de alta, editar o cambiar el estado operativo de los vehículos.

### Consulta de vehículos
* Visualización de placas patentes, compañía, kilometraje y estado visual actual.

### Registro de mantenciones
* Formulario para ingresar tipo de trabajo, taller responsable, costos, kilometraje y observaciones.

### Consulta del historial de mantenciones
* Listado cronológico de todas las intervenciones realizadas a la flota.

### Administración de usuarios (si está disponible)
* Gestión de perfiles y roles (Administrador / Voluntario).

---

## 8. Gestión de la cuenta

### Información del usuario
* Visualización del nombre, rol, compañía y correo en la sección de **Perfil**.

### Cierre de sesión
* Botón **Cerrar Sesión** para finalizar la sesión actual de forma segura.

---

## 9. Mensajes y errores frecuentes

* **Credenciales incorrectas:** Verifique usuario y contraseña.
* **Error de conexión / No se puede conectar:** Verifique que esté en la red Wi-Fi correcta y el servidor encendido.
* **Campos obligatorios:** Rellene todos los campos requeridos en los formularios.

---

## 10. Preguntas frecuentes

* **¿Qué hago si no puedo iniciar sesión?** Compruebe su conexión Wi-Fi y sus credenciales.
* **¿Qué hago si no aparecen los vehículos?** Deslice hacia abajo para actualizar los datos desde el servidor.
* **¿Qué sucede si pierdo la conexión a Internet?** La app requiere conexión Wi-Fi con el servidor local para consultar y registrar datos.

---

## 11. Soporte y contacto

* **Responsable de la aplicación:** Equipo de Desarrollo Capstone - Bomberos Talcahuano.
* **Medio de contacto:** Soporte técnico institucional interno.
* **Procedimiento para reportar problemas:** Anotar el mensaje de error y notificar al administrador de sistemas.

---

## 12. Glosario

* **APK:** Paquete de instalación para aplicaciones Android.
* **Dashboard:** Panel visual principal de control y métricas.
* **API REST:** Sistema de comunicación entre la aplicación móvil y el servidor backend.
* **PDF:** Formato de documento portátil para informes técnicos descargables.
