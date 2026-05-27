package com.ecommerce.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * ============================================================
 * RedisConfig.java — Configures how Redis caching works
 * ============================================================
 *
 * WHAT IS REDIS?
 *   Redis stands for Remote Dictionary Server.
 *   It is an extremely fast in-memory key-value store.
 *   "In-memory" means it stores data in RAM instead of on disk.
 *   RAM is much faster than a hard drive or even a database.
 *
 *   Think of Redis like this:
 *   - Your MySQL database = a big filing cabinet (slow, but permanent)
 *   - Redis = a sticky-note board on your desk (super fast, temporary)
 *
 *   When someone asks for all products for the first time:
 *     1. We go to MySQL, fetch the data (slow — maybe 100ms).
 *     2. We store that result in Redis as a "sticky note".
 *     3. We return the data to the user.
 *
 *   The next time someone asks for the same data:
 *     1. We check the Redis sticky-note board first.
 *     2. The answer is already there! Return it instantly (1-2ms).
 *     3. We never touched MySQL.
 *
 *   This is called a "cache hit". The database got zero requests!
 *
 * WHY DO WE NEED THIS CONFIG CLASS?
 *   Spring Boot can auto-configure Redis, but we want to customize
 *   two important things:
 *   1. SERIALIZATION: How data is converted to/from bytes in Redis.
 *      We use JSON format, so you can read the cached data in tools
 *      like Redis CLI or RedisInsight.
 *   2. TTL (Time to Live): How long a cached value stays before
 *      it automatically expires. We set it to 10 minutes.
 *      After 10 minutes, the next request goes to MySQL again.
 *
 * WHAT IS A @Bean?
 *   A @Bean method tells Spring: "Call this method once, keep the
 *   result, and share it with everyone who needs it."
 *   It's like making one cup of coffee and letting everyone take
 *   from the same pot — rather than making a new cup each time.
 *
 * @Configuration → Tells Spring this class defines configuration beans.
 */
@Configuration
public class RedisConfig {

    /**
     * Configures the CacheManager — the central cache controller.
     *
     * CacheManager is the boss that manages all named caches.
     * We have two caches:
     *   - "products" → stores the list of ALL products
     *   - "product"  → stores ONE product by its ID
     *
     * RedisConnectionFactory → Spring auto-creates this. It holds
     * the connection pool to our Redis server (localhost:6379).
     * We just receive it here via method parameter injection.
     *
     * RedisCacheConfiguration → Our rules for how the cache behaves:
     *   - TTL: 10 minutes (values auto-expire after this)
     *   - Key serializer: StringRedisSerializer (keys stored as plain text)
     *   - Value serializer: JSON (values stored as readable JSON strings)
     *   - disableCachingNullValues() → Never cache a null result.
     *     If a product is not found, we don't store "null" in Redis.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                // Cache entries expire automatically after 10 minutes.
                // After expiry, the next request will hit MySQL and re-populate the cache.
                .entryTtl(Duration.ofMinutes(10))
                // Store cache keys as plain text strings (e.g. "products", "product::1")
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                // Store cache values as JSON.
                // This makes it easy to inspect what's in the cache using Redis CLI.
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer())
                )
                // Don't cache null values.
                // If getAllProducts() returns null (unusual), we skip caching it.
                .disableCachingNullValues();

        // Build and return the CacheManager using our configuration and the Redis connection.
        return RedisCacheManager
                .builder(redisConnectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}
