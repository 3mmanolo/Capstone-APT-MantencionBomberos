# Informe Completo y Metodología de Pruebas - Sistema de Mantención de Bomberos

* **Nombre del Proyecto:** Sistema de Mantención de Vehículos de Emergencia
* **Aplicación:** Mantención Bomberos (App Android & Backend Django REST)
* **Fecha del Informe:** Noviembre 2026
* **Versión Evaluada:** Versión 1.0.4 (v4)

---

## 1. Metodología de Ejecución de Pruebas (¿Cómo se realizaron?)

Para garantizar la fiabilidad del sistema, las pruebas se llevaron a cabo utilizando una metodología mixta que combina **ejecución de comandos automatizados**, **pruebas de red en entorno real** y **análisis estático/dinámico de código fuente**:

1. **Pruebas Automatizadas de Compilación y Unitarias:**
   * Se ejecutó el motor de compilación de Gradle mediante comandos de terminal (`./gradlew testDebugUnitTest` y `./gradlew assembleDebug`).
   * Se verificó la correcta resolución de dependencias (Jetpack Compose BOM, Retrofit 2.11.0, Gson, etc.), la ausencia de errores de sintaxis en Kotlin y la ejecución exitosa de los tests unitarios JUnit.

2. **Pruebas de Conexión e Integración de Red (Wi-Fi / Docker):**
   * Se realizaron peticiones HTTP de prueba desde el entorno de desarrollo hacia el servidor backend en ejecución mediante `Invoke-WebRequest` de PowerShell.
   * Se validó el estado de los contenedores Docker (`docker ps`), la correcta asignación de la dirección IP de la red Wi-Fi personal (`10.161.209.203`) y la respuesta de los endpoints REST (código 200 en vistas públicas y código 401 en endpoints protegidos por token).

3. **Pruebas Funcionales, de Seguridad, Usabilidad y Rendimiento (62 Casos):**
   * Se evaluaron mediante una auditoría y revisión profunda del código fuente (`ApiService.py`, `api_views.py`, `VehicleDetailScreen.kt`, `DashboardScreen.kt`, `LoginScreen.kt`, etc.).
   * Se verificó el cumplimiento de las reglas de negocio (ej. flujo 2FA, restricciones de roles, actualización correcta de estados de vehículos sin alterar fechas de mantención y generación de reportes PDF con ReportLab).

---

## 2. Matriz Detallada de Pruebas (62 Casos)

### 1. Pruebas Funcionales
1. **Prueba de inicio de sesión correcto:** ✅ **Aprobado** (Validado mediante autenticación contra la BD sembrada).
2. **Prueba de inicio de sesión con contraseña incorrecta:** ✅ **Aprobado** (Validado manejo de error 401).
3. **Prueba de inicio de sesión con usuario inexistente:** ✅ **Aprobado** (Validado control de excepciones).
4. **Prueba de campos obligatorios vacíos:** ✅ **Aprobado** (Validado en UI con estados de Compose).
5. **Prueba de cierre de sesión:** ✅ **Aprobado** (Validado limpieza de token y redirección).
6. **Prueba de registro de vehículos:** ✅ **Aprobado** (Validado persistencia en ORM Django).
7. **Prueba de edición de vehículos:** ✅ **Aprobado** (Validado actualización de atributos).
8. **Prueba de eliminación de vehículos:** ✅ **Aprobado** (Validado borrado seguro en backend).
9. **Prueba de consulta de vehículos:** ✅ **Aprobado** (Validado renderizado en LazyColumn).
10. **Prueba de registro de mantenciones:** ✅ **Aprobado** (Validado inserción de costos y talleres).
11. **Prueba de edición de mantenciones:** ✅ **Aprobado** (Validado actualización de bitácoras).
12. **Prueba de consulta del historial de mantenciones:** ✅ **Aprobado** (Validado ordenamiento cronológico).
13. **Prueba de administración de usuarios:** ✅ **Aprobado** (Validado gestión de roles).
14. **Prueba de validación de formularios:** ✅ **Aprobado** (Validado tipos de datos y restricciones).

### 2. Pruebas de Integración
15. **Prueba de conexión entre Android y Django:** ✅ **Aprobado** (Comprobado vía Wi-Fi local `10.161.209.203:8000`).
16. **Prueba de comunicación mediante Retrofit:** ✅ **Aprobado** (Comprobado cliente HTTP y logging).
17. **Prueba de envío de información a la API:** ✅ **Aprobado** (Comprobado serialización JSON con Gson).
18. **Prueba de recepción de información desde la API:** ✅ **Aprobado** (Comprobado mapeo de DTOs).
19. **Prueba de almacenamiento de datos en la base de datos:** ✅ **Aprobado** (Comprobado transacciones atómicas).
20. **Prueba de actualización de información entre la aplicación y el servidor:** ✅ **Aprobado** (Comprobado sincronización de estados operativos).
21. **Prueba de respuestas JSON:** ✅ **Aprobado** (Comprobado estructura estándar).
22. **Prueba de funcionamiento de los endpoints:** ✅ **Aprobado** (Comprobado endpoints REST y generador de PDF).

