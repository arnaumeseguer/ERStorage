package com.erstorage.erstorageapi.demo;

import com.erstorage.erstorageapi.auth.IssuedToken;
import com.erstorage.erstorageapi.auth.LoginResponse;
import com.erstorage.erstorageapi.auth.TokenService;
import com.erstorage.erstorageapi.file.FileRepository;
import com.erstorage.erstorageapi.folder.Folder;
import com.erstorage.erstorageapi.folder.FolderRepository;
import com.erstorage.erstorageapi.user.Role;
import com.erstorage.erstorageapi.user.User;
import com.erstorage.erstorageapi.user.UserRepository;
import com.erstorage.erstorageapi.user.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DemoService {

    private static final List<String> SAMPLE_FOLDERS = List.of("Documents", "Photos", "Projects");

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final DemoRateLimiter rateLimiter;
    private final DemoProperties demo;

    public DemoService(UserRepository userRepository,
                       FolderRepository folderRepository,
                       FileRepository fileRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       DemoRateLimiter rateLimiter,
                       DemoProperties demo) {
        this.userRepository = userRepository;
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.rateLimiter = rateLimiter;
        this.demo = demo;
    }

    @Transactional
    public LoginResponse start(String clientKey) {
        if (!demo.enabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The demo is not available right now");
        }

        Instant now = Instant.now();
        if (userRepository.countByRoleAndExpiresAtAfter(Role.GUEST, now) >= demo.maxActiveUsers()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The demo is full right now. Please try again in a few minutes.");
        }
        if (!rateLimiter.tryAcquire(clientKey)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many demo sessions from your network. Please try again later.");
        }

        User guest = User.guest(
                "demo-" + UUID.randomUUID() + "@demo.erstorage.invalid",
                passwordEncoder.encode(UUID.randomUUID().toString()),
                "Guest",
                demo.quota().toBytes(),
                now.plus(demo.lifetime()));
        User saved = userRepository.save(guest);

        for (String name : SAMPLE_FOLDERS) {
            folderRepository.save(new Folder(saved.getId(), null, name));
        }

        IssuedToken token = tokenService.createToken(saved);
        return new LoginResponse(token.value(), token.expiresInSeconds(), UserResponse.from(saved));
    }

    @Transactional
    public List<String> deleteExpiredGuests() {
        List<UUID> expired = userRepository.findExpiredIds(Role.GUEST, Instant.now());
        if (expired.isEmpty()) {
            return List.of();
        }

        List<String> storageKeys = fileRepository.findStorageKeysByOwnerIds(expired);
        userRepository.deleteAllByIdInBatch(expired);
        return storageKeys;
    }
}