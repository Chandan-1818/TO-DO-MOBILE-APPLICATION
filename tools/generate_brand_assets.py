"""Generates all Master branding assets from the supplied samurai artwork.
Usage: python tools/generate_brand_assets.py <source.jpg> <android_res_dir> <docs_branding_dir>
Requires Pillow + numpy. Deterministic; safe to re-run.
"""
import sys, os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

src, res, docs = sys.argv[1:4]
img = Image.open(src).convert("RGB")
arr = np.asarray(img).astype(np.float32) / 255.0
lum = 0.299 * arr[..., 0] + 0.587 * arr[..., 1] + 0.114 * arr[..., 2]

# --- parchment colour: median of the lightest 40% of pixels (the paper) ---
flat = arr.reshape(-1, 3)
l_flat = lum.reshape(-1)
paper = flat[l_flat >= np.quantile(l_flat, 0.60)]
parchment = tuple(int(round(v * 255)) for v in np.median(paper, axis=0))
PARCH_HEX = "#%02X%02X%02X" % parchment
print("parchment", PARCH_HEX)

# --- ink layer: darkness -> alpha, ink colour is near-black warm ---
BG_L, INK_L = float(np.quantile(l_flat, 0.60)), 0.10
alpha = np.clip((BG_L - lum) / (BG_L - INK_L), 0, 1) ** 0.9
ink_rgb = np.array([0x14, 0x11, 0x0F], dtype=np.uint8)
rgba = np.zeros((*lum.shape, 4), dtype=np.uint8)
rgba[..., :3] = ink_rgb
rgba[..., 3] = (alpha * 255).astype(np.uint8)
ink = Image.fromarray(rgba, "RGBA")

def crop_square(im, cx, cy, side, out):
    """Crop a square window (may extend past the image: padded transparent/parchment)."""
    half = side / 2
    box = (int(cx - half), int(cy - half), int(cx + half), int(cy + half))
    mode = im.mode
    fill = (0, 0, 0, 0) if mode == "RGBA" else parchment
    canvas = Image.new(mode, (box[2]-box[0], box[3]-box[1]), fill)
    canvas.paste(im, (-box[0], -box[1]))
    return canvas.resize((out, out), Image.LANCZOS)

# Subject (hat + face + sword) spans ~x180..610; centre (395,285); 680px window
# puts the subject at ~63% of the canvas, inside the adaptive-icon 66dp safe zone.
CX, CY, SIDE = 395, 285, 680

def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path, optimize=True)

dens = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
for d, px in dens.items():
    fg = crop_square(ink, CX, CY, SIDE, px)
    save(fg, f"{res}/mipmap-{d}/ic_launcher_foreground.png")
    mono_a = np.asarray(fg)[..., 3]
    mono = np.zeros((px, px, 4), dtype=np.uint8); mono[..., 3] = mono_a
    save(Image.fromarray(mono, "RGBA"), f"{res}/mipmap-{d}/ic_launcher_monochrome.png")

# Splash icon (system splash: 288dp canvas, drawn in a 192dp circle)
save(crop_square(ink, CX, CY, SIDE, 768), f"{res}/drawable-nodpi/splash_icon.png")

# Original artwork, unmodified pixels (welcome/onboarding/splash hero)
img.save(f"{res}/drawable-nodpi/samurai_artwork.jpg", quality=92, optimize=True)

# Default profile avatar: face/hat/hand region, square, original pixels
crop_square(img, 385, 280, 560, 512).save(f"{res}/drawable-nodpi/default_avatar.jpg", quality=92, optimize=True)

# Play Store / README icon (flattened) + mask preview sheet
bg = Image.new("RGBA", (432, 432), parchment + (255,))
flat_icon = Image.alpha_composite(bg, crop_square(ink, CX, CY, SIDE, 432))
os.makedirs(docs, exist_ok=True)
flat_icon.resize((512, 512), Image.LANCZOS).convert("RGB").save(f"{docs}/play_store_icon_512.png", optimize=True)

def mask(shape, size):
    m = Image.new("L", (size, size), 0); d = ImageDraw.Draw(m)
    # visible area of the adaptive icon is the 72dp centre of the 108dp canvas
    inset = int(size * 18 / 108); box = (inset, inset, size - inset, size - inset)
    if shape == "circle": d.ellipse(box, fill=255)
    elif shape == "squircle": d.rounded_rectangle(box, radius=int((size-2*inset)*0.36), fill=255)
    else: d.rounded_rectangle(box, radius=int((size-2*inset)*0.18), fill=255)
    return m

S = 216
fg216 = crop_square(ink, CX, CY, SIDE, S)
bgs = [("light", (236, 236, 232)), ("dark", (24, 24, 24)), ("warm", (170, 90, 50))]
sheet = Image.new("RGB", (S * 3, S * 3), (0, 0, 0))
for r, (_, wp) in enumerate(bgs):
    for c, shape in enumerate(["circle", "squircle", "rounded"]):
        cell = Image.new("RGBA", (S, S), wp + (255,))
        icon = Image.alpha_composite(Image.new("RGBA", (S, S), parchment + (255,)), fg216)
        cell.paste(icon, (0, 0), mask(shape, S))
        sheet.paste(cell.convert("RGB"), (c * S, r * S))
sheet.save(f"{docs}/icon_mask_preview.png")
# small-size legibility check: 48px and 36px
small = Image.new("RGB", (48 + 36 + 24 + 40, 60), (24, 24, 24))
x = 10
for px in (48, 36, 24):
    ic = flat_icon.resize((px, px), Image.LANCZOS).convert("RGB")
    m = mask("circle", px).resize((px, px)); 
    small.paste(ic, (x, 6))
    x += px + 8
small = small.resize((small.width * 4, small.height * 4), Image.NEAREST)
small.save(f"{docs}/icon_small_sizes_preview.png")
open("/tmp/parchment.txt", "w").write(PARCH_HEX)
