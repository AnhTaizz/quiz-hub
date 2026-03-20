package com.example.quizhub.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "_classroom")
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 8)
    String code;

    @Column(nullable = false, length = 64)
    String name;

    @Column(name = "is_enable", nullable = false)
    Boolean isEnable;

    @Column(length = 128)
    String description;

    @Column(name = "imageurl", length = 255)
    String imageUrl;

    @Column(name = "is_draft")
    Boolean isDraft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_id", referencedColumnName = "id")
    User creator;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
