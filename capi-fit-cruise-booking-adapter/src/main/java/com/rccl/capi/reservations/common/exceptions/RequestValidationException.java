package com.rccl.capi.reservations.common.exceptions;

public class RequestValidationException extends RuntimeException {

    private static final String MSG = "Exception, during request data validation!";

    public RequestValidationException(Exception e) {
        super(MSG + " - " + e.getMessage(), e);
    }
}
