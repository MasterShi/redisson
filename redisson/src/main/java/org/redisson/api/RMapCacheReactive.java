/**
 * Copyright (c) 2013-2022 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redisson.api;

import org.redisson.api.map.MapLoader;
import org.redisson.api.map.event.MapEntryListener;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * <p>Map-based cache with ability to set TTL for each entry via
 * {@link #put(Object, Object, long, TimeUnit)} or {@link #putIfAbsent(Object, Object, long, TimeUnit)} method.
 * And therefore has an complex lua-scripts inside.</p>
 *
 * <p>Current redis implementation doesnt have map entry eviction functionality.
 * Thus entries are checked for TTL expiration during any key/value/entry read operation.
 * If key/value/entry expired then it doesn't returns and clean task runs asynchronous.
 * Clean task deletes removes 100 expired entries at once.
 * In addition there is {@link org.redisson.eviction.EvictionScheduler}. This scheduler
 * deletes expired entries in time interval between 5 seconds to 2 hours.</p>
 *
 * <p>If eviction is not required then it's better to use {@link org.redisson.reactive.RedissonMapReactive}.</p>
 *
 * @author Nikita Koksharov
 *
 * @param <K> key
 * @param <V> value
 */
public interface RMapCacheReactive<K, V> extends RMapReactive<K, V>, RDestroyable {

    /**
     * Sets max size of the map.
     * Superfluous elements are evicted using LRU algorithm.
     * 
     * @param maxSize - max size
     * @return void
     */
    Mono<Void> setMaxSize(int maxSize);

    /**
     * Sets max size of the map and overrides current value.
     * Superfluous elements are evicted using defined algorithm.
     *
     * @param maxSize - max size
     * @param mode - eviction mode
     */
    Mono<Void> setMaxSize(int maxSize, EvictionMode mode);

    /**
     * Tries to set max size of the map. 
     * Superfluous elements are evicted using LRU algorithm. 
     *
     * @param maxSize - max size
     * @return <code>true</code> if max size has been successfully set, otherwise <code>false</code>.
     */
    Mono<Boolean> trySetMaxSize(int maxSize);

    /**
     * Tries to set max size of the map.
     * Superfluous elements are evicted using defined algorithm.
     *
     * @param maxSize - max size
     * @param mode - eviction mode
     * @return <code>true</code> if max size has been successfully set, otherwise <code>false</code>.
     */
    Mono<Boolean> trySetMaxSize(int maxSize, EvictionMode mode);

    /**
     * If the specified key is not already associated
     * with a value, associate it with the given value.
     * <p>
     * Stores value mapped by key with specified time to live.
     * Entry expires after specified time to live.
     * If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     *
     * @param key - map key
     * @param value - map value
     * @param ttl - time to live for key\value entry.
     *              If <code>0</code> then stores infinitely.
     * @param unit - time unit
     * @return previous associated value
     */
    Mono<V> putIfAbsent(K key, V value, long ttl, TimeUnit unit);

    /**
     * If the specified key is not already associated
     * with a value, associate it with the given value.
     * <p>
     * Stores value mapped by key with specified time to live and max idle time.
     * Entry expires when specified time to live or max idle time has expired.
     * <p>
     * If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     *
     * @param key - map key
     * @param value - map value
     * @param ttl - time to live for key\value entry.
     *              If <code>0</code> then time to live doesn't affect entry expiration.
     * @param ttlUnit - time unit
     * @param maxIdleTime - max idle time for key\value entry.
     *              If <code>0</code> then max idle time doesn't affect entry expiration.
     * @param maxIdleUnit - time unit
     * <p>
     * if <code>maxIdleTime</code> and <code>ttl</code> params are equal to <code>0</code>
     * then entry stores infinitely.
     *
     * @return previous associated value
     */
    Mono<V> putIfAbsent(K key, V value, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit);

    /**
     * Stores value mapped by key with specified time to live.
     * Entry expires after specified time to live.
     * If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     *
     * @param key - map key
     * @param value - map value
     * @param ttl - time to live for key\value entry.
     *              If <code>0</code> then stores infinitely.
     * @param unit - time unit
     * @return previous associated value
     */
    Mono<V> put(K key, V value, long ttl, TimeUnit unit);

    /**
     * Stores value mapped by key with specified time to live and max idle time.
     * Entry expires when specified time to live or max idle time has expired.
     * <p>
     * If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     *
     * @param key - map key
     * @param value - map value
     * @param ttl - time to live for key\value entry.
     *              If <code>0</code> then time to live doesn't affect entry expiration.
     * @param ttlUnit - time unit
     * @param maxIdleTime - max idle time for key\value entry.
     *              If <code>0</code> then max idle time doesn't affect entry expiration.
     * @param maxIdleUnit - time unit
     * <p>
     * if <code>maxIdleTime</code> and <code>ttl</code> params are equal to <code>0</code>
     * then entry stores infinitely.
     *
     * @return previous associated value
     */
    Mono<V> put(K key, V value, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit);

    /**
     * Stores value mapped by key with specified time to live.
     * Entry expires after specified time to live.
     * <p>
     * If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     * <p>
     * Works faster than usual {@link #put(Object, Object, long, TimeUnit)}
     * as it not returns previous value.
     *
     * @param key - map key
     * @param value - map value
     * @param ttl - time to live for key\value entry.
     *              If <code>0</code> then stores infinitely.
     * @param unit - time unit
     * 
     * @return <code>true</code> if key is a new key in the hash and value was set.
     *         <code>false</code> if key already exists in the hash and the value was updated.
     */
    Mono<Boolean> fastPut(K key, V value, long ttl, TimeUnit unit);

