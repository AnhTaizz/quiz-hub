package com.example.quizhub.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "exam_violation")
public class ExamViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    /**
     * Mã vi phạm duy nhất, VD: 'TAB_SWITCH', 'ESC_FULLSCREEN'
     */
    @Column(name = "violation_code", nullable = false, unique = true, length = 50)
    String violationCode;

    /**
     * Mức độ vi phạm: 1 = Nhẹ, 2 = Cảnh cáo, 3 = Nặng
     */
    @Column(name = "severity_level", columnDefinition = "int default 1")
    Integer severityLevel;

    @Column(columnDefinition = "TEXT")
    String description;
}
