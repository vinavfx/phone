import argparse
import os
import sys
from datetime import datetime

from client import PhoneClient, load_config  # type: ignore
from codes import extract  # type: ignore

COLOR = sys.stdout.isatty() and "NO_COLOR" not in os.environ
BOLD = "1"
DIM = "2"
GREEN = "1;32"
YELLOW = "1;33"
CYAN = "1;36"
MAGENTA = "35"
RED = "1;31"


def paint(text, style):
    return f"\033[{style}m{text}\033[0m" if COLOR else text


def money(amount):
    return "$" + f"{amount:,}".replace(",", ".")


def ago(milliseconds):
    seconds = int(datetime.now().timestamp() - milliseconds / 1000)
    for unit, size in (("d", 86400), ("h", 3600), ("min", 60)):
        if seconds >= size:
            return f"{seconds // size} {unit} ago"
    return "just now"


def stamp(milliseconds):
    return datetime.fromtimestamp(milliseconds / 1000).strftime("%d/%m/%Y %H:%M")


def show_latest(code):
    print(paint(code["service"], CYAN), paint(code["kind"], MAGENTA))
    if code["amount"] is not None:
        print("Amount ", paint(money(code["amount"]), YELLOW))
    if code["rut"]:
        print("RUT    ", paint(code["rut"], BOLD))
    print("Code   ", paint(code["code"], GREEN))
    print(paint(f"{stamp(code['date'])} ({ago(code['date'])})", DIM))


def show_rows(codes):
    for code in codes:
        amount = money(code["amount"]) if code["amount"] is not None else ""
        print(
            paint(f"{stamp(code['date']):<17}", DIM),
            paint(f"{code['service']:<16}", CYAN),
            paint(f"{code['kind']:<21}", MAGENTA),
            paint(f"{code['code']:<9}", GREEN),
            paint(f"{amount:>12}", YELLOW),
        )


def latest_per_type(codes):
    found = {}
    for code in codes:
        found.setdefault((code["service"], code["kind"]), code)
    return list(found.values())


def run_status(client, root):
    client.call("/ping")
    print(paint("●", GREEN), "Phone connected at", paint(client.host, BOLD))
    print()
    root.print_help()
    return 0


def run_code(client, args):
    codes = extract(client.messages(0, limit=100000))
    if args.service:
        wanted = args.service.lower()
        codes = [code for code in codes if wanted in code["service"].lower()]
    if not codes:
        print("No verification codes found.", file=sys.stderr)
        return 1
    show_latest(codes[0])
    print()
    show_rows(codes if args.all else latest_per_type(codes))
    return 0


def parser():
    root = argparse.ArgumentParser(prog="ph", description="Phone command line tools.")
    commands = root.add_subparsers(dest="command", title="commands", metavar="COMMAND")
    code = commands.add_parser(
        "code",
        help="Latest verification code and the latest of each type (-a all).",
        description="Show verification codes received by SMS.",
    )
    code.add_argument("service", nargs="?", help="Filter by service, e.g. whatsapp.")
    code.add_argument("-a", "--all", action="store_true", help="List every code.")
    code.set_defaults(handler=run_code)
    return root


def main():
    root = parser()
    args = root.parse_args()
    try:
        client = PhoneClient(load_config())
    except (OSError, ValueError):
        print(paint("Phone not paired. Run sms/install.sh.", RED), file=sys.stderr)
        return 1
    try:
        if args.command is None:
            return run_status(client, root)
        return args.handler(client, args)
    except OSError as error:
        reason = getattr(error, "reason", error)
        message = f"Phone not reachable at {client.host}: {reason}"
        print(paint(message, RED), file=sys.stderr)
        print("Check that it is on and on the same Wi-Fi.", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
