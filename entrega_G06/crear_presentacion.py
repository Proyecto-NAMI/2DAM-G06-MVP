from pathlib import Path
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle


ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "presentacion_G06.pdf"

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="Cover", parent=styles["Title"], fontName="Helvetica-Bold", fontSize=28, leading=34, textColor=colors.HexColor("#123B5D"), alignment=TA_CENTER, spaceAfter=12))
styles.add(ParagraphStyle(name="Subtitle", parent=styles["Normal"], fontName="Helvetica", fontSize=14, leading=20, textColor=colors.HexColor("#45677F"), alignment=TA_CENTER, spaceAfter=18))
styles.add(ParagraphStyle(name="SlideTitle", parent=styles["Heading1"], fontName="Helvetica-Bold", fontSize=22, leading=26, textColor=colors.HexColor("#123B5D"), spaceAfter=12))
styles.add(ParagraphStyle(name="Body", parent=styles["BodyText"], fontName="Helvetica", fontSize=12, leading=18, textColor=colors.HexColor("#23323D"), spaceAfter=8))
styles.add(ParagraphStyle(name="Small", parent=styles["BodyText"], fontName="Helvetica", fontSize=9, leading=12, textColor=colors.HexColor("#567080"), spaceAfter=4))
styles.add(ParagraphStyle(name="Card", parent=styles["BodyText"], fontName="Helvetica", fontSize=11, leading=16, textColor=colors.HexColor("#23323D"), leftIndent=8, rightIndent=8, spaceAfter=0))


def p(text, style="Body"):
    return Paragraph(text, styles[style])


def bullets(items):
    return [p("&#8226; " + item) for item in items]


def card(title, text):
    data = [[p(title, "Heading3")], [p(text, "Card")]]
    table = Table(data, colWidths=[170 * mm])
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#EAF4F8")),
        ("BOX", (0, 0), (-1, -1), 0.6, colors.HexColor("#8DB8C9")),
        ("TOPPADDING", (0, 0), (-1, -1), 9),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 9),
        ("LEFTPADDING", (0, 0), (-1, -1), 10),
        ("RIGHTPADDING", (0, 0), (-1, -1), 10),
    ]))
    return table


def footer(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(colors.HexColor("#C8D9E2"))
    canvas.line(18 * mm, 14 * mm, 192 * mm, 14 * mm)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#567080"))
    canvas.drawString(18 * mm, 9 * mm, "NAMI - Entrega G06")
    canvas.drawRightString(192 * mm, 9 * mm, f"{doc.page}")
    canvas.restoreState()


story = []
story.append(Spacer(1, 30 * mm))
story.append(p("NAMI", "Cover"))
story.append(p("Presentacion final del MVP", "Subtitle"))
story.append(p("Entrega G06", "Subtitle"))
story.append(Spacer(1, 12 * mm))
story.append(card("Proposito", "Mensajeria familiar accesible para personas mayores, con texto legible, acciones grandes, confirmaciones claras y privacidad."))
story.append(Spacer(1, 12 * mm))
story.append(p("Artefactos preparados para piloto y Moodle", "Small"))
story.append(PageBreak())

story.append(p("1. Flujo principal", "SlideTitle"))
story.extend(bullets([
    "Iniciar sesion con verificacion de telefono.",
    "Abrir una conversacion uno a uno.",
    "Componer texto o grabar una nota de voz.",
    "Enviar, confirmar el estado y reintentar sin duplicar.",
]))
story.append(Spacer(1, 8 * mm))
story.append(card("Estados seguros", "El borrador sobrevive a la recreacion de pantalla y los estados de carga, vacio, error, pendiente y exito son visibles y anunciables."))
story.append(PageBreak())

story.append(p("2. Arquitectura y seguridad", "SlideTitle"))
story.extend(bullets([
    "Kotlin, Jetpack Compose y Material 3.",
    "ViewModel y casos de uso para estado inmutable.",
    "Repositorios inyectables para Auth, Firestore y Storage.",
    "Firebase Emulator Suite para Authentication, Firestore, Storage y Functions.",
    "Reglas de seguridad y mensajes idempotentes con identificador de cliente.",
]))
story.append(Spacer(1, 8 * mm))
story.append(card("Privacidad", "Solo se usan cuentas y datos ficticios en debug. No se registran credenciales, telefonos reales, tokens, rutas privadas ni contenido sensible."))
story.append(PageBreak())

story.append(p("3. Accesibilidad y diseno", "SlideTitle"))
story.extend(bullets([
    "Texto escalable y preferencias locales hasta el 200 %.",
    "Objetivos tactiles amplios, etiquetas y foco visible.",
    "Lectura en voz alta y confirmacion anunciable de Mensaje enviado.",
    "Referencia de flujo en Figma y capturas locales versionadas.",
]))
story.append(Spacer(1, 8 * mm))
story.append(card("Alcance", "Las llamadas, el video, los grupos, los adjuntos no previstos y el cifrado E2E propio quedan fuera de este MVP, aunque aparezcan como exploraciones visuales."))
story.append(PageBreak())

story.append(p("4. Verificacion y entrega", "SlideTitle"))
story.extend(bullets([
    "README con clon limpio, compilacion, pruebas y Firebase Emulator Suite.",
    "VALIDACION.md con matriz RF/RNF, regresion CI, limitaciones y evidencias.",
    "SSD/uso-ia.md con cuatro casos de uso y revision humana declarada.",
    "Paquete entrega_G06 con PDF, fuente, manifiesto y APK debug disponible.",
]))
story.append(Spacer(1, 8 * mm))
story.append(card("Pasos externos", "Antes de Moodle se debe confirmar el acceso docente, ejecutar la auditoria TalkBack en dispositivo o emulador y obtener la segunda revision del equipo."))
story.append(Spacer(1, 14 * mm))
story.append(p("Identificador de entrega: G06", "Subtitle"))

doc = SimpleDocTemplate(str(OUTPUT), pagesize=A4, rightMargin=18 * mm, leftMargin=18 * mm, topMargin=18 * mm, bottomMargin=20 * mm, title="NAMI - Presentacion final G06", author="G06")
doc.build(story, onFirstPage=footer, onLaterPages=footer)
print(OUTPUT)
