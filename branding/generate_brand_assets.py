#!/usr/bin/env python3
"""
Génère TOUS les assets du logo JobLog à partir d'UNE géométrie (source de vérité : les constantes ci-dessous).

Logo : un carnet / journal minimaliste (couverture pleine, reliure suggérée par un trait vertical côté gauche) avec une
coche corail sur la couverture — écho direct au nom « JobLog ». Grille 108 × 108 (celle des icônes adaptatives Android) ;
le motif (36 × 46, centré en 54,54) tient dans la zone de sécurité : cercle de 66 au centre (sa diagonale fait 58,4).

Sorties (relancer le script après toute modification de la géométrie ou des couleurs) :
  branding/joblog-logo.svg, joblog-mark-on-light.svg          (masters vectoriels)
  androidApp/src/main/res/drawable*/ic_launcher_*.xml, ic_splash_logo.xml, ic_joblog_mark.xml  (VectorDrawable)
  androidApp/src/main/res/mipmap-*/ic_launcher*.png           (icônes héritées API 24-25 : PNG)
  iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/*.png      (1024 px, sans transparence : exigé par l'App Store)
Dépendance : Pillow (pip install pillow), uniquement pour les PNG.
⚠️ SYNCHRONISATION MANUELLE : le SwiftUI `JobLogMark` (iOS) redessine la même géométrie avec les mêmes constantes,
recopiées à la main dans `iosApp/iosApp/Core/Branding/JobLogMark.swift`. Toute modification des constantes ci-dessous
(BODY_*, SPINE_*, CHECK_*, largeurs de trait) doit être reportée dans ce fichier Swift, puis vérifiée à l'écran.
"""
import os, sys
from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
TEAL, CORAL, WHITE = "#0D6E68", "#E8734A", "#FFFFFF"
DARK_BG = "#0B2B29"   # fond de l'icône iOS en mode sombre

# --- géométrie (grille 108) ---
BODY = (36, 31, 72, 77); BODY_R = 5.0                                   # couverture du carnet (x0, y0, x1, y1), coins arrondis
BODY_D = "M41,31 H67 A5,5 0 0 1 72,36 V72 A5,5 0 0 1 67,77 H41 A5,5 0 0 1 36,72 V36 A5,5 0 0 1 41,31 Z"
SPINE_D = "M44.5,31 V77"                                                # trait de reliure (couleur du fond = « fente » dans la couverture)
SPINE_W = 2.5
CHECK_PTS = [(51, 54), (57, 60), (66, 47)]                           # coche sur la couverture (zone à droite de la reliure)
CHECK_D = "M51,54 L57,60 L66,47"
CHECK_W = 5.5
BBOX = BODY               # boîte englobante du motif (aucun trait ne dépasse de la couverture), pour la version « serrée »

def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w", encoding="utf-8").write(text)

# ---------------------------------------------------------------- SVG
def svg_mark(body, check, spine, extra_bg=None, viewbox="0 0 108 108", size=None, tx=0, ty=0):
    bg = f'  <rect width="108" height="108" fill="{extra_bg}"/>\n' if extra_bg else ""
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="{viewbox}"{f' width="{size}" height="{size}"' if size else ''}>
{bg}  <g transform="translate({tx} {ty})">
    <path d="{BODY_D}" fill="{body}"/>
    <path d="{SPINE_D}" fill="none" stroke="{spine}" stroke-width="{SPINE_W}"/>
    <path d="{CHECK_D}" fill="none" stroke="{check}" stroke-width="{CHECK_W}" stroke-linecap="round" stroke-linejoin="round"/>
  </g>
