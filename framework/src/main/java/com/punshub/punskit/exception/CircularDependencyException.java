package com.punshub.punskit.exception;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Ném ra khi phát hiện vòng phụ thuộc vòng (A cần B, B cần A).
 * Message chứa toàn bộ dependency path để dễ debug.
 */
public class CircularDependencyException extends FrameworkException {

    public CircularDependencyException(List<Class<?>> dependencyPath, Class<?> conflicting) {
        super(buildMessage(dependencyPath, conflicting));
    }

    private static String buildMessage(List<Class<?>> path, Class<?> conflicting) {
        String chain = path.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.joining(" → "));
        return String.format(
                "Circular dependency detected!%n" +
                "Dependency chain: %s → %s (already resolving)%n" +
                "Fix: refactor one dependency to break the cycle.",
                chain, conflicting.getSimpleName()
        );
    }
}
