package com.punshub.punskit.platform;

import com.punshub.punskit.logging.PunsLogger;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Tiện ích nhận diện nền tảng server đang chạy.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlatformDetector {

    /**
     * Nhận diện nền tảng hiện tại dựa trên sự hiện diện của các class đặc trưng.
     * Thứ tự kiểm tra: Folia -> Paper Modern -> Legacy.
     */
    public static PlatformType detect(PunsLogger logger) {
        // 1. Check Folia (cụ thể nhất)
        if (hasClass("io.papermc.paper.threadedregions.RegionizedServer")) {
            logger.debug("Detected Folia platform.");
            return PlatformType.FOLIA;
        }

        // 2. Check Paper Modern (1.20.6+ Lifecycle API)
        if (hasClass("io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager")) {
            logger.debug("Detected Paper Modern platform (1.20.6+).");
            return PlatformType.PAPER;
        }

        // 3. Fallback to Legacy (Spigot/Old Paper)
        logger.debug("Detected Legacy/Spigot platform.");
        return PlatformType.LEGACY;
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
