import sys
import oracledb

# Thiết lập encoding utf-8 cho stdout/stderr để tránh lỗi hiển thị Unicode trên Windows Console
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8')

# Cấu hình kết nối Oracle Database từ application.yml
DB_USER = "DEMO"
DB_PASS = "DEMO"
DB_HOST = "10.10.0.202"
DB_PORT = 1521
DB_SID = "orcl"

def execute_query(sql_query):
    connection = None
    cursor = None
    try:
        # Tạo DSN với SID
        dsn = oracledb.makedsn(DB_HOST, DB_PORT, sid=DB_SID)
        
        # Kết nối ở chế độ Thin mode (không cần cài Oracle Client)
        connection = oracledb.connect(user=DB_USER, password=DB_PASS, dsn=dsn)
        cursor = connection.cursor()
        
        # Chạy câu lệnh SQL
        cursor.execute(sql_query)
        
        # Kiểm tra xem câu lệnh có trả về dữ liệu (SELECT) hay không
        if cursor.description:
            # Lấy tên các cột
            headers = [col[0] for col in cursor.description]
            rows = cursor.fetchall()
            
            if not rows:
                print("Không có bản ghi nào được tìm thấy.")
                return
            
            # Tính độ rộng cột lớn nhất để in đẹp
            col_widths = [len(h) for h in headers]
            for row in rows:
                for idx, val in enumerate(row):
                    val_str = str(val) if val is not None else "NULL"
                    col_widths[idx] = max(col_widths[idx], len(val_str))
            
            # Hàm in dòng phân cách
            separator = "+" + "+".join("-" * (w + 2) for w in col_widths) + "+"
            
            print(separator)
            # In header
            header_str = "|" + "|".join(f" {headers[i].ljust(col_widths[i])} " for i in range(len(headers))) + "|"
            print(header_str)
            print(separator)
            
            # In các dòng dữ liệu
            for row in rows:
                row_cells = []
                for i, val in enumerate(row):
                    val_str = str(val) if val is not None else "NULL"
                    row_cells.append(f" {val_str.ljust(col_widths[i])} ")
                print("|" + "|".join(row_cells) + "|")
                
            print(separator)
            print(f"Tổng số dòng: {len(rows)}")
        else:
            # Các câu lệnh INSERT, UPDATE, DELETE, COMMIT...
            connection.commit()
            print(f"Thực hiện thành công. Số dòng bị ảnh hưởng: {cursor.rowcount}")
            
    except Exception as e:
        print(f"Lỗi thực thi SQL: {e}", file=sys.stderr)
    finally:
        if cursor:
            cursor.close()
        if connection:
            connection.close()

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Sử dụng: python query_db.py \"<CÂU_LỆNH_SQL>\"")
        sys.exit(1)
        
    query = sys.argv[1]
    print(f"Đang thực thi: {query}\n")
    execute_query(query)