</svg>
'''
write(f"{ROOT}/branding/joblog-logo.svg", svg_mark(WHITE, CORAL, TEAL, extra_bg=TEAL))
write(f"{ROOT}/branding/joblog-mark-on-light.svg", svg_mark(TEAL, CORAL, WHITE))

# ---------------------------------------------------------------- Android VectorDrawables
def vd(width_dp, height_dp, vw, vh, body):
    return f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{width_dp}dp"
    android:height="{height_dp}dp"
    android:viewportWidth="{vw}"
    android:viewportHeight="{vh}">
{body}</vector>
'''
def vd_paths(body_c, check_c, spine_c, group=None, body_outline=False, indent="    "):
    out = []
    if body_outline:
        out.append(f'<path android:pathData="{BODY_D}" android:strokeColor="{body_c}" android:strokeWidth="{SPINE_W + 1.5}" android:strokeLineJoin="round" />')
    else:
        out.append(f'<path android:pathData="{BODY_D}" android:fillColor="{body_c}" />')
    out.append(f'<path android:pathData="{SPINE_D}" android:strokeColor="{spine_c}" android:strokeWidth="{SPINE_W}" />')
    out.append(f'<path android:pathData="{CHECK_D}" android:strokeColor="{check_c}" android:strokeWidth="{CHECK_W}" android:strokeLineCap="round" android:strokeLineJoin="round" />')
    text = "\n".join(f"{indent}{l}" for l in out) + "\n"
    if group:
        text = f'    <group android:translateX="{group[0]}" android:translateY="{group[1]}" android:scaleX="{group[2]}" android:scaleY="{group[2]}" android:pivotX="0" android:pivotY="0">\n' + \
               "\n".join(f"        {l}" for l in out) + "\n    </group>\n"
    return text

