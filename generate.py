import json
import os

with open('d:/quiz-hub/de_trac_nghiem_tu_tuong_ho_chi_minh_theo_chuong.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

chapters = {}
for item in data:
    chap = item.get('chapter', 1)
    if chap not in chapters:
        chapters[chap] = []
    chapters[chap].append(item)

sql = []

# PostgreSQL valid deletes
sql.append("DELETE FROM _answer WHERE question_id >= 90000 AND question_id < 100000;")
sql.append("DELETE FROM _question WHERE created_id = 9999;")
sql.append("DELETE FROM categories WHERE creator_id = 9999;")
sql.append("DELETE FROM _user WHERE id IN (9997, 9998, 9999);")

password_hash = "$2a$10$mKPA.JpfPPsx8KUxC4LmSuSSmKgAuiTOhadQFo9YIb.0F82ZzbrWq"

# PostgreSQL Boolean literals are true/false without quotes
sql.append(f"INSERT INTO _user (id, role, email, password, full_name, is_enable, is_verified) VALUES (9999, 'TEACHER', 'teacher@gmail.com', '{password_hash}', 'Teacher JSON', true, true);")
sql.append(f"INSERT INTO _user (id, role, email, password, full_name, is_enable, is_verified) VALUES (9998, 'ADMIN', 'admin@gmail.com', '{password_hash}', 'Admin Quản Trị', true, true);")
sql.append(f"INSERT INTO _user (id, role, email, password, full_name, is_enable, is_verified) VALUES (9997, 'STUDENT', 'student@gmail.com', '{password_hash}', 'Student Học Sinh', true, true);")

parent_cat_id = 89999
sql.append(f"INSERT INTO categories (id, name, creator_id, is_public, parent_id) VALUES ({parent_cat_id}, 'Môn Tư tưởng Hồ Chí Minh', 9999, false, NULL);")

cat_id_start = 90000
question_id_start = 90000
answer_id_start = 90000

for chap, questions in chapters.items():
    cat_id = cat_id_start + chap
    cat_name = f"Chương {chap}"
    
    sql.append(f"INSERT INTO categories (id, name, creator_id, is_public, parent_id) VALUES ({cat_id}, '{cat_name}', 9999, false, {parent_cat_id});")
    
    for q in questions:
        q_text = str(q['question']).replace("'", "''")
        sql.append(f"INSERT INTO _question (id, text, type, category_id, created_id, approval_status) VALUES ({question_id_start}, '{q_text}', 'MULTIPLE_CHOICE', {cat_id}, 9999, 'PRIVATE');")
        
        for ans in q['answers']:
            a_text = str(ans['content']).replace("'", "''")
            is_correct = "true" if ans['is_correct'] else "false"
            sql.append(f"INSERT INTO _answer (id, text, is_correct, question_id) VALUES ({answer_id_start}, '{a_text}', {is_correct}, {question_id_start});")
            answer_id_start += 1
            
        question_id_start += 1

with open('d:/quiz-hub/src/main/resources/data.sql', 'w', encoding='utf-8') as f:
    f.write("\n".join(sql))
print("Generated postgresql compatible mock_data.sql")
