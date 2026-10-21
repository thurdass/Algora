"""Check the installed package's window identity against its desktop launcher.

Run under a display: xvfb-run -a python3 desktop/packaging/tests/verify-linux-desktop.py
Requires dpkg-deb and X11 utilities (xwininfo, xprop).
"""

import pathlib
import re
import subprocess
import tempfile
import time

root = pathlib.Path(__file__).resolve().parents[3]
installer = root / "desktop/build/jpackage/installer/algora_0.2.0_amd64.deb"
with tempfile.TemporaryDirectory(prefix="algora-desktop-") as directory:
    unpacked = pathlib.Path(directory)
    subprocess.run(["dpkg-deb", "-x", str(installer), directory], check=True)
    app = unpacked / "opt/algora"
    desktop = next((app / "lib").glob("*.desktop"))
    entry = dict(line.split("=", 1) for line in desktop.read_text().splitlines() if "=" in line)
    icon = unpacked / entry["Icon"].lstrip("/")
    expected_icon = root / "desktop/src/main/resources/io/algora/desktop/icons/algora.png"
    assert icon.read_bytes() == expected_icon.read_bytes(), "Packaged icon differs from official logo"
    with tempfile.TemporaryFile() as log:
        process = subprocess.Popen([str(app / "bin/Algora")], stdout=log, stderr=log)
        try:
            deadline = time.monotonic() + 20
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    log.seek(0)
                    raise AssertionError("Installed application exited: " + log.read().decode())
                tree = subprocess.check_output(["xwininfo", "-root", "-tree"], text=True)
                match = re.search(r'(0x[0-9a-fA-F]+) "Algora —', tree)
                if match:
                    window = match.group(1)
                    properties = subprocess.check_output(
                        ["xprop", "-id", window, "WM_CLASS", "_NET_WM_ICON"], text=True
                    )
                    classes = re.findall(r'"([^"]+)"', properties.splitlines()[0])
                    assert entry["StartupWMClass"] in classes, properties.splitlines()[0]
                    assert "Icon (" in properties, "The native window has no icon"
                    print("ALGORA_DESKTOP_IDENTITY_OK: " + entry["StartupWMClass"])
                    break
                time.sleep(0.25)
            else:
                raise AssertionError("Installed window did not appear")
        finally:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
