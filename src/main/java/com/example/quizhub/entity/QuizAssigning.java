package com.example.quizhub.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "_quiz_assigning")
public class QuizAssigning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(length = 255)
    String note;

    @Column(name = "max_attempt")
    Integer maxAttempt;

    @Column(name = "question_shuffled")
    Boolean questionShuffled;

    @Column(name = "answer_shuffled")
    Boolean answerShuffled;

    @Column(name = "duration_in_mins")
    Integer durationInMins;

    @Column(name = "start_date")
    LocalDate startDate;

    @Column(name = "due_date")
    LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id")
    Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    Quiz quiz;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
