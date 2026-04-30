import Foundation
import CryptoKit
import SwiftKyber

enum KeyAgreementBench {
    static func run() {
        let ecdh = measureEcdh()
        let mlKem = measureMlKem768()

        PrettyTable.render(
            title: "Comparison: ECDH(Secp256r1) vs ML-KEM-768",
            headers: ["Item", "ECDH", "ML-KEM-768", "Ratio"],
            rows: [
                ["Public Key Size",
                 "\(ecdh.pubKeySize) B",
                 "\(mlKem.pubKeySize) B",
                 ratio(mlKem.pubKeySize, ecdh.pubKeySize)],
                ["Ciphertext Size",
                 "n/a",
                 "\(mlKem.ctSize) B",
                 "n/a"],
                ["Total Protocol",
                 ms(ecdh.totalMs),
                 ms(mlKem.totalMs),
                 ratio(mlKem.totalMs, ecdh.totalMs)],
            ]
        )
    }

    private struct Result {
        let pubKeySize: Int
        let ctSize: Int
        let totalMs: Double
    }

    private static func measureEcdh() -> Result {
        let start = Date()
        let alice = P256.KeyAgreement.PrivateKey()
        let bob = P256.KeyAgreement.PrivateKey()
        let ssA = try! alice.sharedSecretFromKeyAgreement(with: bob.publicKey)
        let ssB = try! bob.sharedSecretFromKeyAgreement(with: alice.publicKey)
        let elapsed = Date().timeIntervalSince(start) * 1000
        precondition(ssA == ssB, "ECDH session keys should match")
        return Result(
            pubKeySize: alice.publicKey.compressedRepresentation.count,
            ctSize: 0,
            totalMs: elapsed
        )
    }

    private static func measureMlKem768() -> Result {
        let start = Date()
        let (encap, decap) = Kyber.GenerateKeyPair(kind: .K768)
        let (kAlice, ct) = encap.Encapsulate()
        let kBob = try! decap.Decapsulate(ct: ct)
        let elapsed = Date().timeIntervalSince(start) * 1000
        precondition(kAlice == kBob, "ML-KEM session keys should match")
        return Result(
            pubKeySize: encap.keyBytes.count,
            ctSize: ct.count,
            totalMs: elapsed
        )
    }

    private static func ms(_ v: Double) -> String { String(format: "%.3f ms", v) }
    private static func ratio(_ a: Int, _ b: Int) -> String {
        b == 0 ? "n/a" : String(format: "%.1fx", Double(a) / Double(b))
    }
    private static func ratio(_ a: Double, _ b: Double) -> String {
        b == 0 ? "n/a" : String(format: "%.2fx", a / b)
    }
}
