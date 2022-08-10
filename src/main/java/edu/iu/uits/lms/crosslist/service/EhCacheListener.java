package edu.iu.uits.lms.crosslist.service;

/*-
 * #%L
 * lms-lti-crosslist
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import lombok.extern.slf4j.Slf4j;

import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import java.io.Serializable;

@Slf4j
public class EhCacheListener implements CacheEntryCreatedListener<Object, Object>, CacheEntryExpiredListener<Object, Object>,
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
