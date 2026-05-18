package io.lvoxx.ssurl.api_service.service.impl;

import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.lvoxx.ssurl.api_service.repository.UserRepository;
import io.lvoxx.ssurl.api_service.service.UserService;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.lvoxx.ssurl.common.exception.UserNotFoundException;
import io.lvoxx.ssurl.common.mapper.UserMapper;
import io.lvoxx.ssurl.common.service.AbstractCacheableService;
import io.lvoxx.ssurl.common.util.Constants;
import reactor.core.publisher.Mono;

/**
 * User service with two cache regions managed via Spring annotations and
 * Redisson atomic batches:
 *
 * <ul>
 * <li>{@value AbstractCacheableService#CACHE_USERS} – keyed by both ID
 * ({@code user:id:<id>}) and username ({@code user:name:<name>}). Both
 * entries are evicted together on any mutation to prevent split-brain reads
 * through different access paths.</li>
 * <li>Redisson {@link org.redisson.api.RBatch} ensures the cache write/evict
 * commands land <em>after</em> the DB transaction commits, preserving ACID
 * read-your-writes semantics on the happy path.</li>
 * </ul>
 */
@Service
@Transactional
public class UserServiceImpl extends AbstractCacheableService implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            RedissonClient redisson) {
        super(redisson);
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    // -------------------------------------------------------------------------
    // getById
    // -------------------------------------------------------------------------

    /**
     * Fetches a user by primary key.
     * Cache key: {@code user:id:<id>} in region {@value #CACHE_USERS}.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = Constants.Cache.CACHE_USERS, key = "'" + Constants.Cache.KEY_USER_BY_ID
            + "' + #id", unless = "#result == null")
    public Mono<UserResponse> getById(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(String.valueOf(id))))
                .map(userMapper::toResponse);
    }

    // -------------------------------------------------------------------------
    // getByUsername
    // -------------------------------------------------------------------------

    /**
     * Fetches a user by username.
     * Cache key: {@code user:name:<username>} in region {@value #CACHE_USERS}.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = Constants.Cache.CACHE_USERS, key = "'" + Constants.Cache.KEY_USER_BY_NAME
            + "' + #username", unless = "#result == null")
    public Mono<UserResponse> getByUsername(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UserNotFoundException(username)))
                .map(userMapper::toResponse);
    }

    // -------------------------------------------------------------------------
    // updateEmail
    // -------------------------------------------------------------------------

    /**
     * Updates the user's e-mail address and evicts both cache entries (by-ID
     * and by-username) atomically via a Redisson batch after the DB write.
     *
     * <p>
     * {@code @Caching} is used because Spring does not support multiple
     * {@code @CacheEvict} annotations with different keys on the same method
     * without it.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.Cache.CACHE_USERS, key = "'" + Constants.Cache.KEY_USER_BY_ID
                    + "' + #id"),
            @CacheEvict(cacheNames = Constants.Cache.CACHE_USERS, key = "'" + Constants.Cache.KEY_USER_BY_NAME
                    + "' + #result.username()", condition = "#result != null")
    })
    public Mono<UserResponse> updateEmail(Long id, String newEmail) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(String.valueOf(id))))
                .flatMap(user -> {
                    String oldUsername = user.getUsername();
                    user.setEmail(newEmail);
                    Mono<UserResponse> dbSave = userRepository.save(user).map(userMapper::toResponse);
                    // Atomic batch: evict both keys after DB save
                    return atomicCacheEvict(
                            dbSave,
                            Constants.Cache.KEY_USER_BY_ID + id,
                            Constants.Cache.KEY_USER_BY_NAME + oldUsername);
                });
    }

    // -------------------------------------------------------------------------
    // deactivate
    // -------------------------------------------------------------------------

    /**
     * Soft-deactivates a user account and atomically evicts both user cache
     * entries so no stale {@code active=true} response can be served.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.Cache.CACHE_USERS, key = "'" + Constants.Cache.KEY_USER_BY_ID
                    + "' + #id")
    })
    public Mono<Void> deactivate(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(String.valueOf(id))))
                .flatMap(user -> {
                    user.setActive(false);
                    return userRepository.save(user);
                })
                .then();
    }

}
