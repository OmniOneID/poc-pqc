import Foundation

enum Multibase {
    static func encodeBase64(_ bytes: [UInt8]) -> String {
        return "m" + Data(bytes).base64EncodedString()
    }

    static func decodeBase64(_ s: String) -> [UInt8]? {
        guard s.hasPrefix("m") else { return nil }
        let body = String(s.dropFirst())
        return Data(base64Encoded: body).map { [UInt8]($0) }
    }
}
