import json
import os

sql = []

# PostgreSQL valid deletes
sql.append("DELETE FROM class_topics WHERE classroom_id >= 90000;")
sql.append("DELETE FROM _class_joining WHERE class_id >= 90000;")
sql.append("DELETE FROM _quiz_assigning WHERE classroom_id >= 90000;")
sql.append("DELETE FROM _question_creating WHERE quiz_id = '00000000-0000-0000-0000-000000000001';")
sql.append("DELETE FROM _quiz WHERE id = '00000000-0000-0000-0000-000000000001';")
sql.append("DELETE FROM _classroom WHERE id >= 90000;")
sql.append("DELETE FROM _answer WHERE question_id >= 90000;")
sql.append("DELETE FROM _question WHERE created_id IN (9990, 9991, 9992, 9993, 9997, 9998, 9999);")
sql.append("DELETE FROM categories WHERE creator_id IN (9990, 9991, 9992, 9993, 9997, 9998, 9999);")
sql.append("DELETE FROM _user WHERE id IN (9990, 9991, 9992, 9993, 9997, 9998, 9999);")

password_hash = "$2a$10$mKPA.JpfPPsx8KUxC4LmSuSSmKgAuiTOhadQFo9YIb.0F82ZzbrWq"

# PostgreSQL Boolean literals are true/false without quotes
sql.append(f"INSERT INTO _user (id, role, email, password, full_name, is_enable, is_verified) VALUES (9999, 'TEACHER', 'teacher@gmail.com', '{password_hash}', 'Teacher JSON', true, true);")
sql.append(f"INSERT INTO _user (id, role, email, password, full_name, is_enable, is_verified) VALUES (9998, 'ADMIN', 'admin@gmail.com', '{password_hash}', 'Admin Quản Trị', true, true);")
sql.append(f"INSERT INTO _user (id, role, email, password, full_name, is_enable, is_verified) VALUES (9997, 'STUDENT', 'student@gmail.com', '{password_hash}', 'Student Học Sinh', true, true);")
sql.append(f"INSERT INTO _user (id, role, email, password, full_name, is_enable, is_verified) VALUES (9990, 'STUDENT', 'student1@gmail.com', '{password_hash}', 'Nguyễn Văn Một', true, true);")
sql.append(f"INSERT INTO _user (id, role, email, password, full_name, is_enable, is_verified) VALUES (9991, 'STUDENT', 'student2@gmail.com', '{password_hash}', 'Trần Thị Hai', true, true);")
sql.append(f"INSERT INTO _user (id, role, email, password, full_name, is_enable, is_verified) VALUES (9992, 'STUDENT', 'student3@gmail.com', '{password_hash}', 'Lê Văn Ba', true, true);")
sql.append(f"INSERT INTO _user (id, role, email, password, full_name, is_enable, is_verified) VALUES (9993, 'STUDENT', 'student4@gmail.com', '{password_hash}', 'Phạm Thị Bốn', true, true);")

global_cat_id = 90000
global_question_id = 90000
global_answer_id = 90000

def process_file(file_path, subject_name, creator_id=9999, is_public=False, chapter_prefix="Chương", chapter_offset=0, parent_id=None):
    global global_cat_id, global_question_id, global_answer_id
    
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
        
    chapters = {}
    for item in data:
        chap = item.get('chapter', 1) + chapter_offset
        if chap not in chapters:
            chapters[chap] = []
        chapters[chap].append(item)
        
    public_val = "true" if is_public else "false"
    if parent_id is None:
        parent_cat_id = global_cat_id
        global_cat_id += 1
        sql.append(f"INSERT INTO categories (id, name, creator_id, is_public, parent_id) VALUES ({parent_cat_id}, '{subject_name}', {creator_id}, {public_val}, NULL);")
    else:
        parent_cat_id = parent_id
    
    for chap, questions in chapters.items():
        cat_id = global_cat_id
        global_cat_id += 1
        cat_name = f"{chapter_prefix} {chap}"
        
        sql.append(f"INSERT INTO categories (id, name, creator_id, is_public, parent_id) VALUES ({cat_id}, '{cat_name}', {creator_id}, {public_val}, {parent_cat_id});")
        
        for q in questions:
            q_text = str(q['question']).replace("'", "''")
            
            # Determine question type: SINGLE_CHOICE if 1 or 0 correct answers, MULTIPLE_CHOICE if > 1
            correct_count = sum(1 for ans in q.get('answers', []) if ans.get('is_correct', False))
            q_type = 'SINGLE_CHOICE' if correct_count <= 1 else 'MULTIPLE_CHOICE'
            
            status = 'PUBLIC' if is_public else 'PRIVATE'
            sql.append(f"INSERT INTO _question (id, text, type, category_id, created_id, approval_status, question_level) VALUES ({global_question_id}, '{q_text}', '{q_type}', {cat_id}, {creator_id}, '{status}', 'MEDIUM');")
            
            for ans in q.get('answers', []):
                a_text = str(ans.get('content', '')).replace("'", "''")
                is_correct = "true" if ans.get('is_correct', False) else "false"
                sql.append(f"INSERT INTO _answer (id, text, is_correct, question_id) VALUES ({global_answer_id}, '{a_text}', {is_correct}, {global_question_id});")
                global_answer_id += 1
                
            global_question_id += 1
            
    return parent_cat_id

