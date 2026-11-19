package com.erstorage.erstorageapi.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    @Modifying
    @Query("""
            update User u
                set u.storageUsedBytes = u.storageUsedBytes + :bytes
              where u.id = :userId
                and u.storageUsedBytes + :bytes <= u.storageQuotaBytes
            """)
    int reserveStorage(@Param("userId") UUID userId, @Param("bytes") long bytes);

    @Modifying
    @Query("""
            update User u
               set u.storageUsedBytes = u.storageUsedBytes - :bytes
             where u.id = :userId
            """)
    int releaseStorage(@Param("userId") UUID userId, @Param("bytes") long bytes);
}
