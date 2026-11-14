# Plan de Pruebas - Sistema de Mantención de Vehículos de Emergencia

* **Nombre del Proyecto:** Sistema de Mantención de Vehículos de Emergencia
* **Aplicación:** Mantención Bomberos (App Android & Backend Django REST)
* **Institución:** Cuerpo de Bomberos de Talcahuano
* **Fecha:** Noviembre 2026
* **Versión del Documento:** Versión 1.0.4

---

## 1. Introducción
El presente **Plan de Pruebas** establece la estrategia, el alcance, los recursos y el cronograma para la validación y aseguramiento de calidad (**QA**) de la aplicación móvil **Mantención Bomberos** y su servidor backend asociado en Django. Su propósito es garantizar que el software cumpla con todos los requisitos funcionales, de seguridad, rendimiento y usabilidad antes de su despliegue operativo.

---

## 2. Objetivos del Plan de Pruebas
* Garantizar el correcto funcionamiento de los flujos de autenticación (Login y 2FA).
* Validar la integridad de la sincronización de datos entre la app móvil y el servidor backend a través de la red Wi-Fi local.
* Comprobar la estabilidad del sistema ante condiciones de red inestables o pérdida de conexión.
* Asegurar el cumplimiento de los niveles de servicio en cuanto a tiempos de respuesta y consumo de recursos.

---

## 3. Alcance de las Pruebas

### Qué se incluye (In-Scope)
* **Pruebas Funcionales:** Operaciones CRUD sobre vehículos, mantenciones y usuarios.
* **Pruebas de Integración:** Comunicación HTTP mediante Retrofit y endpoints REST de Django.
* **Pruebas de Seguridad:** Validación de tokens de sesión, control de acceso por roles y doble factor.
* **Pruebas de Rendimiento:** Tiempos de respuesta de la API y estabilidad del servidor en Docker.
* **Pruebas de Conectividad:** Funcionamiento en red Wi-Fi personal y gestión de timeouts.
* **Pruebas de Compatibilidad:** Verificación en versiones de Android desde API 24 hasta API 37.
* **Pruebas de Usabilidad y Estabilidad:** Experiencia de usuario en Jetpack Compose y persistencia de datos.

### Qué se excluye (Out-of-Scope)
* Pruebas de estrés masivas para más de 10,000 usuarios concurrentes (fuera del alcance institucional de Bomberos).
* Pruebas de hardware en dispositivos físicos obsoletos con versiones de Android inferiores a 7.0.

---

## 4. Estrategia y Niveles de Pruebas

### 4.1. Pruebas Unitarias (Unit Testing)
* **Responsable:** Desarrolladores / Automatización Gradle.
* **Herramientas:** JUnit, AndroidX Test.
* **Enfoque:** Validación de lógica aislada en componentes Kotlin y funciones auxiliares.

### 4.2. Pruebas de Integración (Integration Testing)
* **Responsable:** Equipo de QA.
* **Herramientas:** Retrofit Logging Interceptor, PowerShell (Invoke-WebRequest), Docker logs.
* **Enfoque:** Verificación de contratos JSON, códigos de estado HTTP y transacciones en base de datos.

### 4.3. Pruebas de Sistema y Funcionales (System Testing)
* **Responsable:** Evaluadores del proyecto Capstone.
* **Enfoque:** Ejecución de los 62 casos de prueba cubriendo pantallas, formularios, botones, cambios de estado operativo y descarga de PDF.

### 4.4. Pruebas de Aceptación de Usuario (UAT)
* **Responsable:** Personal operativo del Cuerpo de Bomberos.
* **Enfoque:** Validación final de la experiencia de uso en terreno mediante la instalación del APK (`app-bomberos-talcahuano-v4-debug.apk`).

---

## 5. Criterios de Aprobación y Rechazo

### Criterios de Aprobación (Exit Criteria)
* 100% de ejecución y éxito en las pruebas unitarias automatizadas (`BUILD SUCCESSFUL`).
* Cero defectos críticos (Severity 1) o bloqueantes sin resolver en los flujos principales de Login, Dashboard y Registro de Mantenciones.
* Correcta comunicación red Wi-Fi con el servidor Django en el puerto `8000`.

### Criterios de Suspensión y Reanudación
* Si el servidor backend experimenta fallas críticas de infraestructura (ej. caída de Docker), las pruebas de integración se suspenden hasta la restauración del servicio.

---

## 6. Entorno de Pruebas
* **Entorno Cliente:** Dispositivos móviles Android y emuladores (API 24 - 37).
* **Entorno Servidor:** Contenedores Docker (`web-web-1`, `web-db-1`) ejecutándose sobre Python 3.10 / Django 4.2 en red local Wi-Fi.
* **Herramientas de Soporte:** Android Studio, Gradle, Git, Docker Desktop, PowerShell.

---

## 7. Matriz de Responsabilidades (RACI)
* **Desarrollo / Programación:** Ejecución de pruebas unitarias y corrección de bugs.
* **Aseguramiento de Calidad (QA):** Ejecución de la matriz de 62 casos de prueba y reporte de incidencias.
* **Validación Operativa (Cliente):** Pruebas UAT en terreno por personal de Bomberos.