    /**
     * Stores value mapped by key with specified time to live and max idle time.
     * Entry expires when specified time to live or max idle time has expired.
     * <p>
     * If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     * <p>
     * Works faster than usual {@link #put(Object, Object, long, TimeUnit, long, TimeUnit)}
     * as it not returns previous value.
     *
     * @param key - map key
     * @param value - map value
     * @param ttl - time to live for key\value entry.
     *              If <code>0</code> then time to live doesn't affect entry expiration.
     * @param ttlUnit - time unit
     * @param maxIdleTime - max idle time for key\value entry.
     *              If <code>0</code> then max idle time doesn't affect entry expiration.
     * @param maxIdleUnit - time unit
     * <p>
     * if <code>maxIdleTime</code> and <code>ttl</code> params are equal to <code>0</code>
     * then entry stores infinitely.

     * @return <code>true</code> if key is a new key in the hash and value was set.
     *         <code>false</code> if key already exists in the hash and the value was updated.
     */
    Mono<Boolean> fastPut(K key, V value, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit);
    
    /**
     * If the specified key is not already associated
     * with a value, associate it with the given value.
     * <p>
     * Stores value mapped by key with specified time to live and max idle time.
     * Entry expires when specified time to live or max idle time has expired.
     * <p>
     * Works faster than usual {@link #putIfAbsent(Object, Object, long, TimeUnit, long, TimeUnit)}
     * as it not returns previous value.
     *
     * @param key - map key
     * @param value - map value
     * @param ttl - time to live for key\value entry.
     *              If <code>0</code> then time to live doesn't affect entry expiration.
     * @param ttlUnit - time unit
     * @param maxIdleTime - max idle time for key\value entry.
     *              If <code>0</code> then max idle time doesn't affect entry expiration.
     * @param maxIdleUnit - time unit
     * <p>
     * if <code>maxIdleTime</code> and <code>ttl</code> params are equal to <code>0</code>
     * then entry stores infinitely.
     *
     * @return <code>true</code> if key is a new key in the hash and value was set.
     *         <code>false</code> if key already exists in the hash
     */
    Mono<Boolean> fastPutIfAbsent(K key, V value, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit);

        /**
     * Updates time to live and max idle time of specified entry by key.
     * Entry expires when specified time to live or max idle time was reached.
     * <p>
     * Returns <code>false</code> if entry already expired or doesn't exist,
     * otherwise returns <code>true</code>.
     *
     * @param key - map key
     * @param ttl - time to live for key\value entry.
     *              If <code>0</code> then time to live doesn't affect entry expiration.
     * @param ttlUnit - time unit
     * @param maxIdleTime - max idle time for key\value entry.
     *              If <code>0</code> then max idle time doesn't affect entry expiration.
     * @param maxIdleUnit - time unit
     * <p>
     * if <code>maxIdleTime</code> and <code>ttl</code> params are equal to <code>0</code>
     * then entry stores infinitely.
     *
     * @return returns <code>false</code> if entry already expired or doesn't exist,
     *         otherwise returns <code>true</code>.
     */
    Mono<Boolean> updateEntryExpiration(K key, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleUnit);

    /**
     * Returns the value mapped by defined <code>key</code> or {@code null} if value is absent.
     * <p>
     * If map doesn't contain value for specified key and {@link MapLoader} is defined
     * then value will be loaded in read-through mode.
     * <p>
     * Idle time of entry is not taken into account.
     * Entry last access time isn't modified if map limited by size.
     *
     * @param key the key
     * @return the value mapped by defined <code>key</code> or {@code null} if value is absent
     */
    Mono<V> getWithTTLOnly(K key);

    /**
     * Returns map slice contained the mappings with defined <code>keys</code>.
     * <p>
     * If map doesn't contain value/values for specified key/keys and {@link MapLoader} is defined
     * then value/values will be loaded in read-through mode.
     * <p>
     * NOTE: Idle time of entry is not taken into account.
     * Entry last access time isn't modified if map limited by size.
     *
     * @param keys map keys
     * @return Map slice
     */
    Mono<Map<K, V>> getAllWithTTLOnly(Set<K> keys);

    /**
     * Returns the number of entries in cache.
     * This number can reflects expired entries too
     * due to non realtime cleanup process.
     *
     */
    @Override
    Mono<Integer> size();
    
    /**
     * Remaining time to live of map entry associated with a <code>key</code>. 
     *
     * @param key - map key
     * @return time in milliseconds
     *          -2 if the key does not exist.
     *          -1 if the key exists but has no associated expire.
     */
    Mono<Long> remainTimeToLive(K key);

    /**
     * Adds map entry listener
     *
     * @see org.redisson.api.map.event.EntryCreatedListener
     * @see org.redisson.api.map.event.EntryUpdatedListener
     * @see org.redisson.api.map.event.EntryRemovedListener
     * @see org.redisson.api.map.event.EntryExpiredListener
     *
     * @param listener - entry listener
     * @return listener id
     */
    Mono<Integer> addListener(MapEntryListener listener);

}
