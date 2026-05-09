package com.punshub.punskit.exception;

/**
 * Ném ra khi framework không tìm thấy Bean phù hợp cho một dependency.
 * Thường xảy ra khi class chưa được đánh dấu {@code @PService} hoặc {@code @PComponent}.
 */
public class BeanNotFoundException extends FrameworkException {

    public BeanNotFoundException(Class<?> requestedType) {
        super(String.format(
                "No bean found for type '%s'.%n" +
                "Fix: make sure the class is annotated with @PService or @PComponent " +
                "and is within the scanned package.",
                requestedType.getName()
        ));
    }
}
