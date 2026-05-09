package com.yourname.testplugin.service;

import com.punshub.punskit.annotation.scheduler.PScheduled;
import com.punshub.punskit.annotation.di.PService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PService
public class SchedulerTestService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerTestService.class);
    private int count = 0;

    @PScheduled(delay = 100, period = 200) // Chạy sau 5s, lặp mỗi 10s
    public void repeatingTask() {
        count++;
        log.info("✓ [Scheduled] Repeating task executed. Count: {}", count);
    }

    @PScheduled(delay = 60, runOnce = true) // Chạy sau 3s một lần duy nhất
    public void oneTimeTask() {
        log.info("✓ [Scheduled] One-time task executed.");
    }

    @PScheduled(delay = 40, async = true, runOnce = true)
    public void asyncTask() {
        log.info("✓ [Scheduled] Async task executed on thread: {}", Thread.currentThread().getName());
    }
}
