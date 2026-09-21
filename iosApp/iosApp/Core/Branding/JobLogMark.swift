//
//  JobLogMark.swift
//  iosApp
//

import SwiftUI

/// Logo JobLog : carnet / journal minimaliste (couverture pleine + trait de reliure) avec une coche corail, dessiné en
/// vectoriel (net à toute taille, aucune image). Blanc + corail = à poser sur le teal de marque.
///
/// ⚠️ SYNCHRONISATION MANUELLE avec `branding/generate_brand_assets.py` (source de vérité de la géométrie, grille 108) :
/// les constantes ci-dessous (couverture x 36-72 / y 31-77, rayon 5, reliure x 44,5 de largeur 2,5, coche et largeur 5,5)
/// sont recopiées à la main. Toute modification du script doit être reportée ici (et inversement), puis vérifiée à l'écran.
struct JobLogMark: View {
    var bodyColor: Color = .white
    var checkColor: Color = Color(hex: 0xE8734A)   // Coral40
    var spineColor: Color = Color(hex: 0x0D6E68)   // « fente » de reliure : couleur du fond sur lequel le logo est posé (teal)

    /// Fenêtre visible = la couverture, en unités de la grille 108 (origine 36,31).
    private static let window = CGSize(width: 36, height: 46)
    private static let origin = CGPoint(x: 36, y: 31)

    var body: some View {
        GeometryReader { geo in
            let k = min(geo.size.width / Self.window.width, geo.size.height / Self.window.height)
            let o = CGPoint(x: (geo.size.width - Self.window.width * k) / 2, y: (geo.size.height - Self.window.height * k) / 2)
            ZStack {
                RoundedRectangle(cornerRadius: 5 * k)
                    .fill(bodyColor)
                    .frame(width: Self.window.width * k, height: Self.window.height * k)
                    .position(x: o.x + Self.window.width * k / 2, y: o.y + Self.window.height * k / 2)
                spine(k, o).stroke(spineColor, style: StrokeStyle(lineWidth: 2.5 * k))
                check(k, o).stroke(checkColor, style: StrokeStyle(lineWidth: 5.5 * k, lineCap: .round, lineJoin: .round))
            }
        }
        .aspectRatio(Self.window.width / Self.window.height, contentMode: .fit)
        .accessibilityHidden(true)
    }

    private func point(_ x: CGFloat, _ y: CGFloat, _ k: CGFloat, _ o: CGPoint) -> CGPoint {
        CGPoint(x: o.x + (x - Self.origin.x) * k, y: o.y + (y - Self.origin.y) * k)
    }

    // M44.5,31 V77
    private func spine(_ k: CGFloat, _ o: CGPoint) -> Path {
        Path { p in
            p.move(to: point(44.5, 31, k, o))
            p.addLine(to: point(44.5, 77, k, o))
        }
    }

    // M51,54 L57,60 L66,47
    private func check(_ k: CGFloat, _ o: CGPoint) -> Path {
        Path { p in
            p.move(to: point(51, 54, k, o))
            p.addLine(to: point(57, 60, k, o))
            p.addLine(to: point(66, 47, k, o))
        }
    }
}

/// Pastille de marque : teal primaire (fixe, identique clair/sombre) + logo.
struct JobLogBrandTile: View {
    var size: CGFloat = 72

    var body: some View {
        RoundedRectangle(cornerRadius: size * 0.28, style: .continuous)
            .fill(Color(hex: 0x0D6E68))
            .frame(width: size, height: size)
            .overlay(JobLogMark().padding(size * 0.2))
    }
}
