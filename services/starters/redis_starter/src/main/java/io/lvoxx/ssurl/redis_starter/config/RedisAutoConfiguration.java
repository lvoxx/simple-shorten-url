package io.lvoxx.ssurl.redis_starter.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
public class RedisAutoConfiguration {

    private static final int BLOOM_FILTER_CAPACITY = 10_000_000;
    private static final double BLOOM_FILTER_FALSE_POSITIVE_RATE = 0.01;

    @Bean
    @ConditionalOnMissingBean(name = "urlBloomFilter")
    public RBloomFilter<String> urlBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("bloom:urls");
        bloomFilter.tryInit(BLOOM_FILTER_CAPACITY, BLOOM_FILTER_FALSE_POSITIVE_RATE);
        return bloomFilter;
    }
}