process_file('d:/quiz-hub/de_trac_nghiem_tu_tuong_ho_chi_minh_theo_chuong.json', 'Môn Tư tưởng Hồ Chí Minh')
process_file('d:/quiz-hub/de_trac_nghiem_mang_may_tinh_so_1_co_giai_thich.json', 'Môn Mạng Máy Tính')
process_file('d:/quiz-hub/de_trac_nghiem_phap_luat_dai_cuong_theo_chuong.json', 'Môn Pháp Luật Đại Cương')
security_parent_id = process_file('d:/quiz-hub/de_trac_nghiem_an_toan_bao_mat_he_thong_thong_tin_so_1.json', 'An toàn bảo mật hệ thống thông tin', creator_id=9998, is_public=True, chapter_prefix="Đề số")
process_file('d:/quiz-hub/de_trac_nghiem_an_toan_bao_mat_he_thong_thong_tin_so_2.json', 'An toàn bảo mật hệ thống thông tin', creator_id=9998, is_public=True, chapter_prefix="Đề số", chapter_offset=1, parent_id=security_parent_id)
process_file('d:/quiz-hub/de_trac_nghiem_co_so_du_lieu_so_1.json', 'Cơ sở dữ liệu', creator_id=9998, is_public=True, chapter_prefix="Đề số")
process_file('d:/quiz-hub/de_trac_nghiem_an_toan_web_va_csdl_so_1.json', 'An toàn Web và Cơ sở dữ liệu', creator_id=9998, is_public=True, chapter_prefix="Đề số")
process_file('d:/quiz-hub/de_trac_nghiem_phuong_phap_luan_nghien_cuu_khoa_hoc_so_1.json', 'Phương pháp luận nghiên cứu khoa học', creator_id=9998, is_public=True, chapter_prefix="Đề số")

# --- Sample Classrooms & Quiz Data ---
# 1. Create Classrooms for each subject the teacher created
classrooms = [
    (90000, 'HCM001', 'Lớp học Tư tưởng Hồ Chí Minh'),
    (90001, 'MMT001', 'Lớp học Mạng Máy Tính'),
    (90002, 'PLD001', 'Lớp học Pháp Luật Đại Cương')
]

for c_id, c_code, c_name in classrooms:
    sql.append(f"INSERT INTO _classroom (id, code, name, is_enable, description, is_draft, created_id, require_approval, created_at) VALUES ({c_id}, '{c_code}', '{c_name}', true, 'Lớp học mẫu cho {c_name}', false, 9999, false, NOW());")
    # Add topics for each classroom
    sql.append(f"INSERT INTO class_topics (id, name, classroom_id) VALUES ({c_id + 100}, 'Kiểm tra & Bài tập', {c_id});")
    sql.append(f"INSERT INTO class_topics (id, name, classroom_id) VALUES ({c_id + 200}, 'Tài liệu tham khảo', {c_id});")

# 2. Create a Sample Quiz for one of the classrooms (e.g., Mạng Máy Tính)
quiz_id = '00000000-0000-0000-0000-000000000001'
sql.append(f"INSERT INTO _quiz (id, title, description, is_draft, is_enable, is_exam, created_id, created_at, updated_at) VALUES ('{quiz_id}', 'Kiểm tra Mạng Máy Tính', 'Bài kiểm tra kiến thức Mạng Máy Tính', false, true, false, 9999, NOW(), NOW());")

# 3. Add 10 questions to the Quiz from the "Mạng Máy Tính" category (cat_id starts from 90000)
# Let's find the start of MMT questions. HCM (90000), then subcats. 
# It's easier to just pick some IDs we know exist.
for i in range(10):
    q_id = 90280 + i  # MMT questions start after HCM (280 questions)
    sql.append(f"INSERT INTO _question_creating (quiz_id, quest_id) VALUES ('{quiz_id}', {q_id});")

# 4. Assign Quiz to the Mạng Máy Tính classroom (90001) under "Kiểm tra & Bài tập" topic (90101)
sql.append(f"INSERT INTO _quiz_assigning (classroom_id, quiz_id, note, max_attempt, question_shuffled, answer_shuffled, duration_in_mins, start_date, due_date, created_at, topic_id) VALUES (90001, '{quiz_id}', 'Làm bài trong 60 phút', 3, true, true, 60, '2024-05-01', '2024-12-31', NOW(), 90101);")

# 5. Enroll 5 Students in all classrooms
student_ids = [9997, 9990, 9991, 9992, 9993]
for c_id, _, _ in classrooms:
    for s_id in student_ids:
        sql.append(f"INSERT INTO _class_joining (class_id, learner_id, status, _displayed_name, joined_at) VALUES ({c_id}, {s_id}, 'APPROVED', (SELECT full_name FROM _user WHERE id={s_id}), NOW());")

with open('d:/quiz-hub/src/main/resources/data.sql', 'w', encoding='utf-8') as f:
    f.write("\n".join(sql))
print("Generated postgresql compatible mock_data.sql")
