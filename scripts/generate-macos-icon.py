#!/usr/bin/env python3
from __future__ import annotations

import argparse
import shutil
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


ICON_SIZES = [16, 32, 64, 128, 256, 512, 1024]


def mix(
    c0: tuple[int, int, int, int], c1: tuple[int, int, int, int], t: float
) -> tuple[int, int, int, int]:
    return (
        int(c0[0] + (c1[0] - c0[0]) * t),
        int(c0[1] + (c1[1] - c0[1]) * t),
        int(c0[2] + (c1[2] - c0[2]) * t),
        int(c0[3] + (c1[3] - c0[3]) * t),
    )


def gradient_image(
    width: int,
    height: int,
    c0: tuple[int, int, int, int],
    c1: tuple[int, int, int, int],
    diagonal: bool,
) -> Image.Image:
    grad = Image.new("RGBA", (width, height), c0)
    draw = ImageDraw.Draw(grad)
    if diagonal:
        span = max(1, width + height - 2)
        for y in range(height):
            for x in range(width):
                t = (x + y) / span
                draw.point((x, y), fill=mix(c0, c1, t))
        return grad
    span = max(1, height - 1)
    for y in range(height):
        t = y / span
        draw.line((0, y, width, y), fill=mix(c0, c1, t))
    return grad


def paste_round_gradient(
    base: Image.Image,
    x0: int,
    y0: int,
    x1: int,
    y1: int,
    radius: int,
    c0: tuple[int, int, int, int],
    c1: tuple[int, int, int, int],
    diagonal: bool,
) -> None:
    width = x1 - x0
    height = y1 - y0
    grad = gradient_image(width, height, c0, c1, diagonal)
    mask = Image.new("L", (width, height), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, width - 1, height - 1), radius=radius, fill=255
    )
    base.paste(grad, (x0, y0), mask)


def draw_icon(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    outer = int(round(size * 0.08))
    x0 = outer
    y0 = outer
    x1 = size - outer
    y1 = size - outer
    outer_radius = int(round(size * 0.21))

    shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow)
    shift = int(round(size * 0.018))
    sdraw.rounded_rectangle(
        (x0, y0 + shift, x1, y1 + shift),
        radius=outer_radius,
        fill=(6, 16, 34, 110),
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=max(1, size // 46)))
    img.alpha_composite(shadow)

    paste_round_gradient(
        img,
        x0,
        y0,
        x1,
        y1,
        outer_radius,
        (245, 249, 255, 255),
        (208, 217, 231, 255),
        False,
    )

    stroke_w = max(1, int(round(size * 0.01)))
    draw.rounded_rectangle(
        (x0, y0, x1, y1),
        radius=outer_radius,
        outline=(255, 255, 255, 220),
        width=stroke_w,
    )

    inner_pad = int(round(size * 0.185))
    ix0 = inner_pad
    iy0 = inner_pad
    ix1 = size - inner_pad
    iy1 = size - inner_pad
    inner_radius = int(round(size * 0.145))

    paste_round_gradient(
        img,
        ix0,
        iy0,
        ix1,
        iy1,
        inner_radius,
        (10, 24, 45, 255),
        (6, 53, 66, 255),
        True,
    )

    ring_w = max(2, int(round(size * 0.015)))
    draw.rounded_rectangle(
        (ix0, iy0, ix1, iy1),
        radius=inner_radius,
        outline=(82, 239, 205, 235),
        width=ring_w,
    )

    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse(
        (
            ix0 - int(round(size * 0.06)),
            iy0 - int(round(size * 0.05)),
            ix1 + int(round(size * 0.05)),
            iy1 + int(round(size * 0.08)),
        ),
        fill=(46, 214, 186, 70),
    )
    glow = glow.filter(ImageFilter.GaussianBlur(radius=max(1, size // 34)))
    img.alpha_composite(glow)

    width = ix1 - ix0
    height = iy1 - iy0
    waveform = [
        (0.12, 0.60),
        (0.24, 0.48),
        (0.34, 0.66),
        (0.47, 0.32),
        (0.60, 0.60),
        (0.74, 0.42),
        (0.88, 0.56),
    ]
    points = [
        (ix0 + int(round(px * width)), iy0 + int(round(py * height)))
        for (px, py) in waveform
    ]

    line_w = max(2, int(round(size * 0.046)))
    draw.line(points, fill=(120, 251, 232, 238), width=line_w, joint="curve")
    draw.line(
        points, fill=(230, 255, 252, 165), width=max(1, line_w // 2), joint="curve"
    )

    pulse_r = max(2, int(round(size * 0.034)))
    pulse_points = [points[1], points[3], points[5]]
    pulse_colors = [(64, 214, 255, 250), (46, 231, 170, 250), (184, 248, 98, 250)]
    for idx, center in enumerate(pulse_points):
        cx, cy = center
        draw.ellipse(
            (cx - pulse_r, cy - pulse_r, cx + pulse_r, cy + pulse_r),
            fill=pulse_colors[idx],
        )
        shine_r = max(1, pulse_r // 3)
        draw.ellipse(
            (
                cx - pulse_r + shine_r,
                cy - pulse_r + shine_r,
                cx - pulse_r + shine_r * 3,
                cy - pulse_r + shine_r * 3,
            ),
            fill=(255, 255, 255, 155),
        )

    return img


def ensure_iconutil() -> None:
    if shutil.which("iconutil") is None:
        raise SystemExit("iconutil not found. This script must run on macOS.")


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate .icns icon")
    parser.add_argument("--output", required=True, help="Output .icns path")
    parser.add_argument(
        "--workdir", required=True, help="Temporary iconset parent directory"
    )
    args = parser.parse_args()

    ensure_iconutil()

    out_path = Path(args.output).resolve()
    workdir = Path(args.workdir).resolve()
    iconset_dir = workdir / "app.iconset"
    if iconset_dir.exists():
        shutil.rmtree(iconset_dir)
    iconset_dir.mkdir(parents=True, exist_ok=True)

    for size in ICON_SIZES:
        img = draw_icon(size)
        img.save(iconset_dir / f"icon_{size}x{size}.png")
        if size <= 512:
            img2 = draw_icon(size * 2)
            img2.save(iconset_dir / f"icon_{size}x{size}@2x.png")

    out_path.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["iconutil", "-c", "icns", str(iconset_dir), "-o", str(out_path)], check=True
    )


if __name__ == "__main__":
    main()
