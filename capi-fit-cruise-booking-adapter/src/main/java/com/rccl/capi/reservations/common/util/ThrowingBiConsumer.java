package com.rccl.capi.reservations.common.util;

import com.rccl.capi.reservations.common.exceptions.RequestValidationException;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface ThrowingBiConsumer<T, U> extends BiConsumer<T, U> {

    @Override
    default void accept(final T extract, final U load) {
        try {
            acceptThrows(extract, load);
        } catch (final Exception e) {
            throw new RequestValidationException(e);
        }
    }

    void acceptThrows(T extract, U load) throws RequestValidationException;
}
