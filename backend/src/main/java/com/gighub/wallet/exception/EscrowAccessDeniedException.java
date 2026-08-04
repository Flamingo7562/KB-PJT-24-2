package com.gighub.wallet.exception;

import com.gighub.common.exception.ForbiddenException;

public class EscrowAccessDeniedException extends ForbiddenException {

    public EscrowAccessDeniedException(String message) {
        super(message);
    }
}
