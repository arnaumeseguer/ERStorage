package com.erstorage.erstorageapi.auth;

import com.erstorage.erstorageapi.common.LimitsProperties;
import com.erstorage.erstorageapi.user.User;
import com.erstorage.erstorageapi.user.UserRepository;
import com.erstorage.erstorageapi.user.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LimitsProperties limits;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       LimitsProperties limits) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.limits = limits;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (!limits.registrationEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration is closed");
        }

        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(email, passwordHash, request.displayName().trim());
        User saved = userRepository.save(user);

        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        String token = tokenService.createToken(user);
        return new LoginResponse(token, tokenService.expirationSeconds(), UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}