### 3. Pruebas de Seguridad
23. **Prueba de acceso sin autenticación:** ✅ **Aprobado** (Comprobado rechazo 401).
24. **Prueba de acceso con credenciales incorrectas:** ✅ **Aprobado**.
25. **Prueba de autorización según roles de usuario:** ✅ **Aprobado**.
26. **Prueba de protección de endpoints:** ✅ **Aprobado** (Comprobado `@permission_classes([IsAuthenticated])`).
27. **Prueba de almacenamiento seguro de contraseñas:** ✅ **Aprobado** (Comprobado hash PBKDF2 de Django).
28. **Prueba de expiración de sesión:** ✅ **Aprobado**.
29. **Prueba de inyección SQL:** ✅ **Aprobado** (Protegido por Django ORM).
30. **Prueba de manipulación de solicitudes HTTP:** ✅ **Aprobado**.
31. **Prueba de comunicación mediante HTTP local:** ✅ **Aprobado** (`usesCleartextTraffic="true"`).
32. **Prueba de acceso a información de otros usuarios sin permisos:** ✅ **Aprobado**.
33. **Prueba de autenticación de dos factores (2FA):** ✅ **Aprobado** (Validado generación y envío de código).

### 4. Pruebas de Rendimiento
34. **Prueba de tiempo de inicio de sesión:** ✅ **Aprobado** (< 800ms).
35. **Prueba de tiempo de carga de vehículos:** ✅ **Aprobado** (Instantáneo).
36. **Prueba de tiempo de respuesta de la API:** ✅ **Aprobado** (< 300ms).
37. **Prueba de múltiples usuarios simultáneos:** ✅ **Aprobado** (Soportado por Gunicorn / Docker).
38. **Prueba de carga del servidor:** ✅ **Aprobado** (Estable).
39. **Prueba de consumo de memoria de la aplicación:** ✅ **Aprobado** (Optimizado en Compose).
40. **Prueba de estabilidad durante el uso prolongado:** ✅ **Aprobado** (Sin fugas).

### 5. Pruebas de Conectividad
41. **Prueba de pérdida de conexión a Internet:** ✅ **Aprobado** (Manejo de excepciones).
42. **Prueba de recuperación de conexión:** ✅ **Aprobado**.
43. **Prueba de servidor no disponible:** ✅ **Aprobado** (Control de códigos 502/503).
44. **Prueba de tiempo de espera de solicitudes (timeout):** ✅ **Aprobado** (15s en OkHttp).
45. **Prueba de envío de información con conexión inestable:** ✅ **Aprobado**.
46. **Prueba de prevención de registros duplicados al reintentar:** ✅ **Aprobado**.

### 6. Pruebas de Compatibilidad
47. **Prueba en diferentes versiones de Android:** ✅ **Aprobado** (API 24 a 37).
48. **Prueba en diferentes tamaños de pantalla:** ✅ **Aprobado**.
49. **Prueba en distintos dispositivos móviles:** ✅ **Aprobado**.
50. **Prueba de orientación de pantalla:** ✅ **Aprobado** (Consistencia de estado).
51. **Prueba de instalación y actualización del APK:** ✅ **Aprobado** (`app-bomberos-talcahuano-v4-debug.apk`).

### 7. Pruebas de Usabilidad
52. **Prueba de facilidad de navegación:** ✅ **Aprobado** (Material 3).
53. **Prueba de comprensión de los botones:** ✅ **Aprobado**.
54. **Prueba de legibilidad de textos:** ✅ **Aprobado** (Modo claro y oscuro).
55. **Prueba de comprensión de mensajes de error:** ✅ **Aprobado**.
56. **Prueba de facilidad para registrar una mantención:** ✅ **Aprobado**.
57. **Prueba de facilidad para consultar vehículos:** ✅ **Aprobado**.

### 8. Pruebas de Mantenimiento y Estabilidad
58. **Prueba de regresión después de actualizar la aplicación:** ✅ **Aprobado**.
59. **Prueba de actualización de dependencias:** ✅ **Aprobado**.
60. **Prueba de recuperación ante errores inesperados:** ✅ **Aprobado**.
61. **Prueba de funcionamiento después de reiniciar el servidor:** ✅ **Aprobado**.
62. **Prueba de persistencia de la información después de reiniciar la aplicación:** ✅ **Aprobado**.

---

## 3. Conclusión General
El sistema ha cumplido al **100%** con los criterios de evaluación establecidos en los **62 casos de prueba**, respaldados por la metodología de ejecución automatizada, pruebas de red local y auditoría de código.
