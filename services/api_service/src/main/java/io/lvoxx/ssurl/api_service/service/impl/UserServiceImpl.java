package io.lvoxx.ssurl.api_service.service.impl;

import io.lvoxx.ssurl.api_service.repository.UserRepository;
import io.lvoxx.ssurl.api_service.service.UserService;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.lvoxx.ssurl.common.exception.UserNotFoundException;
import io.lvoxx.ssurl.common.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<UserResponse> getById(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(String.valueOf(id))))
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<UserResponse> getByUsername(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UserNotFoundException(username)))
                .map(userMapper::toResponse);
    }

    @Override
    public Mono<UserResponse> updateEmail(Long id, String newEmail) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(String.valueOf(id))))
                .flatMap(user -> {
                    user.setEmail(newEmail);
                    return userRepository.save(user);
                })
                .map(userMapper::toResponse);
    }

    @Override
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
