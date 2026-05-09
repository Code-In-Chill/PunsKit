package com.punshub.punskit.annotation.di;

/**
 * Các loại phạm vi của Bean.
 */
public enum PScopeType {
    /**
     * Chỉ có một instance duy nhất trong toàn bộ lifecycle của plugin.
     */
    SINGLETON,

    /**
     * Mỗi lần được yêu cầu (inject) sẽ tạo ra một instance mới.
     */
    PROTOTYPE
}
