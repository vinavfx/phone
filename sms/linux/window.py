import queue
import shutil
import subprocess
import threading
import time
import tkinter as tk
from datetime import datetime
from tkinter import ttk

POLL_SECONDS = 3


class SmsWindow:
    def __init__(self, root, client):
        self.root = root
        self.client = client
        self.messages = {}
        self.threads = []
        self.selected = None
        self.loaded = False
        self.events = queue.Queue()
        self.build()
        threading.Thread(target=self.poll_loop, daemon=True).start()
        self.root.after(200, self.drain_events)

    def build(self):
        self.root.title("SMS")
        self.root.geometry("960x640")
        panes = ttk.PanedWindow(self.root, orient=tk.HORIZONTAL)
        panes.pack(fill=tk.BOTH, expand=True)
        panes.add(self.build_sidebar(panes), weight=1)
        panes.add(self.build_chat(panes), weight=3)
        self.status = ttk.Label(self.root, text="Connecting...", anchor=tk.W)
        self.status.pack(fill=tk.X, padx=6, pady=2)

    def build_sidebar(self, parent):
        frame = ttk.Frame(parent)
        self.conversations = tk.Listbox(frame, activestyle=tk.NONE, font=("Sans", 11))
        self.conversations.pack(fill=tk.BOTH, expand=True, padx=4, pady=(0, 4))
        self.conversations.bind("<<ListboxSelect>>", self.on_select)
        return frame

    def build_chat(self, parent):
        frame = ttk.Frame(parent)
        self.title = ttk.Label(frame, font=("Sans", 13, "bold"))
        self.title.pack(fill=tk.X, padx=8, pady=6)
        self.chat = tk.Text(frame, wrap=tk.WORD, state=tk.DISABLED, font=("Sans", 11))
        self.chat.tag_configure("in", lmargin1=8, lmargin2=8, rmargin=120)
        self.chat.tag_configure(
            "out",
            justify=tk.RIGHT,
            lmargin1=120,
            lmargin2=120,
            rmargin=8,
            foreground="#1f5f4a",
        )
        self.chat.tag_configure("time", foreground="#888888", font=("Sans", 8))
        self.chat.pack(fill=tk.BOTH, expand=True, padx=8, pady=(0, 8))
        return frame

    def poll_loop(self):
        since = 0
        while True:
            try:
                batch = self.client.messages(since)
                if batch:
                    since = max(message["date"] for message in batch)
                self.events.put(("messages", batch))
            except Exception as error:
                self.events.put(("error", error))
            time.sleep(POLL_SECONDS)

    def drain_events(self):
        while not self.events.empty():
            kind, value = self.events.get()
            if kind == "messages":
                self.merge(value)
                self.status.config(text=f"Connected to {self.client.host}")
            else:
                self.status.config(text=f"Phone not reachable: {value}")
        self.root.after(200, self.drain_events)

    def merge(self, batch):
        fresh = [message for message in batch if message["id"] not in self.messages]
        for message in batch:
            self.messages[message["id"]] = message
        if self.loaded:
            for message in fresh:
                if not message["sent"]:
                    notify(display_name(message), message["body"])
        self.loaded = True
        if fresh:
            self.refresh_threads()

    def refresh_threads(self):
        latest = {}
        for message in self.messages.values():
            current = latest.get(message["thread"])
            if current is None or message["date"] > current["date"]:
                latest[message["thread"]] = message
        ordered = sorted(latest.values(), key=lambda item: item["date"], reverse=True)
        self.threads = [message["thread"] for message in ordered]
        self.conversations.delete(0, tk.END)
        for message in ordered:
            self.conversations.insert(tk.END, display_name(message))
        if self.selected in self.threads:
            index = self.threads.index(self.selected)
            self.conversations.selection_set(index)
            self.show_thread()

    def on_select(self, _):
        selection = self.conversations.curselection()
        if selection:
            self.selected = self.threads[selection[0]]
            self.show_thread()

    def thread_messages(self):
        items = [m for m in self.messages.values() if m["thread"] == self.selected]
        return sorted(items, key=lambda item: item["date"])

    def show_thread(self):
        items = self.thread_messages()
        if not items:
            return
        self.title.config(text=display_name(items[-1]))
        self.chat.config(state=tk.NORMAL)
        self.chat.delete("1.0", tk.END)
        for message in items:
            tag = "out" if message["sent"] else "in"
            stamp = datetime.fromtimestamp(message["date"] / 1000)
            self.chat.insert(tk.END, message["body"] + "\n", tag)
            self.chat.insert(
                tk.END, stamp.strftime("%Y-%m-%d %H:%M") + "\n\n", (tag, "time")
            )
        self.chat.config(state=tk.DISABLED)
        self.chat.see(tk.END)


def display_name(message):
    return message["name"] or message["address"] or "Unknown"


def notify(title, body):
    if shutil.which("notify-send"):
        subprocess.Popen(
            ["notify-send", "-a", "SMS", "-i", "mail-message-new", title, body]
        )
