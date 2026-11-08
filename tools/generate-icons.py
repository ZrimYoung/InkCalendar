#!/usr/bin/env python3
"""Export the approved calendar mark for Android, Web and the offline prototype.

Requires Pillow. The transparent source is shared/branding/calendar-mark.png.
"""

import base64
from pathlib import Path
import re

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "shared/branding/calendar-mark.png"
ANDROID = ROOT / "app/src/main/res"
PUBLIC = ROOT / "web/public"


def render(mark, size, fraction, background=(255, 255, 255, 255)):
    edge = round(size * fraction)
    scale = edge / max(mark.size)
    shape = mark.resize(
        (round(mark.width * scale), round(mark.height * scale)),
        Image.Resampling.LANCZOS,
    )
    canvas = Image.new("RGBA", (size, size), background)
    canvas.alpha_composite(shape, ((size - shape.width) // 2, (size - shape.height) // 2))
    return canvas


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.info.clear()
    image.save(path, optimize=True)


def main():
    mark = Image.open(SOURCE).convert("RGBA")
    for density, size in (
        ("ldpi", 36), ("mdpi", 48), ("hdpi", 72),
        ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192),
    ):
        save(render(mark, size, 2 / 3), ANDROID / ("mipmap-" + density) / "ic_launcher.png")
    # The transparent mark fits the circular safe area of adaptive/themed icons.
    save(
        render(mark, 432, 46 / 108, (0, 0, 0, 0)),
        ANDROID / "drawable-xxxhdpi-v26/ic_launcher_foreground.png",
    )
    for size in (192, 512):
        save(render(mark, size, 2 / 3), PUBLIC / ("icon-" + str(size) + ".png"))
    save(render(mark, 512, 0.54), PUBLIC / "icon-maskable-512.png")
    save(render(mark, 32, 0.75), PUBLIC / "favicon.png")

    # Keep the directly opened HTML prototype self-contained.
    preview = ROOT / "preview/ink-calendar-preview.html"
    content = preview.read_text(encoding="utf-8")
    encoded = base64.b64encode((PUBLIC / "favicon.png").read_bytes()).decode("ascii")
    link = '<link rel="icon" type="image/png" href="data:image/png;base64,' + encoded + '">'
    if re.search(r'<link\s+rel="icon"[^>]*>', content):
        content = re.sub(r'<link\s+rel="icon"[^>]*>', link, content)
    else:
        content = content.replace("</title>", "</title>\n" + link, 1)
    preview.write_text(content, encoding="utf-8")
    print("Exported shared calendar icons for Android, Web and the offline prototype.")


if __name__ == "__main__":
    main()
