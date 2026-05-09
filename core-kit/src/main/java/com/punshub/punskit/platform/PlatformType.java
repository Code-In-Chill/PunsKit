package com.punshub.punskit.platform;

/**
 * Các loại nền tảng Minecraft server được hỗ trợ.
 */
public enum PlatformType {
    /**
     * PaperMC 1.20.6+ (Sử dụng Brigadier & Lifecycle API).
     */
    PAPER,

    /**
     * Folia (Regionized multithreading).
     */
    FOLIA,

    /**
     * Spigot hoặc các bản cũ (Sử dụng CommandMap truyền thống).
     */
    LEGACY
}
