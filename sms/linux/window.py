import queue
import shutil
import subprocess
import threading
import time
import tkinter as tk
from datetime import datetime
from tkinter import ttk

import theme  # type: ignore
from state import ReadState  # type: ignore

POLL_SECONDS = 3


class SmsWindow:
    def __init__(self, root, client):
        self.root = root
        self.client = client
        self.messages = {}
        self.threads = []
        self.selected = ""
        self.hovered = ""
        self.read_state = ReadState()
        self.loaded = False
        self.events = queue.Queue()
        self.build()
        threading.Thread(target=self.poll_loop, daemon=True).start()
        self.root.after(200, self.drain_events)

    def build(self):
        self.root.title("SMS")
        self.root.geometry("1000x680")
        theme.apply(self.root)
        self.status = ttk.Label(self.root, text="Connecting...", style="Status.TLabel")
        self.status.pack(side=tk.BOTTOM, fill=tk.X)
        sidebar = self.build_sidebar(self.root)
        sidebar.pack(side=tk.LEFT, fill=tk.Y)
        sidebar.pack_propagate(False)
        self.build_chat(self.root).pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

    def build_sidebar(self, parent):
        frame = ttk.Frame(parent, style="Panel.TFrame", width=340)
        self.build_toolbar(frame)
        self.conversations = ttk.Treeview(
            frame, columns=("when",), show="tree", selectmode=tk.BROWSE, cursor="hand2"
        )
        self.conversations.column("#0", width=190, minwidth=120)
        self.conversations.column(
            "when", width=86, minwidth=86, stretch=False, anchor=tk.E
        )
        self.conversations.tag_configure("unread", font=theme.BOLD)
        self.conversations.tag_configure("hover", background=theme.HOVER)
        self.add_scrollbar(frame, self.conversations)
        self.conversations.bind("<<TreeviewSelect>>", self.on_select)
        self.conversations.bind("<Motion>", self.on_hover)
        self.conversations.bind("<Leave>", self.clear_hover)
        return frame

    def build_toolbar(self, frame):
        toolbar = ttk.Frame(frame, style="Panel.TFrame", padding=(8, 8, 8, 4))
        toolbar.pack(side=tk.TOP, fill=tk.X)
        self.unread_only = tk.BooleanVar(value=False)
        ttk.Checkbutton(
            toolbar,
            text="Unread only",
            variable=self.unread_only,
            command=self.refresh_threads,
            cursor="hand2",
        ).pack(side=tk.LEFT)
        ttk.Button(
            toolbar, text="Mark all read", command=self.mark_all_read, cursor="hand2"
        ).pack(side=tk.RIGHT)
        self.unread_total = ttk.Label(
            frame, style="Muted.TLabel", padding=(12, 0, 8, 6)
        )
        self.unread_total.pack(side=tk.TOP, fill=tk.X)

    def build_chat(self, parent):
        frame = ttk.Frame(parent)
        self.title = ttk.Label(frame, style="Title.TLabel", padding=(16, 12, 16, 4))
        self.title.pack(fill=tk.X)
        self.chat = tk.Text(frame, wrap=tk.WORD, state=tk.DISABLED)
        theme.configure_chat(self.chat)
        self.add_scrollbar(frame, self.chat)
        return frame

    def add_scrollbar(self, frame, widget):
        bar = ttk.Scrollbar(frame, orient=tk.VERTICAL, command=widget.yview)
        widget.configure(yscrollcommand=bar.set)
        bar.pack(side=tk.RIGHT, fill=tk.Y)
        widget.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

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
        unread = {}
        for message in self.messages.values():
            thread = str(message["thread"])
            current = latest.get(thread)
            if current is None or message["date"] > current["date"]:
                latest[thread] = message
            if self.read_state.is_unread(message):
                unread[thread] = unread.get(thread, 0) + 1
        if self.selected in latest:
            self.read_state.mark_thread(self.selected, latest[self.selected]["date"])
            unread.pop(self.selected, None)
        self.render_threads(latest, unread)

    def render_threads(self, latest, unread):
        ordered = sorted(latest.items(), key=lambda item: item[1]["date"], reverse=True)
        self.threads = [thread for thread, _ in ordered]
        top = self.conversations.yview()[0]
        self.conversations.delete(*self.conversations.get_children())
        self.hovered = ""
        for thread, message in ordered:
            count = unread.get(thread, 0)
            if self.unread_only.get() and not count and thread != self.selected:
                continue
            label = "  " + display_name(message)
            if count:
                label += f"  ({count})"
            self.conversations.insert(
                "",
                tk.END,
                iid=thread,
                text=label,
                values=(short_date(message["date"]),),
                tags=("unread",) if count else (),
            )
        self.unread_total.config(text=f"{sum(unread.values())} unread")
        if self.conversations.exists(self.selected):
            self.conversations.selection_set(self.selected)
            self.show_thread()
        self.conversations.yview_moveto(top)

    def mark_all_read(self):
        newest = max((m["date"] for m in self.messages.values()), default=0)
        self.read_state.mark_all(newest)
        self.refresh_threads()

    def on_hover(self, event):
        row = self.conversations.identify_row(event.y)
        if row == self.hovered:
            return
        self.clear_hover()
        if row:
            tags = self.conversations.item(row, "tags")
            self.conversations.item(row, tags=(*tags, "hover"))
        self.hovered = row

    def clear_hover(self, _=None):
        if self.hovered and self.conversations.exists(self.hovered):
            tags = self.conversations.item(self.hovered, "tags")
            kept = tuple(tag for tag in tags if tag != "hover")
            self.conversations.item(self.hovered, tags=kept)
        self.hovered = ""

    def on_select(self, _):
        selection = self.conversations.selection()
        if selection and selection[0] != self.selected:
            self.selected = selection[0]
            self.refresh_threads()

    def thread_messages(self):
        items = [m for m in self.messages.values() if str(m["thread"]) == self.selected]
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
            self.chat.insert(tk.END, message["body"] + "\n", (tag, "first"))
            self.chat.insert(
                tk.END, stamp.strftime("%d %b %Y  %H:%M") + "\n", (tag, "time")
            )
            self.chat.insert(tk.END, "\n", "gap")
        self.chat.config(state=tk.DISABLED)
        self.chat.see(tk.END)


def short_date(milliseconds):
    stamp = datetime.fromtimestamp(milliseconds / 1000)
    if stamp.date() == datetime.now().date():
        return stamp.strftime("%H:%M")
    return stamp.strftime("%d/%m/%y")


def display_name(message):
    return message["name"] or message["address"] or "Unknown"


def notify(title, body):
    if shutil.which("notify-send"):
        subprocess.Popen(
            ["notify-send", "-a", "SMS", "-i", "mail-message-new", title, body]
        )
