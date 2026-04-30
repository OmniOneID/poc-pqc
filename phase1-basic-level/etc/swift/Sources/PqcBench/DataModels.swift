import Foundation

enum IsoDate {
    static func now() -> String {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f.string(from: Date())
    }
}

struct VerificationMethod: Encodable {
    let id: String
    let type: String
    let controller: String
    let publicKeyMultibase: String
}

struct DidDocument: Encodable {
    let context: [String]
    let id: String
    let controller: String
    let verificationMethod: [VerificationMethod]
    let assertionMethod: [String]
    let authentication: [String]
    let deactivated: Bool

    enum CodingKeys: String, CodingKey {
        case context = "@context"
        case id, controller, verificationMethod, assertionMethod, authentication, deactivated
    }
}

struct Proof: Encodable {
    let type: String
    let created: String
    let proofPurpose: String
    let verificationMethod: String
    let proofValue: String
    let challenge: String?

    init(
        type: String,
        proofPurpose: String,
        verificationMethod: String,
        proofValue: String,
        challenge: String? = nil
    ) {
        self.type = type
        self.created = IsoDate.now()
        self.proofPurpose = proofPurpose
        self.verificationMethod = verificationMethod
        self.proofValue = proofValue
        self.challenge = challenge
    }

    enum CodingKeys: String, CodingKey {
        case type, created, proofPurpose, verificationMethod, proofValue, challenge
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(type, forKey: .type)
        try c.encode(created, forKey: .created)
        try c.encode(proofPurpose, forKey: .proofPurpose)
        try c.encode(verificationMethod, forKey: .verificationMethod)
        try c.encode(proofValue, forKey: .proofValue)
        try c.encodeIfPresent(challenge, forKey: .challenge)
    }
}

struct CredentialSubject: Encodable {
    let id: String
    let name: String
}

struct VerifiableCredential: Encodable {
    let context: [String]
    let id: String
    let type: [String]
    let issuer: String
    let issuanceDate: String
    let credentialSubject: CredentialSubject
    var proof: Proof?

    enum CodingKeys: String, CodingKey {
        case context = "@context"
        case id, type, issuer, issuanceDate, credentialSubject, proof
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(context, forKey: .context)
        try c.encode(id, forKey: .id)
        try c.encode(type, forKey: .type)
        try c.encode(issuer, forKey: .issuer)
        try c.encode(issuanceDate, forKey: .issuanceDate)
        try c.encode(credentialSubject, forKey: .credentialSubject)
        try c.encodeIfPresent(proof, forKey: .proof)
    }
}

struct VerifiablePresentation: Encodable {
    let context: [String]
    let type: [String]
    let verifiableCredential: [VerifiableCredential]
    let holder: String
    var proof: Proof?

    enum CodingKeys: String, CodingKey {
        case context = "@context"
        case type, verifiableCredential, holder, proof
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(context, forKey: .context)
        try c.encode(type, forKey: .type)
        try c.encode(verifiableCredential, forKey: .verifiableCredential)
        try c.encode(holder, forKey: .holder)
        try c.encodeIfPresent(proof, forKey: .proof)
    }
}
