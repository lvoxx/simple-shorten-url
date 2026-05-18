package io.lvoxx.ssurl.redis_starter.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import io.lvoxx.ssurl.common.util.Constants;

import java.time.Duration;

@AutoConfiguration
@EnableCaching
@ConditionalOnClass(RedissonClient.class)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = Constants.Beans.URL_BLOOM_FILTER)
    public RBloomFilter<String> urlBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(Constants.Cache.BLOOM_FILTER);
        bloomFilter.tryInit(Constants.Cache.BLOOM_CAPACITY, Constants.Cache.BLOOM_FPR);
        return bloomFilter;
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration shortUrlConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(Constants.Cache.TTL_HOURS))
                .computePrefixWith(cacheName -> Constants.Cache.KEY_PREFIX_SHORT)
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(RedisSerializer.string()));

        return RedisCacheManager.builder(connectionFactory)
                .withCacheConfiguration(Constants.Cache.SHORT_URLS, shortUrlConfig)
                .build();
    }
}
