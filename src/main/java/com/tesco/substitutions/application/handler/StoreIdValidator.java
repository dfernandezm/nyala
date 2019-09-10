package com.tesco.substitutions.application.handler;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StoreIdValidator {

    public boolean isValidStoreId(final String storeId){
        return storeId.matches("[0-9]{4}");
    }
}
