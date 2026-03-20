package com.example.quizhub.entity;

import com.example.quizhub.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "_user")
public class UserDtls {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Role role;

    @Column(nullable = false, unique = true, length = 100)
    String email;

    @Column(nullable = false)
    String password;

    @Column(name = "full_name", nullable = false)
    String fullName;

    @Column(name = "is_enable", nullable = false, columnDefinition = "boolean default false")
    Boolean isEnable;

    @Column(name = "is_verified", nullable = false, columnDefinition = "boolean default false")
    Boolean isVerified;

    @Column(length = 15)
    String phone;

    @Column(name = "avatar_url", length = 500)
    String avatarUrl;

    @Column(length = 255)
    String note;

    @Column(name = "created_id")
    Long createdId;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
