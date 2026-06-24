package com.sebatmal.devflow.common.exception;

import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.Getter;

@Getter
public class DevflowException extends RuntimeException {

    private final FailMessage failMessage;

    public DevflowException(final FailMessage failMessage) {
        super(failMessage.getMessage());
        this.failMessage = failMessage;
    }

    public DevflowException(final FailMessage failMessage, final Throwable cause) {
        super(failMessage.getMessage(), cause);
        this.failMessage = failMessage;
    }
}
