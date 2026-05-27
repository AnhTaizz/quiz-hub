package com.example.quizhub.service.student.impl;

import com.example.quizhub.dto.student.QuizDashboardInfoDTO;
import com.example.quizhub.dto.student.StudentHomeDashboardDTO;
import com.example.quizhub.entity.*;
import com.example.quizhub.entity.enums.JoinStatus;
import com.example.quizhub.repository.*;
import com.example.quizhub.service.student.StudentHomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentHomeServiceImpl implements StudentHomeService {

    private final AttemptRepository attemptRepository;
    private final PracticeRepository practiceRepository;
    private final QuizTakingRepository quizTakingRepository;
    private final UserRepository userRepository;
    private final ClassJoiningRepository classJoiningRepository;
    private final QuizAssigningRepository quizAssigningRepository;

    private boolean isStudentAllowed(QuizAssigning a, Long studentId) {
        String ids = a.getAssignedStudentIds();
        if (ids == null || ids.isBlank()) return true;
        return Arrays.asList(ids.split(",")).contains(String.valueOf(studentId));
    }

    @Override
    public User getStudentByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public StudentHomeDashboardDTO getDashboardData(String email) {
        User student = userRepository.findByEmail(email).orElse(null);
        if (student == null) return null;

        List<Attempt> allCompleted = attemptRepository
                .findByQuizTakingLearnerIdAndEndedAtIsNotNull(student.getId());
        List<Attempt> classroomAttempts = allCompleted.stream()
                .filter(a -> a.getQuizTaking() != null && a.getQuizTaking().getQuizAssigning() != null)
                .collect(Collectors.toList());

        long practiceCount = practiceRepository.countByUserIdAndIsCompletedTrue(student.getId());
        long totalCompleted = classroomAttempts.size() + practiceCount;

        List<Practice> completedPractices = practiceRepository
                .findByUserIdAndIsCompletedTrueOrderByCreatedAtDesc(student.getId());

        BigDecimal quizAvg = BigDecimal.ZERO;
        if (!classroomAttempts.isEmpty()) {
            BigDecimal sum = classroomAttempts.stream()
                    .map(a -> a.getResult() != null ? a.getResult() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            quizAvg = sum.divide(BigDecimal.valueOf(classroomAttempts.size()), 1, RoundingMode.HALF_UP);
        }

        BigDecimal practiceAvg = BigDecimal.ZERO;
        if (!completedPractices.isEmpty()) {
            BigDecimal sum = completedPractices.stream()
                    .map(p -> {
                        if (p.getTotalQuestions() != null && p.getTotalQuestions() > 0)
                            return BigDecimal.valueOf((p.getCorrectAnswers() * 10.0) / p.getTotalQuestions());
                        return BigDecimal.ZERO;
                    }).reduce(BigDecimal.ZERO, BigDecimal::add);
            practiceAvg = sum.divide(BigDecimal.valueOf(completedPractices.size()), 1, RoundingMode.HALF_UP);
        }

        List<ClassJoining> joinedClasses = classJoiningRepository
                .findByLearnerIdAndStatusIn(student.getId(), List.of(JoinStatus.APPROVED, JoinStatus.PENDING))
                .stream().filter(j -> j.getClassroom() != null).collect(Collectors.toList());

        List<QuizAssigning> rawQuizzes = new ArrayList<>();
        for (ClassJoining j : joinedClasses) {
            if (j.getStatus() == JoinStatus.APPROVED && j.getClassroom() != null)
                rawQuizzes.addAll(quizAssigningRepository.findByClassroomId(j.getClassroom().getId()));
        }

        List<QuizDashboardInfoDTO> dashboardQuizzes = new ArrayList<>();
        int pendingThisWeekCount = 0;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextWeek = now.plusDays(7);

        for (QuizAssigning a : rawQuizzes) {
            if (Boolean.TRUE.equals(a.getIsHidden())) continue;
            if (!isStudentAllowed(a, student.getId())) continue;

            QuizTaking taking = quizTakingRepository
                    .findByLearnerIdAndQuizAssigningId(student.getId(), a.getId()).orElse(null);
            int finishedCount = 0;
            boolean anyAttempt = false, hasUnfinished = false;
            if (taking != null) {
                List<Attempt> attempts = attemptRepository.findByQuizTakingId(taking.getId());
                finishedCount = (int) attempts.stream().filter(at -> at.getEndedAt() != null).count();
                anyAttempt = !attempts.isEmpty();
                hasUnfinished = attempts.stream().anyMatch(at -> at.getEndedAt() == null);
            }
            int max = a.getMaxAttempt() != null ? a.getMaxAttempt() : 0;
            boolean hasLeft = (max == 0) || (finishedCount < max);
            boolean isExpired = a.getDueDate() != null && now.isAfter(a.getDueDate());
            boolean isUpcoming = a.getStartDate() != null && now.isBefore(a.getStartDate());

            if (hasLeft && !isExpired && !isUpcoming) {
                QuizDashboardInfoDTO info = new QuizDashboardInfoDTO();
                info.setAssigning(a);
                info.setAttemptsMade(finishedCount);
                info.setAttemptsLeft(max == 0 ? -1 : (max - finishedCount));
                info.setHasStarted(anyAttempt);
                info.setHasUnfinished(hasUnfinished);
                dashboardQuizzes.add(info);
                if (a.getDueDate() != null && a.getDueDate().isBefore(nextWeek)) pendingThisWeekCount++;
            }
        }

        return StudentHomeDashboardDTO.builder()
                .totalCompleted(totalCompleted).quizAvg(quizAvg).practiceAvg(practiceAvg)
                .assignedQuizzes(dashboardQuizzes).pendingCount(dashboardQuizzes.size())
                .pendingThisWeekCount(pendingThisWeekCount).build();
    }

    @Override
    public List<QuizDashboardInfoDTO> getAllQuizzes(String email) {
        User student = userRepository.findByEmail(email).orElse(null);
        if (student == null) return new ArrayList<>();

        List<ClassJoining> joinedClasses = classJoiningRepository
                .findByLearnerIdAndStatusIn(student.getId(), List.of(JoinStatus.APPROVED, JoinStatus.PENDING))
                .stream().filter(j -> j.getClassroom() != null).collect(Collectors.toList());

        Map<Long, QuizDashboardInfoDTO> quizMap = new LinkedHashMap<>();
        for (ClassJoining j : joinedClasses) {
            if (j.getStatus() != JoinStatus.APPROVED || j.getClassroom() == null) continue;
            List<QuizAssigning> classQuizzes = quizAssigningRepository.findByClassroomId(j.getClassroom().getId());
            if (classQuizzes == null) continue;
            for (QuizAssigning a : classQuizzes) {
                if (a == null || a.getQuiz() == null || Boolean.TRUE.equals(a.getIsHidden())) continue;
                if (!isStudentAllowed(a, student.getId())) continue;
                if (quizMap.containsKey(a.getId())) continue;

                QuizTaking taking = quizTakingRepository
                        .findByLearnerIdAndQuizAssigningId(student.getId(), a.getId()).orElse(null);
                int finishedCount = 0;
                boolean anyAttempt = false, hasUnfinished = false;
                if (taking != null) {
                    List<Attempt> attempts = attemptRepository.findByQuizTakingId(taking.getId());
                    if (attempts != null) {
                        finishedCount = (int) attempts.stream().filter(at -> at.getEndedAt() != null).count();
                        anyAttempt = !attempts.isEmpty();
                        hasUnfinished = attempts.stream().anyMatch(at -> at.getEndedAt() == null);
                    }
                }
                QuizDashboardInfoDTO info = new QuizDashboardInfoDTO();
                info.setAssigning(a);
                info.setAttemptsMade(finishedCount);
                info.setAttemptsLeft(a.getMaxAttempt() == null || a.getMaxAttempt() == 0 ? -1
                        : Math.max(0, a.getMaxAttempt() - finishedCount));
                info.setHasStarted(anyAttempt);
                info.setHasUnfinished(hasUnfinished);
                quizMap.put(a.getId(), info);
            }
        }

        List<QuizDashboardInfoDTO> allQuizzes = new ArrayList<>(quizMap.values());
        allQuizzes.sort((a, b) -> {
            LocalDateTime d1 = a.getAssigning().getDueDate();
            LocalDateTime d2 = b.getAssigning().getDueDate();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d1.compareTo(d2);
        });
        return allQuizzes;
    }

    @Override
    public List<ClassJoining> getApprovedClasses(String email) {
        User student = userRepository.findByEmail(email).orElse(null);
        if (student == null) return new ArrayList<>();
        return classJoiningRepository
                .findByLearnerIdAndStatusIn(student.getId(), List.of(JoinStatus.APPROVED, JoinStatus.PENDING))
                .stream().filter(j -> j.getClassroom() != null && j.getStatus() == JoinStatus.APPROVED)
                .collect(Collectors.toList());
    }

    @Override
    public QuizAssigning getQuizAssigningById(Long assigningId) {
        return quizAssigningRepository.findByIdIncludingDeleted(assigningId).orElseThrow();
    }

    @Override
    public List<Attempt> getQuizHistory(Long assigningId, String email) {
        User student = userRepository.findByEmail(email).orElseThrow();
        QuizTaking taking = quizTakingRepository
                .findByLearnerIdAndQuizAssigningId(student.getId(), assigningId).orElse(null);
        if (taking != null) return attemptRepository.findByQuizTakingIdOrderByStartedAtDesc(taking.getId());
        return new ArrayList<>();
    }

    @Override
    public List<Attempt> getAllQuizAttempts(String email) {
        User student = userRepository.findByEmail(email).orElseThrow();
        return attemptRepository.findByQuizTakingLearnerIdOrderByStartedAtDesc(student.getId());
    }
}
