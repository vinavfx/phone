import tkinter as tk
from tkinter import messagebox

from client import CONFIG_PATH, PhoneClient, load_config  # type: ignore
from window import SmsWindow  # type: ignore


def main():
    root = tk.Tk(className="sms")
    try:
        config = load_config()
    except (OSError, ValueError):
        root.withdraw()
        messagebox.showerror(
            "SMS",
            f"Not paired. Run sms/install.sh with the phone on USB.\n{CONFIG_PATH}",
        )
        return
    SmsWindow(root, PhoneClient(config))
    root.mainloop()


if __name__ == "__main__":
    main()
