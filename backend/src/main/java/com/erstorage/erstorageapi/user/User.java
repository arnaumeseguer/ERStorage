package com.erstorage.erstorageapi.user;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;

    private String passwordHash;

    private String displayName;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    private long storageUsedBytes = 0;

    private long storageQuotaBytes = 1_073_741_824L; //1GB

    private Instant expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    protected User() {}

    public User(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public static User guest(String email, String passwordHash, String displayName,
                             long quotaBytes, Instant expiresAt) {
        User user = new User(email, passwordHash, displayName);
        user.role = Role.GUEST;
        user.storageQuotaBytes = quotaBytes;
        user.expiresAt = expiresAt;
        return user;
    }

    public boolean isGuest() {return role == Role.GUEST;}

    public UUID getId() {return id;}
    public String getEmail() {return email;}
    public String getPasswordHash() {return passwordHash;}
    public String getDisplayName() {return displayName;}
    public Role getRole() {return role;}
    public long getStorageUsedBytes() {return storageUsedBytes;}
    public long getStorageQuotaBytes() {return storageQuotaBytes;}
    public Instant getExpiresAt() {return expiresAt;}
    public Instant getCreatedAt() {return createdAt;}
}