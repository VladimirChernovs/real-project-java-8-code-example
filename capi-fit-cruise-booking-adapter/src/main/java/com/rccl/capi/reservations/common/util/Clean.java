package com.rccl.capi.reservations.common.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

public interface Clean {

    static Optional<String> string(String str) {
        return ofNullable(str).filter(StringUtils::isNotBlank)
                .map(s -> s.replace("&", " "))
                .map(StringUtils::trim);
    }

    static <T> Optional<T> all(T obj) {
        return ofNullable(obj).flatMap(x -> {
            if (x instanceof String) {
                return Clean.string((String) x).map(s -> ((T) s));
            }
            return of(x);
        });
    }

}
