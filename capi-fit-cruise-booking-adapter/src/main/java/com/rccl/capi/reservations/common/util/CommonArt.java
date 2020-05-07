package com.rccl.capi.reservations.common.util;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Optional.ofNullable;

public final class CommonArt {

    public static final Predicate<List<?>> notEmptyList = l -> !l.isEmpty();

    // not for instantiation
    private CommonArt() {
    }

    public static <S, T, V> ThrowingBiConsumer<S, T> mapFun(Function<S, Optional<V>> extract, BiConsumer<T, V> load) {
        return mapFun(extract, load, Optional::ofNullable);
    }

    public static <S, T, X, V> ThrowingBiConsumer<S, T> mapFun(Function<S, Optional<X>> extract,
                                                               BiConsumer<T, V> load,
                                                               Function<X, Optional<V>> transform) {
        return (S source, T target) -> extract.apply(source).flatMap(Clean::all)
                .flatMap(transform)
                .ifPresent(v -> load.accept(target, v));
    }

    public static Optional<BigDecimal> stringToBigDecimal(String in) {
        BigDecimal num;
        try {
            num = new BigDecimal(in);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return Optional.of(num);
    }

    public static <S, T> void mapBigDecWithException(S source, Function<S, Optional<String>> extract,
                                                     T target, BiConsumer<T, BigDecimal> load) {
        Optional<String> ex = extract.apply(source);
        if (ex.isPresent()) {
            String x = ex.get();
            if (StringUtils.isBlank(x)) return;
            load.accept(target, new BigDecimal(x));
        }
    }

    public static Object invokePrivateMethod(Class<?> targetClass, Object targetObj,
                                             String methodName, Class[] argClasses, Object[] argObjects) {
        try {
            Method method = targetClass.getDeclaredMethod(methodName, argClasses);
            method.setAccessible(true);
            return method.invoke(targetObj, argObjects);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            return null;
        }
    }

    public <L> Optional<List<L>> ofEmptyList(List<L> list) {
        return ofNullable(list).filter(notEmptyList);
    }
}

