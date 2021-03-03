package com.nyala.server.common.errorhandling;

import lombok.Generated;

import java.beans.ConstructorProperties;

public class MissingConfigurationPropertyException extends RuntimeException {
    private String message;

    @ConstructorProperties({"message"})
    @Generated
    public MissingConfigurationPropertyException(String message) {
        this.message = message;
    }
}