RES = f"{ROOT}/androidApp/src/main/res"
# Icône adaptative : fond teal uni + premier plan (blanc / corail) — 108 dp, le motif reste dans la zone de sécurité
write(f"{RES}/drawable/ic_launcher_background.xml", vd(108, 108, 108, 108, f'    <path android:fillColor="{TEAL}" android:pathData="M0,0h108v108h-108z" />\n'))
write(f"{RES}/drawable-v24/ic_launcher_foreground.xml", vd(108, 108, 108, 108, vd_paths(WHITE, CORAL, TEAL)))
# Icône thématique Android 13+ : monochrome, en traits (le système la teinte)
write(f"{RES}/drawable/ic_launcher_monochrome.xml", vd(108, 108, 108, 108, vd_paths("#FFFFFF", "#FFFFFF", "#FFFFFF", body_outline=True)))
for name in ("ic_launcher", "ic_launcher_round"):
    write(f"{RES}/mipmap-anydpi-v26/{name}.xml", '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
''')
# Splash Android 12+ : icône SANS fond, 288 dp, contenu visible dans un cercle de 192 dp → motif réduit (échelle 1.5 sur 108 → 162) recentré
S = 2.6   # 108 → 280 unités : le motif (36 × 46) devient ~94 × 120 (diagonale 152), dans le cercle de 192
write(f"{RES}/drawable/ic_splash_logo.xml", vd(288, 288, 288, 288,
      vd_paths(WHITE, CORAL, TEAL, group=(144 - 54 * S, 144 - 54 * S, S))))
# Pastille de marque dans l'app (écran À propos) : motif serré, fond fourni par l'appelant
w, h = BBOX[2] - BBOX[0] + 4, BBOX[3] - BBOX[1] + 4   # 2 unités de marge de chaque côté
write(f"{RES}/drawable/ic_joblog_mark.xml", vd(w, h, w, h, vd_paths(WHITE, CORAL, TEAL, group=(-(BBOX[0] - 2), -(BBOX[1] - 2), 1)).replace(f'android:scaleX="1" android:scaleY="1" ', "")))

# ---------------------------------------------------------------- PNG (Pillow) — supersampling 4×
def raster(size, bg=None, shape="square", body=WHITE, check=CORAL, spine=TEAL, mark_scale=1.0, content_box=108):
    ss = 4
    N = size * ss
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if bg:
        if shape == "circle": d.ellipse((0, 0, N - 1, N - 1), fill=bg)
        elif shape == "rounded": d.rounded_rectangle((0, 0, N - 1, N - 1), radius=int(N * 0.22), fill=bg)
        else: d.rectangle((0, 0, N, N), fill=bg)
    k = N / content_box * mark_scale
    off = N * (1 - mark_scale) / 2
    P = lambda x, y: (off + x * k, off + y * k)
    def thick_line(pts, w, color, caps=True):
        pp = [P(*p) for p in pts]
        d.line(pp, fill=color, width=int(w * k), joint="curve")
        if caps:
            r = w * k / 2
            for (x, y) in pp: d.ellipse((x - r, y - r, x + r, y + r), fill=color)
    d.rounded_rectangle((*P(BODY[0], BODY[1]), *P(BODY[2], BODY[3])), radius=BODY_R * k, fill=body)
    thick_line([(44.5, BODY[1]), (44.5, BODY[3])], SPINE_W, spine, caps=False)
    thick_line(CHECK_PTS, CHECK_W, check)
    return im.resize((size, size), Image.LANCZOS)

# Icônes héritées (API 24-25) : le fond fait partie de l'image ; motif à ~ 75 % de la zone (les coins arrondis rognent)
for d_, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    raster(px, bg=TEAL, shape="rounded", mark_scale=1.30).save(f"{RES}/mipmap-{d_}/ic_launcher.png")
    raster(px, bg=TEAL, shape="circle", mark_scale=1.25).save(f"{RES}/mipmap-{d_}/ic_launcher_round.png")

# iOS : 1024 px, SANS transparence (App Store), l'OS applique le masque arrondi
ICONSET = f"{ROOT}/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
def flat(im, bg):
    base = Image.new("RGB", im.size, bg); base.paste(im, mask=im.split()[3]); return base
flat(raster(1024, bg=TEAL, mark_scale=1.35), TEAL).save(f"{ICONSET}/AppIcon-1024.png")
flat(raster(1024, bg=DARK_BG, mark_scale=1.35, spine=DARK_BG), DARK_BG).save(f"{ICONSET}/AppIcon-Dark-1024.png")
# « tinted » : niveaux de gris sur fond noir (iOS le teinte)
flat(raster(1024, bg="#000000", mark_scale=1.35, body="#FFFFFF", check="#B0B0B0", spine="#000000"), "#000000").save(f"{ICONSET}/AppIcon-Tinted-1024.png")
write(f"{ICONSET}/Contents.json", '''{
  "images" : [
    {
      "filename" : "AppIcon-1024.png",
      "idiom" : "universal",
      "platform" : "ios",
      "size" : "1024x1024"
    },
    {
      "appearances" : [
        {
          "appearance" : "luminosity",
          "value" : "dark"
        }
      ],
      "filename" : "AppIcon-Dark-1024.png",
      "idiom" : "universal",
      "platform" : "ios",
      "size" : "1024x1024"
    },
    {
      "appearances" : [
        {
          "appearance" : "luminosity",
          "value" : "tinted"
        }
      ],
      "filename" : "AppIcon-Tinted-1024.png",
      "idiom" : "universal",
      "platform" : "ios",
      "size" : "1024x1024"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
''')
# Aperçu de revue : passer PREVIEW=chemin.png pour l'écrire (hors app)
import os
if os.environ.get('PREVIEW'):
    prev = Image.new('RGB', (1300, 360), '#ECECEC')
    x = 20
    for im in (raster(256, bg=TEAL, shape='rounded', mark_scale=1.30), raster(256, bg=TEAL, shape='circle', mark_scale=1.25),
               flat(raster(256, bg=DARK_BG, mark_scale=1.35, spine=DARK_BG), DARK_BG), flat(raster(256, bg='#000000', mark_scale=1.35, check='#B0B0B0', spine='#000000'), '#000000')):
        prev.paste(im.convert('RGBA'), (x, 20), im.convert('RGBA')); x += 290
    prev.save(os.environ['PREVIEW'])
print("assets générés")
