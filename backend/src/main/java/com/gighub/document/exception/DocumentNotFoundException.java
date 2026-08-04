package com.gighub.document.exception;

import com.gighub.common.exception.ResourceNotFoundException;

public class DocumentNotFoundException extends ResourceNotFoundException {

    public DocumentNotFoundException(String message) {
        super(message);
    }
}
