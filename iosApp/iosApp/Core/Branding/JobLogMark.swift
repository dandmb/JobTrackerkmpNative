
import SwiftUI

struct JobLogMark: View {
    var bodyColor: Color = .white
    var checkColor: Color = Color(hex: 0xE8734A)
    var spineColor: Color = Color(hex: 0x0D6E68)

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

    private func spine(_ k: CGFloat, _ o: CGPoint) -> Path {
        Path { p in
            p.move(to: point(44.5, 31, k, o))
            p.addLine(to: point(44.5, 77, k, o))
        }
    }

    private func check(_ k: CGFloat, _ o: CGPoint) -> Path {
        Path { p in
            p.move(to: point(51, 54, k, o))
            p.addLine(to: point(57, 60, k, o))
            p.addLine(to: point(66, 47, k, o))
        }
    }
}

struct JobLogBrandTile: View {
    var size: CGFloat = 72

    var body: some View {
        RoundedRectangle(cornerRadius: size * 0.28, style: .continuous)
            .fill(Color(hex: 0x0D6E68))
            .frame(width: size, height: size)
            .overlay(JobLogMark().padding(size * 0.2))
    }
}
