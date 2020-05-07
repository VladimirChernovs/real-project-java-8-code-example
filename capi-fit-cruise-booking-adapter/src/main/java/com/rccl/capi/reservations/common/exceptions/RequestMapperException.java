package com.rccl.capi.reservations.common.exceptions;

public class RequestMapperException extends Exception {

    private static final String DEFAULT_MSG = "There was an error when mapping the OTA request.";

    public RequestMapperException(Exception e) {
        super(DEFAULT_MSG, e);
    }

}