import json

from client import CONFIG_PATH  # type: ignore

STATE_PATH = CONFIG_PATH.parent / "state.json"


class ReadState:
    def __init__(self):
        try:
            self.read_until = json.loads(STATE_PATH.read_text())["read_until"]
        except (OSError, ValueError, KeyError):
            self.read_until = {}

    def is_unread(self, message):
        if message["sent"] or message["read"]:
            return False
        limit = self.read_until.get(str(message["thread"]), 0)
        return message["date"] > max(limit, self.read_until.get("all", 0))

    def mark_thread(self, thread, date):
        self.read_until[str(thread)] = date
        self.save()

    def mark_all(self, date):
        self.read_until = {"all": date}
        self.save()

    def save(self):
        STATE_PATH.parent.mkdir(parents=True, exist_ok=True)
        STATE_PATH.write_text(json.dumps({"read_until": self.read_until}))
