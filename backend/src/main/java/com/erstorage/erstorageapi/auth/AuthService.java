package com.erstorage.erstorageapi.auth;

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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request){
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(email, passwordHash, request.displayName().trim());
        User saved = userRepository.save(user);

        return UserResponse.from(saved);
    }
}
