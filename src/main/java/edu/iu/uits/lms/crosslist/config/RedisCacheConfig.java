package edu.iu.uits.lms.crosslist.config;

import edu.iu.uits.lms.crosslist.CrosslistConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.time.Duration;

@Profile("redis-cache")
@Configuration
@EnableCaching
@Slf4j
public class RedisCacheConfig {

    @Autowired
    private ToolConfig toolConfig;

    @Autowired
    private JedisConnectionFactory redisConnectionFactory;

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        final int courseServiceTtl = 300;
        return RedisCacheConfiguration.defaultCacheConfig()
              .entryTtl(Duration.ofSeconds(courseServiceTtl))
              .disableCachingNullValues()
              .prefixCacheNameWith(toolConfig.getEnv() + "-crosslister");
    }

    @Bean(name = "CrosslistCacheManager")
    public CacheManager cacheManager() {
        log.debug("cacheManager()");
        log.debug("Redis hostname: {}", redisConnectionFactory.getHostName());
        return RedisCacheManager.builder(redisConnectionFactory)
              .withCacheConfiguration(CrosslistConstants.COURSE_SECTIONS_CACHE_NAME, cacheConfiguration())
              .withCacheConfiguration(CrosslistConstants.COURSES_TAUGHT_BY_CACHE_NAME, cacheConfiguration())
              .build();
    }
}
