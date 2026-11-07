from tkinter import ttk

BACKGROUND = "#eef1f4"
PANEL = "#ffffff"
CARD_IN = "#ffffff"
CARD_OUT = "#dcefe6"
BUTTON = "#e8ecf0"
TEXT = "#1f2933"
MUTED = "#6b7684"
ACCENT = "#568c78"
HOVER = "#eef3f1"
SELECTION = "#bfe0d2"
FONT = ("Noto Sans", 11)
BOLD = ("Noto Sans", 11, "bold")


def apply(root):
    root.configure(background=BACKGROUND)
    style = ttk.Style(root)
    style.theme_use("clam")
    style.configure(".", background=BACKGROUND, foreground=TEXT, font=FONT)
    style.configure("TFrame", background=BACKGROUND)
    style.configure("Panel.TFrame", background=PANEL)
    style.configure("TLabel", background=BACKGROUND, foreground=TEXT)
    style.configure("Title.TLabel", font=("Noto Sans", 14, "bold"))
    style.configure(
        "Status.TLabel", background=PANEL, foreground=MUTED, padding=(10, 4)
    )
    style.configure("Muted.TLabel", background=PANEL, foreground=MUTED)
    style.configure(
        "TButton",
        background=BUTTON,
        foreground=TEXT,
        bordercolor=BUTTON,
        lightcolor=BUTTON,
        darkcolor=BUTTON,
        padding=(10, 4),
    )
    style.map(
        "TButton",
        background=[("active", ACCENT)],
        foreground=[("active", "#ffffff")],
    )
    style.configure(
        "TCheckbutton",
        background=PANEL,
        foreground=TEXT,
        indicatorbackground=BUTTON,
        indicatorforeground="#ffffff",
    )
    style.map(
        "TCheckbutton",
        background=[("active", PANEL)],
        indicatorbackground=[("selected", ACCENT)],
    )
    style.configure(
        "Treeview",
        background=PANEL,
        fieldbackground=PANEL,
        foreground=TEXT,
        borderwidth=0,
        rowheight=40,
    )
    style.map(
        "Treeview",
        background=[("selected", SELECTION)],
        foreground=[("selected", TEXT)],
    )
    style.layout("Treeview", [("Treeview.treearea", {"sticky": "nswe"})])
    style.configure(
        "Vertical.TScrollbar",
        background="#cfd6dd",
        troughcolor=BACKGROUND,
        bordercolor=BACKGROUND,
        arrowcolor=MUTED,
        gripcount=0,
        lightcolor="#cfd6dd",
        darkcolor="#cfd6dd",
    )


def configure_chat(chat):
    chat.configure(
        background=BACKGROUND,
        foreground=TEXT,
        font=FONT,
        borderwidth=0,
        highlightthickness=0,
        padx=16,
        pady=12,
        cursor="xterm",
        selectbackground=SELECTION,
        selectforeground=TEXT,
        inactiveselectbackground=SELECTION,
    )
    for tag, color, right in (("in", CARD_IN, 160), ("out", CARD_OUT, 16)):
        left = 16 if tag == "in" else 160
        chat.tag_configure(
            tag,
            background=color,
            lmargin1=left,
            lmargin2=left,
            rmargin=right,
            spacing1=2,
        )
    chat.tag_configure("first", spacing1=10)
    chat.tag_configure("time", foreground=MUTED, font=("Noto Sans", 8), spacing3=10)
    chat.tag_configure("gap", font=("Noto Sans", 4))
    chat.tag_raise("sel")
