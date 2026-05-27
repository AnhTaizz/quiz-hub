package com.example.quizhub.service.teacher;

import com.example.quizhub.entity.*;
import java.util.List;
import java.util.Map;

public interface TeacherClassroomWebService {
    List<Classroom> getTeacherClassrooms(Long teacherId);
    Map<Long, Long> getActiveMemberCounts(List<Classroom> classrooms);
    Classroom getClassroomByIdAndTeacher(Long classroomId, Long teacherId);
    List<ClassJoining> getActiveMembers(Long classroomId);
    List<ClassJoining> getPendingMembers(Long classroomId);
    List<QuizAssigning> getAssignedQuizzes(Long classroomId);
    List<Quiz> getTeacherQuizzes(Long teacherId);
    List<ClassTopic> getClassTopics(Long classroomId);
    List<Category> getTeacherCategories(Long teacherId);
}
