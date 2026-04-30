import Foundation
import CryptoKit
import SwiftDilithium

enum VcBench {
    static func run() {
        let payload = Data("vc-payload-for-phase1-pqc-bench".utf8)

        let secp = measureSecp256r1(payload: payload)
        let mlDsa = measureMlDsa44(payload: payload)

        PrettyTable.render(
            title: "Comparison: Secp256r1 vs ML-DSA-44 (VC Issue/Verify)",
            headers: ["Item", "Secp256r1", "ML-DSA-44", "Ratio"],
            rows: [
                ["Signature Size",
                 "\(secp.sigSize) B",
                 "\(mlDsa.sigSize) B",
                 ratio(mlDsa.sigSize, secp.sigSize)],
                ["Sign", ms(secp.signMs), ms(mlDsa.signMs), ratio(mlDsa.signMs, secp.signMs)],
                ["Verify", ms(secp.verifyMs), ms(mlDsa.verifyMs), ratio(mlDsa.verifyMs, secp.verifyMs)],
            ]
        )
    }

    private struct Result { let sigSize: Int; let signMs: Double; let verifyMs: Double }

    private static func measureSecp256r1(payload: Data) -> Result {
        let sk = P256.Signing.PrivateKey()
        let pk = sk.publicKey

        let signStart = Date()
        let sig = try! sk.signature(for: payload)
        let signMs = Date().timeIntervalSince(signStart) * 1000

        let verifyStart = Date()
        _ = pk.isValidSignature(sig, for: payload)
        let verifyMs = Date().timeIntervalSince(verifyStart) * 1000

        return Result(sigSize: sig.derRepresentation.count, signMs: signMs, verifyMs: verifyMs)
    }

    private static func measureMlDsa44(payload: Data) -> Result {
        let (sk, pk) = Dilithium.GenerateKeyPair(kind: .ML_DSA_44)
        let msg = Bytes(payload)

        let signStart = Date()
        let sig = sk.Sign(message: msg)
        let signMs = Date().timeIntervalSince(signStart) * 1000

        let verifyStart = Date()
        _ = pk.Verify(message: msg, signature: sig)
        let verifyMs = Date().timeIntervalSince(verifyStart) * 1000

        return Result(sigSize: sig.count, signMs: signMs, verifyMs: verifyMs)
    }

    private static func ms(_ v: Double) -> String { String(format: "%.3f ms", v) }
    private static func ratio(_ a: Int, _ b: Int) -> String {
        b == 0 ? "n/a" : String(format: "%.1fx", Double(a) / Double(b))
    }
    private static func ratio(_ a: Double, _ b: Double) -> String {
        b == 0 ? "n/a" : String(format: "%.2fx", a / b)
    }
}
