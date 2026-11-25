package com.erstorage.erstorageapi.demo;

import com.erstorage.erstorageapi.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DemoCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(DemoCleanupJob.class);

    private final DemoService demoService;
    private final StorageService storageService;
    private final DemoRateLimiter rateLimiter;

    public DemoCleanupJob(DemoService demoService, StorageService storageService, DemoRateLimiter rateLimiter) {
        this.demoService = demoService;
        this.storageService = storageService;
        this.rateLimiter = rateLimiter;
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT1M")
    public void run() {
        List<String> storageKeys = demoService.deleteExpiredGuests();

        if (!storageKeys.isEmpty()) {
            try {
                storageService.deleteAll(storageKeys);
                log.info("Deleted {} files of expired demo users", storageKeys.size());
            } catch (RuntimeException exception) {
                log.warn("Could not delete {} files of expired demo users from storage", storageKeys.size(), exception);
            }
        }

        rateLimiter.forgetOldEntries();
    }
}