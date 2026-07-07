import Foundation
import SwiftDilithium

enum Scenario {
    static func run() throws {
        let (issuerSk, issuerPk) = Dilithium.GenerateKeyPair(kind: .ML_DSA_44)
        let (holderSk, holderPk) = Dilithium.GenerateKeyPair(kind: .ML_DSA_44)
        let issuerDid = "did:omn:issuer"
        let holderDid = "did:omn:holder"
        let controller = "did:omn:tas"

        section("1. Issuer DID Document (ML-DSA-44)")
        let issuerDoc = makeDidDoc(did: issuerDid, controller: controller, pkBytes: issuerPk.keyBytes)
        try printJson(issuerDoc)

        section("2. Holder DID Document (ML-DSA-44)")
        let holderDoc = makeDidDoc(did: holderDid, controller: controller, pkBytes: holderPk.keyBytes)
        try printJson(holderDoc)

        section("3. Issuer issues a VC for Holder")
        let vc = try issueVc(issuerDid: issuerDid, holderDid: holderDid, issuerSk: issuerSk)
        try printJson(vc)

        section("4. Verify VC signature (Issuer pubkey)")
        let vcOk = try verifyVc(vc, issuerPk: issuerPk)
        result(vcOk, label: "VC signature")

        section("5. Holder presents VP wrapping the VC")
        let challenge = "nonce-" + UUID().uuidString
        let vp = try presentVp(vc: vc, holderDid: holderDid, holderSk: holderSk, challenge: challenge)
        try printJson(vp)

        section("6. Verify VP signature (Holder pubkey)")
        let vpOk = try verifyVp(vp, holderPk: holderPk)
        result(vpOk, label: "VP signature")

        section("7. Verify embedded VC inside VP (Issuer pubkey)")
        let embeddedVc = vp.verifiableCredential[0]
        let embeddedOk = try verifyVc(embeddedVc, issuerPk: issuerPk)
        result(embeddedOk, label: "embedded VC signature")
    }

    private static func makeDidDoc(did: String, controller: String, pkBytes: [UInt8]) -> DidDocument {
        let mb = Multibase.encodeBase64(pkBytes)
        return DidDocument(
            context: ["https://www.w3.org/ns/did/v1"],
            id: did,
            controller: controller,
            verificationMethod: [
                VerificationMethod(
                    id: "\(did)#assert",
                    type: "MlDsa44VerificationKey2024",
                    controller: controller,
                    publicKeyMultibase: mb
                ),
                VerificationMethod(
                    id: "\(did)#auth",
                    type: "MlDsa44VerificationKey2024",
                    controller: controller,
                    publicKeyMultibase: mb
                ),
            ],
            assertionMethod: ["\(did)#assert"],
            authentication: ["\(did)#auth"],
            deactivated: false
        )
    }

    private static func issueVc(issuerDid: String, holderDid: String, issuerSk: SecretKey) throws -> VerifiableCredential {
        let unsigned = VerifiableCredential(
            context: [
                "https://www.w3.org/2018/credentials/v1",
                "https://www.w3.org/ns/did/v1",
            ],
            id: "urn:uuid:" + UUID().uuidString,
            type: ["VerifiableCredential"],
            issuer: issuerDid,
            issuanceDate: IsoDate.now(),
            credentialSubject: CredentialSubject(id: holderDid, name: "Alice"),
            proof: nil
        )
        let canonical = try Canonicalize.sortedJson(unsigned)
        let sig = issuerSk.Sign(message: Bytes(canonical))
        var signed = unsigned
        signed.proof = Proof(
            type: "MlDsa44Signature2024",
            proofPurpose: "assertionMethod",
            verificationMethod: "\(issuerDid)#assert",
            proofValue: Multibase.encodeBase64(sig)
        )
        return signed
    }

    private static func verifyVc(_ vc: VerifiableCredential, issuerPk: PublicKey) throws -> Bool {
        guard let proof = vc.proof, let sig = Multibase.decodeBase64(proof.proofValue) else { return false }
        var unsigned = vc
        unsigned.proof = nil
        let canonical = try Canonicalize.sortedJson(unsigned)
        return issuerPk.Verify(message: Bytes(canonical), signature: sig)
    }

    private static func presentVp(vc: VerifiableCredential, holderDid: String, holderSk: SecretKey, challenge: String) throws -> VerifiablePresentation {
        let unsigned = VerifiablePresentation(
            context: ["https://www.w3.org/2018/credentials/v1"],
            type: ["VerifiablePresentation"],
            verifiableCredential: [vc],
            holder: holderDid,
            proof: nil
        )
        let canonical = try Canonicalize.sortedJson(unsigned)
        let sig = holderSk.Sign(message: Bytes(canonical))
        var signed = unsigned
        signed.proof = Proof(
            type: "MlDsa44Signature2024",
            proofPurpose: "authentication",
            verificationMethod: "\(holderDid)#auth",
            proofValue: Multibase.encodeBase64(sig),
            challenge: challenge
        )
        return signed
    }

    private static func verifyVp(_ vp: VerifiablePresentation, holderPk: PublicKey) throws -> Bool {
        guard let proof = vp.proof, let sig = Multibase.decodeBase64(proof.proofValue) else { return false }
        var unsigned = vp
        unsigned.proof = nil
        let canonical = try Canonicalize.sortedJson(unsigned)
        return holderPk.Verify(message: Bytes(canonical), signature: sig)
    }

    private static func section(_ s: String) {
        print()
        print("=== \(s) ===")
    }

    private static func result(_ ok: Bool, label: String) {
        print(ok ? "  ✅ \(label) valid" : "  ❌ \(label) INVALID")
    }

    private static func printJson<T: Encodable>(_ value: T) throws {
        print(try Canonicalize.prettyString(value))
    }
}
