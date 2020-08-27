package io.smallrye.common.powerannotations.impl;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

class Utils {
    /**
     * Collect to an {@link Optional}: if the Stream is empty return an empty Optional.
     * If it contains one element, return an Optional with that element. If the stream
     * contains more than one element get an exception from the supplier and throw it.
     */
    static <T> Collector<T, ?, Optional<T>> toOptionalOrThrow(Function<List<T>, ? extends RuntimeException> throwableSupplier) {
        return Collector.of(
                (Supplier<List<T>>) ArrayList::new,
                List::add,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                },
                list -> {
                    switch (list.size()) {
                        case 0:
                            return Optional.empty();
                        case 1:
                            return Optional.of(list.get(0));
                        default:
                            throw throwableSupplier.apply(list);
                    }
                });
    }

    static <T extends Enum<T>> Enum<T> enumValue(Class<?> type, String value) {
        @SuppressWarnings("unchecked")
        Class<T> enumType = (Class<T>) type;
        return Enum.valueOf(enumType, value);
    }

    static String signature(String methodName, String... argTypeNames) {
        return methodName + Stream.of(argTypeNames).collect(joining(", ", "(", ")"));
    }
}
