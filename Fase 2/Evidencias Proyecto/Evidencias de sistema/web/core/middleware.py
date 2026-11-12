# core/middleware.py
# Encabezados de seguridad agregados a todas las respuestas.

class SecurityHeadersMiddleware:
    # Content-Security-Policy: limita desde donde el navegador puede cargar recursos.
    # 'unsafe-inline' es necesario porque las plantillas usan <script> y style"" en linea.
    CSP = (
        "default-src 'self'; "
        "script-src 'self' 'unsafe-inline'; "
        "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; "
        "font-src 'self' https://cdnjs.cloudflare.com; "
        "img-src 'self' data:; "
        "connect-src 'self'; "
        "frame-ancestors 'none'; "
        "base-uri 'self'; "
        "form-action 'self' "
    )
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        response = self.get_response(request)
        response.setdefault('Content-Security-Policy', self.CSP)
        # Paginas con sesion: que el navegador no las guarde en caché
        #Evita ver datos con el boton de "Atrás" despues de cerrar sesion
        user = getattr(request, 'user', None)
        if user is not None and user.is_authenticated:
            response['Cache-Control'] = 'no-store'
        return response