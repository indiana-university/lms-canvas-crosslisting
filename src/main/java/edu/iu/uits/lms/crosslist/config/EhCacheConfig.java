package edu.iu.uits.lms.crosslist.config;

import edu.iu.uits.lms.crosslist.CrosslistConstants;
import edu.iu.uits.lms.crosslist.service.EhCacheListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.cache.Caching;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import java.util.concurrent.TimeUnit;

@Profile("ehcache")
@Configuration
@EnableCaching
@Slf4j
public class EhCacheConfig {
    @Bean(name = "CrosslistCacheManager")
    public CacheManager cacheManager() {
        log.debug("CrosslistCacheManager");

        // Spring doesn't natively support ehcache 3.  It does ehcache 2.
        // But ehcache 3 IS JCache compliant (JSR-107 specification) and
        // therefore Spring does support that.

        // One has the option of using a JCache configuration (via a MutableConfiguration)
        // or a direct ehcache configuration. There also appears to be a way to
        // configure with a MutableConfiguration and then pull out a complete configuration
        // to do vendor specific things.  But just using the ehcache configuration from
        // the start seems to be the easiest setup to give us a simple ehcache.

        // Using an ehcache configuration allows one to use things (which we aren't currently
        // using but one day might) specific to ehcache.
        //
        // http://www.ehcache.org/documentation/3.0/107.html

        // NOTE: Typing the cache seems to cause exceptions to be thrown about needing
        // getCache() defined.  But setting them to generic Object.class seems to solve this.
        // There might be a way around this but that is work for a future ticket!

//        int heapSize = 1000;
        final int ttl = 3600;
        final int courseServiceTtl = 300;

        CacheEntryListenerConfiguration<Object, Object> listenerConfiguration =
                new MutableCacheEntryListenerConfiguration<>(
                        FactoryBuilder.factoryOf(EhCacheListener.class),
                        null,
                        false,
                        false);

        final MutableConfiguration<Object, Object> mutableMediumAccessedConfiguration =
                new MutableConfiguration<Object, Object>()
                        .setTypes(Object.class, Object.class)
                        .setStoreByValue(false)
                        .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, courseServiceTtl)))
                        .setManagementEnabled(true)
                        .setStatisticsEnabled(true)
                        .addCacheEntryListenerConfiguration(listenerConfiguration);

        final CachingProvider provider = Caching.getCachingProvider(CrosslistConstants.EHCACHE_PROVIDER_TYPE);

        final javax.cache.CacheManager cacheManager = provider.getCacheManager();

        createCacheIfMissing(cacheManager, CrosslistConstants.COURSE_SECTIONS_CACHE_NAME, mutableMediumAccessedConfiguration);
        createCacheIfMissing(cacheManager, CrosslistConstants.COURSES_TAUGHT_BY_CACHE_NAME, mutableMediumAccessedConfiguration);

        return new JCacheCacheManager(cacheManager);
    }

    private void createCacheIfMissing(javax.cache.CacheManager cacheManager, String cacheName, MutableConfiguration<Object, Object> cacheConfig) {
        if (cacheManager.getCache(cacheName, Object.class, Object.class) == null) {
            cacheManager.createCache(cacheName, cacheConfig);
        }
    }
}
