-- [TEST DATA] Câu hỏi Nhiều lựa chọn
INSERT INTO _question (id, text, type, category_id, created_id, approval_status, question_level) 
VALUES (99990, 'Các thành phần nào sau đây thuộc QuizHub (Chọn nhiều)?', 'MULTIPLE_CHOICE', 90001, 9999, 'PUBLIC', 'EASY');
INSERT INTO _answer (id, text, is_correct, question_id) VALUES (999900, 'Giao diện người dùng (Frontend)', true, 99990);
INSERT INTO _answer (id, text, is_correct, question_id) VALUES (999901, 'Hệ thống máy chủ (Backend)', true, 99990);
INSERT INTO _answer (id, text, is_correct, question_id) VALUES (999902, 'Cơ sở dữ liệu (Database)', true, 99990);
INSERT INTO _answer (id, text, is_correct, question_id) VALUES (999903, 'Người máy vận hành (Robot)', false, 99990);

-- [TEST DATA] Câu hỏi Điền khuyết
INSERT INTO _question (id, text, type, category_id, created_id, approval_status, question_level) 
VALUES (99991, 'Thủ đô của nước Cộng hòa Xã hội Chủ nghĩa Việt Nam là gì?', 'FILL_IN_BLANK', 90001, 9999, 'PUBLIC', 'EASY');
INSERT INTO _answer (id, text, is_correct, question_id) VALUES (999910, 'Hà Nội', true, 99991);
INSERT INTO _answer (id, text, is_correct, question_id) VALUES (999911, 'Ha Noi', true, 99991);

-- [TEST DATA] Tạo Quiz mới
INSERT INTO _quiz (id, title, description, is_draft, is_enable, is_exam, created_id, created_at, updated_at) 
VALUES ('00000000-0000-0000-0000-000000000099', 'Bài Test Các Loại Câu Hỏi', 'Kiểm tra trắc nghiệm nhiều đáp án và điền khuyết', false, true, false, 9999, NOW(), NOW());

-- [TEST DATA] Gắn câu hỏi vào Quiz
INSERT INTO _question_creating (quiz_id, quest_id) VALUES ('00000000-0000-0000-0000-000000000099', 99990);
INSERT INTO _question_creating (quiz_id, quest_id) VALUES ('00000000-0000-0000-0000-000000000099', 99991);

-- [TEST DATA] Giao bài cho lớp Tư tưởng Hồ Chí Minh (90000)
INSERT INTO _quiz_assigning (classroom_id, quiz_id, note, max_attempt, question_shuffled, answer_shuffled, duration_in_mins, start_date, due_date, created_at, topic_id) 
VALUES (90000, '00000000-0000-0000-0000-000000000099', 'Lưu ý: Chọn đầy đủ đáp án đúng cho câu nhiều lựa chọn', 5, false, false, 15, '2026-05-01 00:00:00', '2026-12-31 23:59:59', NOW(), 90100);
