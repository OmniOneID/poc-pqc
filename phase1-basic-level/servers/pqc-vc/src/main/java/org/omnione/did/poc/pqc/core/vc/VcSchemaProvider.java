package org.omnione.did.poc.pqc.core.vc;

// Identity VC 스키마 (임시 테스트용)
public class VcSchemaProvider {

    public static final String NAMESPACE_ID = "org.opendid.v1.identity";

    /** 텍스트 10개 클레임 스키마 */
    public static String getSchema() {
        return getTextOnlySchema();
    }

    /** 텍스트 10개 클레임 스키마 */
    public static String getTextOnlySchema() {
        return """
            {
                "@id": "https://example.com/api/v1/vc-schema?name=identity",
                "@schema": "https://opendid.org/schema/vc.osd",
                "title": "PQC Identity Verifiable Credential",
                "description": "Identity VC with ML-DSA-44 PQC signing",
                "metadata": {
                    "language": "ko",
                    "formatVersion": "1.0"
                },
                "credentialSubject": {
                    "claims": [{
                        "namespace": {
                            "id": "org.opendid.v1.identity",
                            "name": "PQC Identity Credential",
                            "ref": " "
                        },
                        "items": [
                            {"id": "given_name", "caption": "Given Name", "type": "text", "format": "plain"},
                            {"id": "family_name", "caption": "Family Name", "type": "text", "format": "plain"},
                            {"id": "birthdate", "caption": "Birthdate", "type": "text", "format": "plain"},
                            {"id": "national_id", "caption": "National ID", "type": "text", "format": "plain"},
                            {"id": "address", "caption": "Address", "type": "text", "format": "plain"},
                            {"id": "phone", "caption": "Phone", "type": "text", "format": "plain"},
                            {"id": "email", "caption": "Email", "type": "text", "format": "plain"},
                            {"id": "gender", "caption": "Gender", "type": "text", "format": "plain"},
                            {"id": "nationality", "caption": "Nationality", "type": "text", "format": "plain"},
                            {"id": "issue_date", "caption": "Issue Date", "type": "text", "format": "plain"}
                        ]
                    }]
                }
            }
            """;
    }

    /** 텍스트 10개 + JPG 이미지 1개 (11 클레임) 스키마 */
    public static String getWithPhotoSchema() {
        return """
            {
                "@id": "https://example.com/api/v1/vc-schema?name=identity-photo",
                "@schema": "https://opendid.org/schema/vc.osd",
                "title": "PQC Identity Verifiable Credential (with Photo)",
                "description": "Identity VC with ML-DSA-44 PQC signing including photo",
                "metadata": {
                    "language": "ko",
                    "formatVersion": "1.0"
                },
                "credentialSubject": {
                    "claims": [{
                        "namespace": {
                            "id": "org.opendid.v1.identity",
                            "name": "PQC Identity Credential",
                            "ref": " "
                        },
                        "items": [
                            {"id": "given_name", "caption": "Given Name", "type": "text", "format": "plain"},
                            {"id": "family_name", "caption": "Family Name", "type": "text", "format": "plain"},
                            {"id": "birthdate", "caption": "Birthdate", "type": "text", "format": "plain"},
                            {"id": "national_id", "caption": "National ID", "type": "text", "format": "plain"},
                            {"id": "address", "caption": "Address", "type": "text", "format": "plain"},
                            {"id": "phone", "caption": "Phone", "type": "text", "format": "plain"},
                            {"id": "email", "caption": "Email", "type": "text", "format": "plain"},
                            {"id": "gender", "caption": "Gender", "type": "text", "format": "plain"},
                            {"id": "nationality", "caption": "Nationality", "type": "text", "format": "plain"},
                            {"id": "issue_date", "caption": "Issue Date", "type": "text", "format": "plain"},
                            {"id": "photo", "caption": "Photo", "type": "image", "format": "jpg"}
                        ]
                    }]
                }
            }
            """;
    }
}
