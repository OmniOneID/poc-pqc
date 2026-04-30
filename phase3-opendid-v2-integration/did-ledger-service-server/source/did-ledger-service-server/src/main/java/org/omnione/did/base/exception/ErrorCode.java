package org.omnione.did.base.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 1. General errors (10000 ~ 10999)
    CLIENT_ERROR("SSRVLSS10000", "Client Error", 400),
    SERVER_ERROR("SSRVLSS10001", "Server Error", 500),
    ENCODING_FAILED("SSRVLSS10002", "Failed to encoding data.", 500),
    DECODING_FAILED("SSRVLSS10003", "Failed to decoding data.", 400),
    ENCRYPTION_FAILED("SSRVLSS10004", "Failed to encrypt data.", 500),
    DECRYPTION_FAILED("SSRVLSS100015", "Failed to decrypt data.", 400),


    // 2. Error during API processing (11000 ~ 11499)
    VERIFY_SIGN_FAIL("SSRVLSS11000", "Verify Signature Fail", 400),
    REQUEST_BODY_UNREADABLE("SSRVLSS11001", "", 500),


    // 3. DID-related errors (11500 ~ 11999)
    DID_DOC_VERSION_MISMATCH("SSRVLSS11500", "DidDoc version mismatch", 400),
    DID_NOT_FOUND("SSRVLSS11501", "DID not found", 400),
    ROLE_TYPE_MISMATCH("SSRVLSS11502", "Role type mismatch", 400),
    DID_NOT_ACTIVATED("SSRVLSS11503", "DID not activated", 400),
    DID_DOC_NOT_FOUND("SSRVLSS11504", "Failed to find DID Document: DID Document not found", 400),
    ROLE_DID_NOT_FOUND("SSRVLSS11505", "Failed to find DID: DID not found by Role", 400),
    TA_DID_DOC_NOT_FOUND("SSRVLSS11506", "Failed to find DID Document: TA DID Document not found", 400),


    // 4. VC-related errors (12000 ~ 12499)
    REVOKED_VC_CANNOT_UPDATE("SSRVLSS12000", "A revoked VC cannot be updated.", 400),
    INVALID_VC_SCHEMA("SSRVLSS12001", "Invalid VC Schema.", 400),
    VC_SCHEMA_ALREADY_REGISTERED("SSRVLSS12002", "Failed to register VC Schema: VC Schema already exists.", 400),
    VC_SCHEMA_NOT_FOUND("SSRVLSS12003", "VC Schema not found.", 500),


    // 5. ZKP-related errors (12500 ~ 12999)
    INVALID_CREDENTIAL_SCHEMA_ID("SSRVLSS12500", "Invalid Credential Schema ID.", 400),
    CREDENTIAL_SCHEMA_ALREADY_REGISTERED("SSRVLSS12501", "Failed to register Credential Schema: Credential Schema already exists.", 400),
    CREDENTIAL_SCHEMA_NOT_FOUND("SSRVLSS12502", "Credential Schema not found.", 500),
    CREDENTIAL_DEFINITION_ALREADY_EXISTS("SSRVLSS12503", "Failed to register Credential Definition: Credential Definition already exists.", 400),
    CREDENTIAL_DEFINITION_NOT_FOUND("SSRVLSS12504", "Credential Definition not found.", 400),
    INVALID_CREDENTIAL_SCHEMA("SSRVLSS12505", "Invalid Credential Schema.", 400),
    INVALID_CREDENTIAL_DEFINITION("SSRVLSS12506", "Invalid Credential Definition.", 400),


    // 6. Policy errors (13000 ~ 13499)
    TERMINATED_STATUS_CAN_NOT_CHANGE("SSRVLSS13000", "Terminated DIDs can't change their status.", 400),
    REVOKED_STATUS_CAN_NOT_CHANGE("SSRVLSS13001", "Revoked DIDs can't change their status to (De)Activate.", 400),
    DID_ROLE_MISMATCH_TA("SSRVLSS13002", "The DID's Role is not TA.", 400),


    // 7. DB-related errors (13500 ~ 13999)
    DB_CONNECTION_ERROR("SSRVLSS13500", "Database connection error.", 500),
    DB_QUERY_ERROR("SSRVLSS13501", "Database query error.", 500),
    DB_INSERT_ERROR("SSRVLSS13502", "Database insert error.", 500),
    DB_UPDATE_ERROR("SSRVLSS13503", "Database update error.", 500),
    DB_DELETE_ERROR("SSRVLSS13504", "Database delete error.", 500),


    // 99. Miscellaneous errors (90000 ~ 99999)
    TODO("SSRVLSS99999", "TODO.", 500),


    ;


    private final String code;
    private final String message;
    private final int httpStatus;

    /**
     * Constructor for ErrorCode enum.
     *
     * @param code       Error Code
     * @param message    Error Message
     * @param httpStatus HTTP Status Code
     */
    ErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * Get the error code.
     *
     * @return Error Code
     */
    public static String getMessageByCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode.getMessage();
            }
        }
        return "Unknown error code: " + code;
    }
}
