//
//  FlowLayout.swift
//  iosApp
//
//  Équivalent de FlowRow (Compose) : place les vues en ligne et passe à la ligne quand la largeur manque.
//

import SwiftUI

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        Self.arrange(
            sizes: subviews.map { $0.sizeThatFits(.unspecified) },
            maxWidth: proposal.width ?? .infinity,
            spacing: spacing
        ).size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = Self.arrange(
            sizes: subviews.map { $0.sizeThatFits(.unspecified) },
            maxWidth: bounds.width,
            spacing: spacing
        )
        for (index, origin) in result.origins.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + origin.x, y: bounds.minY + origin.y),
                proposal: .unspecified
            )
        }
    }

    /// Calcul de placement pur (sans SwiftUI) : retourne la taille totale et l'origine de chaque vue.
    /// Une vue passe à la ligne suivante quand elle ne tient plus dans `maxWidth` (sauf en début de ligne).
    static func arrange(sizes: [CGSize], maxWidth: CGFloat, spacing: CGFloat) -> (size: CGSize, origins: [CGPoint]) {
        var origins: [CGPoint] = []
        var x: CGFloat = 0, y: CGFloat = 0, rowHeight: CGFloat = 0, usedWidth: CGFloat = 0
        for size in sizes {
            if x > 0 && x + size.width > maxWidth {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            origins.append(CGPoint(x: x, y: y))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
            usedWidth = max(usedWidth, x - spacing)
        }
        return (CGSize(width: usedWidth, height: y + rowHeight), origins)
    }
}
