package com.erstorage.erstorageapi.folder;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "folders")
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    private String name;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected  Folder() {
    }
    public Folder(UUID ownerId, Folder parent, String name) {
        this.ownerId = ownerId;
        this.parent = parent;
        this.name = name;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public void moveTo(Folder newParent) {
        this.parent = newParent;
    }

    public UUID getId() {return id;}
    public UUID getOwnerId() {return ownerId;}
    public Folder getParent() {return parent;}
    public String getName() {return name;}
    public Instant getCreatedAt() {return createdAt;}
    public Instant getUpdatedAt() {return updatedAt;}
}
