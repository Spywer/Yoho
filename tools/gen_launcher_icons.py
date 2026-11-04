from PIL import Image, ImageDraw, ImageFont
import os

res = r"d:\AndroidStudioProjects\Yoho\app\src\main\res"
sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

BG = (26, 32, 38, 255)
WHITE = (255, 255, 255, 255)


def load_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    for path in (
        r"C:\Windows\Fonts\arialbd.ttf",
        r"C:\Windows\Fonts\segoeuib.ttf",
        r"C:\Windows\Fonts\arial.ttf",
    ):
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def draw_yh(canvas: Image.Image, font_size: int, fill=WHITE) -> None:
    draw = ImageDraw.Draw(canvas)
    font = load_font(font_size)
    text = "YH"
    bbox = draw.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    x = (canvas.width - tw) / 2 - bbox[0]
    y = (canvas.height - th) / 2 - bbox[1] - canvas.height * 0.02
    draw.text((x, y), text, fill=fill, font=font)


# Full icons for pre-26 / legacy mipmaps
base = 512
full = Image.new("RGBA", (base, base), BG)
draw_yh(full, 180)

for folder, size in sizes.items():
    out_dir = os.path.join(res, folder)
    os.makedirs(out_dir, exist_ok=True)
    resized = full.resize((size, size), Image.Resampling.LANCZOS)
    for name in ("ic_launcher.webp", "ic_launcher_round.webp"):
        resized.save(os.path.join(out_dir, name), "WEBP", quality=95)

# Adaptive foreground: 108dp @ xxxhdpi = 432px, transparent + YH in safe zone
fg_size = 432
fg = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
draw_yh(fg, 138)
fg_path = os.path.join(res, "drawable-xxxhdpi")
os.makedirs(fg_path, exist_ok=True)
fg.save(os.path.join(fg_path, "ic_launcher_foreground.png"), "PNG")

# Lower densities for foreground
for dens, size, font_sz in (
    ("drawable-mdpi", 108, 34),
    ("drawable-hdpi", 162, 52),
    ("drawable-xhdpi", 216, 69),
    ("drawable-xxhdpi", 324, 104),
):
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw_yh(layer, font_sz)
    out = os.path.join(res, dens)
    os.makedirs(out, exist_ok=True)
    layer.save(os.path.join(out, "ic_launcher_foreground.png"), "PNG")

preview = os.path.join(os.path.dirname(__file__), "launcher_preview.png")
full.save(preview, "PNG")
print("ok", preview)
