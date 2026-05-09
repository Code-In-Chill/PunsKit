package com.punshub.punskit.exception;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Ném ra khi có nhiều Bean cùng thỏa mãn một dependency
 * mà không có {@code @PPrimary} hay {@code @PQualifier} để phân biệt.
 */
public class AmbiguousBeanException extends FrameworkException {

    public AmbiguousBeanException(Class<?> requestedType, List<Class<?>> candidates) {
        super(buildMessage(requestedType, candidates));
    }

    private static String buildMessage(Class<?> type, List<Class<?>> candidates) {
        String list = candidates.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        return String.format(
                "Ambiguous bean for type '%s'. Found %d candidates: [%s].%n" +
                "Fix: add @PPrimary to one of them, or use @PQualifier to specify.",
                type.getSimpleName(), candidates.size(), list
        );
    }
}
