import Foundation

enum PrettyTable {
    static func render(title: String, headers: [String], rows: [[String]]) {
        let widths = computeWidths(headers: headers, rows: rows)
        let total = widths.reduce(0, +) + (widths.count - 1) * 3

        let top = "╔" + String(repeating: "═", count: total + 2) + "╗"
        let mid = "╠" + colBorder("═", "╪", widths) + "╣"
        let bot = "╚" + colBorder("═", "╧", widths) + "╝"

        print(top)
        print("║ " + center(title, width: total) + " ║")
        print(mid)
        printRow(headers, widths: widths)
        print(mid)
        for row in rows { printRow(row, widths: widths) }
        print(bot)
    }

    private static func computeWidths(headers: [String], rows: [[String]]) -> [Int] {
        var widths = headers.map { $0.count }
        for row in rows {
            for (i, cell) in row.enumerated() where i < widths.count {
                widths[i] = max(widths[i], cell.count)
            }
        }
        return widths
    }

    private static func colBorder(_ fill: String, _ sep: String, _ widths: [Int]) -> String {
        widths.map { String(repeating: fill, count: $0 + 2) }.joined(separator: sep)
    }

    private static func printRow(_ cells: [String], widths: [Int]) {
        let padded = zip(cells, widths).map { pad($0, width: $1) }
        print("║ " + padded.joined(separator: " │ ") + " ║")
    }

    private static func pad(_ s: String, width: Int) -> String {
        let diff = width - s.count
        return diff > 0 ? String(repeating: " ", count: diff) + s : s
    }

    private static func center(_ s: String, width: Int) -> String {
        let diff = width - s.count
        guard diff > 0 else { return s }
        let left = diff / 2
        return String(repeating: " ", count: left) + s
            + String(repeating: " ", count: diff - left)
    }
}
