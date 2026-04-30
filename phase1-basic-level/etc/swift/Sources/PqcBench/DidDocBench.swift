import Foundation
import CryptoKit
import SwiftDilithium

enum DidDocBench {
    static func run() {
        let secp = measureSecp256r1KeyGen()
        let mlDsa = measureMlDsa44KeyGen()

        PrettyTable.render(
            title: "Comparison: Secp256r1 vs ML-DSA-44 (DID Document)",
            headers: ["Item", "Secp256r1", "ML-DSA-44", "Ratio"],
            rows: [
                ["Public Key Size",
                 "\(secp.pubKeySize) B",
                 "\(mlDsa.pubKeySize) B",
                 ratio(mlDsa.pubKeySize, secp.pubKeySize)],
                ["Key Gen (x2)",
                 ms(secp.keyGenMs),
                 ms(mlDsa.keyGenMs),
                 ratio(mlDsa.keyGenMs, secp.keyGenMs)],
            ]
        )
    }

    private struct Result { let pubKeySize: Int; let keyGenMs: Double }

    private static func measureSecp256r1KeyGen() -> Result {
        let start = Date()
        let k1 = P256.Signing.PrivateKey()
        let k2 = P256.Signing.PrivateKey()
        let elapsed = Date().timeIntervalSince(start) * 1000
        let pkSize = k1.publicKey.compressedRepresentation.count
        _ = k2
        return Result(pubKeySize: pkSize, keyGenMs: elapsed)
    }

    private static func measureMlDsa44KeyGen() -> Result {
        let start = Date()
        let (_, pk1) = Dilithium.GenerateKeyPair(kind: .ML_DSA_44)
        let (_, pk2) = Dilithium.GenerateKeyPair(kind: .ML_DSA_44)
        let elapsed = Date().timeIntervalSince(start) * 1000
        _ = pk2
        return Result(pubKeySize: pk1.keyBytes.count, keyGenMs: elapsed)
    }

    private static func ms(_ v: Double) -> String { String(format: "%.3f ms", v) }
    private static func ratio(_ a: Int, _ b: Int) -> String {
        b == 0 ? "n/a" : String(format: "%.1fx", Double(a) / Double(b))
    }
    private static func ratio(_ a: Double, _ b: Double) -> String {
        b == 0 ? "n/a" : String(format: "%.2fx", a / b)
    }
}
