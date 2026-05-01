import json
import os

sql = []

# PostgreSQL valid deletes
sql.append("DELETE FROM _answer WHERE question_id >= 90000;")
sql.append("DELETE FROM _question WHERE created_id = 9999;")
sql.append("DELETE FROM categories WHERE creator_id = 9999;")
sql.append("DELETE FROM _user WHERE id IN (9997, 9998, 9999);")

password_hash = "$2a$10$mKPA.JpfPPsx8KUxC4LmSuSSmKgAuiTOhadQFo9YIb.0F82ZzbrWq"

# PostgreSQL Boolean literals are true/false without quotes
sql.append(f"INSERT INTO _user (id, role, email, password, full_name, is_enable, is_verified) VALUES (9999, 'TEACHER', 'teacher@gmail.com', '{password_hash}', 'Teacher JSON', true, true);")
sql.append(f"INSERT INTO _user (id, role, email, password, full_name, is_enable, is_verified) VALUES (9998, 'ADMIN', 'admin@gmail.com', '{password_hash}', 'Admin Quản Trị', true, true);")
sql.append(f"INSERT INTO _user (id, role, email, password, full_name, is_enable, is_verified) VALUES (9997, 'STUDENT', 'student@gmail.com', '{password_hash}', 'Student Học Sinh', true, true);")

global_cat_id = 90000
global_question_id = 90000
global_answer_id = 90000

def process_file(file_path, subject_name):
    global global_cat_id, global_question_id, global_answer_id
    
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
        
    chapters = {}
    for item in data:
        chap = item.get('chapter', 1)
        if chap not in chapters:
            chapters[chap] = []
        chapters[chap].append(item)
        
    parent_cat_id = global_cat_id
    global_cat_id += 1
    sql.append(f"INSERT INTO categories (id, name, creator_id, is_public, parent_id) VALUES ({parent_cat_id}, '{subject_name}', 9999, true, NULL);")
    
    for chap, questions in chapters.items():
        cat_id = global_cat_id
        global_cat_id += 1
        cat_name = f"Chương {chap}"
        
        sql.append(f"INSERT INTO categories (id, name, creator_id, is_public, parent_id) VALUES ({cat_id}, '{cat_name}', 9999, true, {parent_cat_id});")
        
        for q in questions:
            q_text = str(q['question']).replace("'", "''")
            sql.append(f"INSERT INTO _question (id, text, type, category_id, created_id, approval_status) VALUES ({global_question_id}, '{q_text}', 'MULTIPLE_CHOICE', {cat_id}, 9999, 'PRIVATE');")
            
            for ans in q.get('answers', []):
                a_text = str(ans.get('content', '')).replace("'", "''")
                is_correct = "true" if ans.get('is_correct', False) else "false"
                sql.append(f"INSERT INTO _answer (id, text, is_correct, question_id) VALUES ({global_answer_id}, '{a_text}', {is_correct}, {global_question_id});")
                global_answer_id += 1
                
            global_question_id += 1

process_file('d:/quiz-hub/de_trac_nghiem_tu_tuong_ho_chi_minh_theo_chuong.json', 'Môn Tư tưởng Hồ Chí Minh')
process_file('d:/quiz-hub/de_trac_nghiem_mang_may_tinh_so_1_co_giai_thich.json', 'Môn Mạng Máy Tính')
process_file('d:/quiz-hub/de_trac_nghiem_phap_luat_dai_cuong_theo_chuong.json', 'Môn Pháp Luật Đại Cương')

with open('d:/quiz-hub/src/main/resources/data.sql', 'w', encoding='utf-8') as f:
    f.write("\n".join(sql))
print("Generated postgresql compatible mock_data.sql")
