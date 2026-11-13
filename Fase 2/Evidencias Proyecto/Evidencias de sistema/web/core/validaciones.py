# core/validaciones.py
# Validaciones compartidas por las vistas web y la API.
# Cada función devuelve (valor, error): si error no es None, el valor no debe usarse.
import datetime
import re
from decimal import Decimal, InvalidOperation

from django.utils import timezone

ANIO_MINIMO = 1950

# Patentes chilenas: AA1234 (antigua), BBBB12 (desde 2007) y motos AAA12 / AAA123.
_PATENTE_RE = re.compile(r'^(?:[A-Z]{2}\d{4}|[A-Z]{4}\d{2}|[A-Z]{3}\d{2,3})$')
_FECHAS_VACIAS = {'', 'dd-mm-aaaa', 'yyyy-mm-dd', 'none', 'null'}
_FORMATOS_FECHA = ('%Y-%m-%d', '%d-%m-%Y', '%d/%m/%Y', '%Y/%m/%d')


def normalizar_patente(raw):
    """Devuelve la patente en mayúsculas con guion (p. ej. 'HXPL-21') o None si viene vacía."""
    texto = re.sub(r'[\s.\-]', '', str(raw or '')).upper()
    if not texto:
        return None, None
    if not _PATENTE_RE.match(texto):
        return None, ('Patente inválida. Usa el formato chileno: '
                      'AA-1234, BBBB-12 o, para motos, AAA-12 / AAA-123.')
    letras = re.match(r'^[A-Z]+', texto).group()
    return f"{letras}-{texto[len(letras):]}", None


def validar_anio(raw):
    """Año opcional; si viene, debe ser un número entre 1950 y el próximo año."""
    texto = str(raw or '').strip()
    if not texto:
        return None, None
    if not texto.isdigit():
        return None, 'El año debe ser un número (por ejemplo, 2020).'
    anio = int(texto)
    maximo = timezone.localdate().year + 1
    if not ANIO_MINIMO <= anio <= maximo:
        return None, f'El año debe estar entre {ANIO_MINIMO} y {maximo}.'
    return anio, None


def parse_fecha(raw):
    """Fecha opcional en formato AAAA-MM-DD, DD-MM-AAAA, DD/MM/AAAA o AAAA/MM/DD."""
    if isinstance(raw, datetime.date):
        return raw, None
    texto = str(raw or '').strip()
    if texto.lower() in _FECHAS_VACIAS:
        return None, None
    for fmt in _FORMATOS_FECHA:
        try:
            return datetime.datetime.strptime(texto, fmt).date(), None
        except ValueError:
            pass
    return None, 'Fecha inválida. Usa el formato DD-MM-AAAA.'


def parse_monto(raw):
    """Monto en pesos que puede venir como '$ 85.000', '85000' o 85000. No admite negativos."""
    texto = str(raw if raw is not None else '').strip()
    if '-' in texto:
        return None, 'El costo no puede ser negativo.'
    limpio = re.sub(r'[\s$.]', '', texto)
    if not limpio:
        return Decimal('0'), None
    if not limpio.isdigit():
        return None, 'El costo debe ser un número en pesos (por ejemplo, 85000).'
    return Decimal(limpio), None


def parse_cantidad(raw):
    """Cantidad de insumo mayor a 0 (admite decimales con punto o coma)."""
    texto = str(raw or '').strip().replace(',', '.')
    try:
        cantidad = Decimal(texto)
    except (InvalidOperation, ValueError):
        return None, 'debe ser un número'
    if cantidad <= 0:
        return None, 'debe ser mayor a 0'
    return cantidad, None
