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


    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update User u
               set u.storageUsedBytes = u.storageUsedBytes + :bytes
             where u.id = :userId
               and u.storageUsedBytes + :bytes <= u.storageQuotaBytes
               and (select coalesce(sum(other.storageUsedBytes), 0) from User other) + :bytes <= :totalLimit
            """)
    int reserveStorage(@Param("userId") UUID userId,
                       @Param("bytes") long bytes,
                       @Param("totalLimit") long totalLimit);

    @Modifying
    @Query("""
            update User u
               set u.storageUsedBytes = u.storageUsedBytes - :bytes
             where u.id = :userId
            """)
    int releaseStorage(@Param("userId") UUID userId, @Param("bytes") long bytes);
}
