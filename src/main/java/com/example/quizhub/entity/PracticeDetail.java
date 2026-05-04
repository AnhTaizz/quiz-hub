package com.example.quizhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "_practice_detail")
public class PracticeDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_id", nullable = false)
    Practice practice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    Question question;

    @jakarta.persistence.ManyToMany(fetch = FetchType.LAZY)
    @jakarta.persistence.JoinTable(
        name = "_practice_detail_selected_answers",
        joinColumns = @jakarta.persistence.JoinColumn(name = "practice_detail_id"),
        inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "answer_id")
    )
    java.util.List<Answer> selectedAnswers;

    @Column(name = "selected_text")
    String selectedText;

    @Column(name = "is_correct")
    Boolean isCorrect;
}
