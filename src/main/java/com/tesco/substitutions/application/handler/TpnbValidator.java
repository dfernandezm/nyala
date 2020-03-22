package com.tesco.substitutions.application.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Singleton;
import java.util.List;

@Slf4j
@Singleton
public class TpnbValidator {

    boolean isValidTpnb(final String tpnb) {
        return StringUtils.isNotEmpty(tpnb) && isWellFormatted(tpnb);
    }

    boolean areValidTpnbs(List<String> tpnbs){
        return !tpnbs.isEmpty() && tpnbs.stream().allMatch(this::isValidTpnb);
    }

    private boolean isWellFormatted(String tpnb) {
        try {
            Long.parseLong(tpnb);
            return true;
        } catch(Exception e) {
            log.info("tpnb is not well formatted {} ", tpnb);
            return false;
        }
    }
}
