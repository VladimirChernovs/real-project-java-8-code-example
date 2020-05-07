package com.rccl.capi.reservations.common.util;

import java.util.List;
import java.util.Optional;

import static com.rccl.capi.reservations.common.util.CommonArt.notEmptyList;


public interface Idx {

    static <T> Optional<T> first(List<T> list) {
        return Optional.ofNullable(list).filter(notEmptyList).map(l -> l.get(0));
    }

    static <T> Optional<T> second(List<T> list) {
        return Optional.ofNullable(list).filter(l -> l.size() == 2).map(l -> l.get(1));
    }

    static <T> Optional<T> last(List<T> list) {
        return Optional.ofNullable(list).filter(notEmptyList).map(l -> l.get(l.size() - 1));
    }

}
