package io.lvoxx.ssurl.test_starter.config;

@TestConfiguration
public class NoCacheLoadConfig {
    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new NoOpCacheManager(); // Cache manager không làm gì cả
    }
}