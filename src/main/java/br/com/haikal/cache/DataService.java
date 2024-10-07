package br.com.haikal.cache;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataService.class);
    public static final String MY_CACHE_DATA = "my-cache-data";

    @Inject
    @Channel("cache-invalidation-out")
    Emitter<String> emitter;


    @CacheResult(cacheName = MY_CACHE_DATA)
    public String getData(String key) {
        LOGGER.info("The data was get by primary sources. [key:{}]", key);
        //here the logic for get the data in the primary source
        return "Sample Data for key " + key;
    }

    public void updateData(String key) {
        //it is onçy for academic propose, the cache value is not updated, the objective is the flow.
        LOGGER.info("Update data was invoked. [key:{}]", key);

        //here the logic for update the data in the primary source

        //send the key to invalidate the cache in other instances
        LOGGER.info("Cache notification sent to other instances  [key:{}]", key);
        emitter.send(key);
    }

    @CacheInvalidate(cacheName = MY_CACHE_DATA)
    protected void invalidateData(String key) {
        LOGGER.info("The invalidate cache was invoked. [key:{}]", key);
    }

    @Incoming("cache-invalidation-in")
    protected void onCacheInvalidation(String key) {
        LOGGER.info("the invalidate cache request is received for key {}", key);
        invalidateData(key);
    }


}