package com.tesco.substitutions.application.handler;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

@Slf4j
@Singleton
public class StoreIdValidator {

    public boolean isValidStoreId(final String storeId){
        return storeId.matches("[0-9]{4}");
    }
}
