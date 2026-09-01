package com.jumbo.trus.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String HOME_STATS_CACHE = "homeStats";

    @Bean
    public CacheManager cacheManager(
            @Value("${cache.home-stats-ttl-seconds:15}") long homeStatsTtlSeconds
    ) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(HOME_STATS_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofSeconds(Math.max(1, homeStatsTtlSeconds))));
        return cacheManager;
    }
}
