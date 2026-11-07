import ipaddress
import json
import socket
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

CONFIG_PATH = Path.home() / ".config" / "sms" / "config.json"
PORT = 8765


def load_config():
    return json.loads(CONFIG_PATH.read_text())


def save_config(config):
    CONFIG_PATH.parent.mkdir(parents=True, exist_ok=True)
    CONFIG_PATH.write_text(json.dumps(config, indent=4))
    CONFIG_PATH.chmod(0o600)


def local_address():
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as probe:
        probe.connect(("192.0.2.1", 9))
        return probe.getsockname()[0]


class PhoneClient:
    def __init__(self, config):
        self.config = config

    @property
    def host(self):
        return self.config["host"]

    def messages(self, since):
        path = f"/messages?since={since}&limit=5000"
        return self.call(path)["messages"]

    def call(self, path):
        try:
            return self.request(self.host, path)
        except urllib.error.HTTPError:
            raise
        except OSError:
            host = self.discover()
            if host is None:
                raise
            self.config["host"] = host
            save_config(self.config)
            return self.request(host, path)

    def request(self, host, path, timeout=15):
        headers = {"Authorization": f"Bearer {self.config['token']}"}
        url = f"http://{host}:{PORT}{path}"
        request = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.loads(response.read())

    def discover(self):
        network = ipaddress.ip_network(f"{local_address()}/24", strict=False)
        hosts = [str(host) for host in network.hosts()]
        with ThreadPoolExecutor(64) as pool:
            for host, found in zip(hosts, pool.map(self.probe, hosts)):
                if found:
                    return host
        return None

    def probe(self, host):
        try:
            return self.request(host, "/ping", timeout=1)["ok"]
        except (OSError, ValueError, KeyError):
            return False
