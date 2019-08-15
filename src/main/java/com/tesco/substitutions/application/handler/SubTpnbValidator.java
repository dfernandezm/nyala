package com.tesco.substitutions.application.handler;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class SubTpnbValidator {

    boolean validateTpnb(final String tpnb) {
        return StringUtils.isNotEmpty(tpnb) && isWellFormatted(tpnb);
    }

    boolean isWellFormatted(String tpnb) {
        try {
            Long.parseLong(tpnb);
            return true;
        } catch(Exception e) {
            log.info("tpnb is not well formatted {} ", tpnb);
            return false;
        }
    }

    boolean validateTpnbs(List<String> tpnbs){
        return tpnbs.stream().allMatch(this::validateTpnb);
    }
}
