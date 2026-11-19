package com.erstorage.erstorageapi.file;

import com.erstorage.erstorageapi.folder.Folder;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    private String name;

    private String storageKey;

    private String contentType;

    private long sizeBytes;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected FileEntity() {
    }

    public FileEntity(UUID ownerId, Folder folder, String name, String storageKey, String contentType, long sizeBytes) {
        this.ownerId = ownerId;
        this.folder = folder;
        this.name = name;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public void moveTo(Folder newFolder) {
        this.folder = newFolder;
    }

    public UUID getId() {return id;}
    public UUID getOwnerId() {return ownerId;}
    public Folder getFolder() {return folder;}
    public String getName() {return name;}
    public String getStorageKey() {return storageKey;}
    public String getContentType() {return contentType;}
    public long getSizeBytes() {return sizeBytes;}
    public Instant getCreatedAt() {return createdAt;}
    public Instant getUpdatedAt() {return updatedAt;}

}
