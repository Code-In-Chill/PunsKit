package com.punshub.punskit.annotation;

/**
 * Các loại phạm vi của Bean.
 */
public enum ScopeType {
    /**
     * Chỉ có một instance duy nhất trong toàn bộ lifecycle của plugin.
     * Mặc định cho tất cả các Bean.
     */
    SINGLETON,

    /**
     * Mỗi lần được yêu cầu (inject) sẽ tạo ra một instance mới.
     * Bean prototype sẽ không được framework quản lý vòng đời (@PreDestroy).
     */
    PROTOTYPE
}
