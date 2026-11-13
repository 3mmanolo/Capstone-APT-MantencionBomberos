# core/reportes.py
# Informe PDF de un vehículo. Lo usan la vista web y la API (antes el código
# estaba duplicado y leía campos inexistentes: costo, taller, tipo, nombre -> defecto 41).
import io
import re
from xml.sax.saxutils import escape

from django.http import HttpResponse
from django.utils import timezone
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

from .models import Mantencion

ROJO = colors.HexColor("#b91c1c")
GRIS_BORDE = colors.HexColor("#e5e7eb")
GRIS_FONDO = colors.HexColor("#f9fafb")


def _pesos(valor):
    """Formato chileno: $ 1.234.567"""
    return "$ " + f"{int(valor or 0):,}".replace(",", ".")


def _texto(valor, por_defecto="-"):
    """Escapa el texto para ReportLab (los datos vienen de usuarios)."""
    valor = str(valor).strip() if valor is not None else ""
    return escape(valor) if valor else por_defecto


def generar_pdf_vehiculo(vehiculo):
    """Devuelve los bytes del informe técnico del vehículo."""
    from .views import _estado_visual_vehiculo  # import diferido para evitar ciclos

    mantenciones = (Mantencion.objects.filter(id_vehi=vehiculo)
                    .select_related('id_tipo', 'registrado_por')
                    .order_by('-fecha_in', '-id_mant'))

    styles = getSampleStyleSheet()
    titulo = ParagraphStyle('Titulo', parent=styles['Heading1'], fontSize=18, leading=22,
                            textColor=ROJO, fontName="Helvetica-Bold", spaceAfter=6)
    subtitulo = ParagraphStyle('Sub', parent=styles['Normal'], fontSize=10,
                               textColor=colors.HexColor("#666666"), spaceAfter=15)
    h2 = ParagraphStyle('H2', parent=styles['Heading2'], fontSize=13, spaceBefore=12, spaceAfter=8,
                        textColor=colors.HexColor("#111827"), fontName="Helvetica-Bold")
    celda = ParagraphStyle('Celda', parent=styles['Normal'], fontSize=8.5, leading=10.5)

    estado = _estado_visual_vehiculo(vehiculo, timezone.localdate())['label']
    costo_total = sum((m.costo_t or 0) for m in mantenciones)
    proxima = vehiculo.proxima_mantencion.strftime("%d/%m/%Y") if vehiculo.proxima_mantencion else "Sin programar"
    compania = vehiculo.id_compania.nombre if vehiculo.id_compania else "Sin compañía"

    story = [
        Paragraph(f"Informe técnico del vehículo: {_texto(vehiculo.modelo, 'Sin nombre')}", titulo),
        Paragraph(f"Patente: {_texto(vehiculo.patente, 'Sin patente')} | {escape(compania)} | "
                  f"Generado el {timezone.localdate().strftime('%d/%m/%Y')}", subtitulo),
        Paragraph("Resumen del vehículo", h2),
    ]

    resumen = Table([
        ["Tipo de vehículo:", vehiculo.tipo_v or "-", "Año:", str(vehiculo.anio or "-")],
        ["Kilometraje:", f"{int(vehiculo.kilometraje or 0):,} km".replace(",", "."), "Estado:", estado],
        ["Próxima mantención:", proxima, "Total mantenciones:", str(mantenciones.count())],
        ["Costo total acumulado:", _pesos(costo_total), "", ""],
    ], colWidths=[120, 150, 120, 150])
    resumen.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), GRIS_FONDO),
        ('TEXTCOLOR', (0, 0), (-1, -1), colors.HexColor("#374151")),
        ('FONTNAME', (0, 0), (0, -1), 'Helvetica-Bold'),
        ('FONTNAME', (2, 0), (2, -1), 'Helvetica-Bold'),
        ('TOPPADDING', (0, 0), (-1, -1), 6),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
        ('GRID', (0, 0), (-1, -1), 0.5, GRIS_BORDE),
    ]))
    story += [resumen, Spacer(1, 15), Paragraph("Historial de mantenciones", h2)]

    filas = [["Inicio", "Término", "Tipo / trabajo", "Responsable", "Costo", "Estado"]]
    for m in mantenciones:
        trabajo = f"<b>{_texto(m.id_tipo.nombre if m.id_tipo else None, 'Mantención')}</b>"
        if m.descripcion:
            trabajo += f"<br/>{_texto(m.descripcion)}"
        filas.append([
            m.fecha_in.strftime("%d/%m/%Y") if m.fecha_in else "-",
            m.fecha_ter.strftime("%d/%m/%Y") if m.fecha_ter else "-",
            Paragraph(trabajo, celda),
            Paragraph(_texto(m.respon), celda),
            _pesos(m.costo_t),
            (m.estado or "-").capitalize(),
        ])
    if len(filas) == 1:
        filas.append(["-", "-", "Sin mantenciones registradas", "-", "-", "-"])

    historial = Table(filas, colWidths=[55, 55, 190, 100, 70, 70], repeatRows=1)
    historial.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), ROJO),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
        ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
        ('FONTSIZE', (0, 0), (-1, -1), 8.5),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('TOPPADDING', (0, 0), (-1, -1), 5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 5),
        ('ALIGN', (4, 0), (4, -1), 'RIGHT'),
        ('GRID', (0, 0), (-1, -1), 0.5, GRIS_BORDE),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, GRIS_FONDO]),
    ]))
    story.append(historial)

    buffer = io.BytesIO()
    SimpleDocTemplate(buffer, pagesize=letter, rightMargin=36, leftMargin=36,
                      topMargin=36, bottomMargin=36,
                      title=f"Informe {vehiculo.patente or vehiculo.pk}").build(story)
    return buffer.getvalue()


def respuesta_pdf_vehiculo(vehiculo):
    """HttpResponse con el PDF como descarga y un nombre de archivo seguro."""
    nombre = re.sub(r'[^A-Za-z0-9_-]+', '_', f"Informe_{vehiculo.modelo or 'vehiculo'}_{vehiculo.patente or vehiculo.pk}")
    response = HttpResponse(generar_pdf_vehiculo(vehiculo), content_type='application/pdf')
    response['Content-Disposition'] = f'attachment; filename="{nombre.strip("_")}.pdf"'
    return response
