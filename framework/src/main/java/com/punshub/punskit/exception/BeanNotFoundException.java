package com.punshub.punskit.exception;

/**
 * Ném ra khi framework không tìm thấy Bean phù hợp cho một dependency.
 * Thường xảy ra khi class chưa được đánh dấu {@code @Service} hoặc {@code @Component}.
 */
public class BeanNotFoundException extends FrameworkException {

    public BeanNotFoundException(Class<?> requestedType) {
        super(String.format(
                "No bean found for type '%s'.%n" +
                "Fix: make sure the class is annotated with @Service or @Component " +
                "and is within the scanned package.",
                requestedType.getName()
        ));
    }
}
