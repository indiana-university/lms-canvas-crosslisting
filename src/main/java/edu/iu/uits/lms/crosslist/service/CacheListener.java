package edu.iu.uits.lms.crosslist.service;

import lombok.extern.slf4j.Slf4j;

import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import java.io.Serializable;

@Slf4j
public class CacheListener implements CacheEntryCreatedListener<Object, Object>, CacheEntryExpiredListener<Object, Object>,
      CacheEntryRemovedListener<Object, Object>, CacheEntryUpdatedListener<Object, Object>, Serializable {

   /**
    * Do the event logging
    * @param cacheEntryEvents
    */
   private void onEvent(Iterable<CacheEntryEvent<?, ?>> cacheEntryEvents) {
      for (CacheEntryEvent<?, ?> entryEvent : cacheEntryEvents) {
         log.debug("Cache event = {}, Key = {},  Old value = {}, New value = {}", entryEvent.getEventType(),
               entryEvent.getKey(), entryEvent.getOldValue(), entryEvent.getValue());
      }
   }

   @Override
   public void onCreated(Iterable<CacheEntryEvent<?, ?>> cacheEntryEvents) throws CacheEntryListenerException {
      onEvent(cacheEntryEvents);
   }

   @Override
   public void onExpired(Iterable<CacheEntryEvent<?, ?>> cacheEntryEvents) throws CacheEntryListenerException {
      onEvent(cacheEntryEvents);
   }

   @Override
   public void onRemoved(Iterable<CacheEntryEvent<?, ?>> cacheEntryEvents) throws CacheEntryListenerException {
      onEvent(cacheEntryEvents);
   }

   @Override
   public void onUpdated(Iterable<CacheEntryEvent<?, ?>> cacheEntryEvents) throws CacheEntryListenerException {
      onEvent(cacheEntryEvents);
   }
}
