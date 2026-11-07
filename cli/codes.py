import re

CODE = r"([0-9A-Z]{4,8}|\d{3}-\d{3})"
AMOUNT = r"\$\s?([\d.]+)"

RULES = (
    (
        "Banco de Chile",
        "Payment",
        re.compile(
            rf"Autorizar el Pago por {AMOUNT}.*?Clave Dinamica (\d+)", re.I | re.S
        ),
    ),
    (
        "Banco de Chile",
        "Transfer",
        re.compile(
            rf"transferencia de {AMOUNT} a RUT (\d+).*?Clave Dinamica (\d+)",
            re.I | re.S,
        ),
    ),
    (
        "Banco de Chile",
        "App registration",
        re.compile(r"codigo (\d+) para registrarse en la App del Banco", re.I),
    ),
    (
        "Banco de Chile",
        "Mobile authorization",
        re.compile(r"Codigo Autorizacion Banco de Chile Movil (\d+)", re.I),
    ),
    (
        "BancoEstado",
        "Payment",
        re.compile(r"tercera clave (\d+) para realizar su pago via BancoEstado", re.I),
    ),
    (
        "BancoEstado",
        "Password change",
        re.compile(
            r"codigo (\d+) para completar tu cambio de clave.*BancoEstado", re.I
        ),
    ),
    (
        "SII",
        "Phone registration",
        re.compile(rf"SII.*codigo de validacion {CODE}", re.I),
    ),
    ("WhatsApp", "Verification", re.compile(rf"Codigo de WhatsApp: {CODE}", re.I)),
    (
        "Entel",
        "Verification",
        re.compile(rf"codigo de verificacion Entel es {CODE}", re.I),
    ),
    ("Uber", "Verification", re.compile(r"codigo Uber(?: es|:) (\d+)", re.I)),
    (
        "GoDaddy",
        "Verification",
        re.compile(rf"verificación de GoDaddy es {CODE}", re.I),
    ),
    (
        "Capital.com",
        "Verification",
        re.compile(r"Capital\.com .*codigo.*? (\d{4,8})", re.I),
    ),
)

GENERIC = re.compile(
    r"(?:c[oó]digo|code|clave|PIN|OTP)\D{0,40}?\b(\d{4,8}|\d{3}-\d{3})\b", re.I
)


def parse(message):
    body = message["body"]
    for service, kind, pattern in RULES:
        found = pattern.search(body)
        if found:
            return build(message, service, kind, found.groups())
    if message["sent"]:
        return None
    found = GENERIC.search(body)
    if found:
        sender = message["name"] or message["address"]
        return build(message, sender, "Verification", found.groups())
    return None


def build(message, service, kind, groups):
    code = {
        "service": service,
        "kind": kind,
        "code": groups[-1],
        "amount": None,
        "rut": None,
        "date": message["date"],
    }
    if kind in ("Payment", "Transfer") and len(groups) > 1:
        code["amount"] = int(groups[0].replace(".", ""))
    if kind == "Transfer" and len(groups) > 2:
        code["rut"] = groups[1].lstrip("0")
    return code


def extract(messages):
    codes = [code for code in map(parse, messages) if code]
    return sorted(codes, key=lambda code: code["date"], reverse=True)
