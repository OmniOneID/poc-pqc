package org.omnione.did.base.exception;

import lombok.Getter;

@Getter
public class OpenDidException extends RuntimeException{
    private final ErrorCode errorCode;

    public OpenDidException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    public OpenDidException(ErrorCode errorCode, String addMessage) {
        super(errorCode.getMessage() + addMessage);
        this.errorCode = errorCode;
    }
}
