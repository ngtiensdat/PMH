-- ====================================================================
-- V10: CẬP NHẬT STORED PROCEDURE DUYỆT & TỪ CHỐI AN TOÀN CHO PMH
-- ====================================================================

-- 1. Stored Procedure duyệt Tham số danh mục (Có guard WHERE STATUS = 3)
CREATE OR REPLACE PROCEDURE PROC_APPROVE_GROUP_CATEGORY (
    p_id IN NUMBER,
    p_user IN VARCHAR2,
    p_status OUT NUMBER,
    p_message OUT VARCHAR2
) AS
BEGIN
    UPDATE PMH_GROUP_CATEGORY
    SET STATUS = 4, -- Đã duyệt
        IS_DISPLAY = 2, -- Đã duyệt không cho phép xóa
        IS_ACTIVE = CASE 
            WHEN EFFECTIVE_DATE <= SYSDATE AND (END_EFFECTIVE_DATE IS NULL OR END_EFFECTIVE_DATE >= SYSDATE) THEN 1 
            ELSE 0 
        END,
        UPDATED_BY = p_user,
        UPDATED_DATE = SYSDATE
    WHERE ID = p_id AND STATUS = 3;
    
    IF SQL%ROWCOUNT > 0 THEN
        p_status := 1;
        p_message := 'Duyệt thành công';
    ELSE
        p_status := 0;
        p_message := 'Không tìm thấy bản ghi hoặc bản ghi không ở trạng thái Chờ duyệt';
    END IF;
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        p_status := -1;
        p_message := SQLERRM;
END;
/

-- 2. Stored Procedure từ chối Tham số danh mục (Phân nhánh theo IS_DISPLAY)
CREATE OR REPLACE PROCEDURE PROC_REJECT_GROUP_CATEGORY (
    p_id IN NUMBER,
    p_user IN VARCHAR2,
    p_status OUT NUMBER,
    p_message OUT VARCHAR2
) AS
    v_is_display NUMBER;
BEGIN
    SELECT IS_DISPLAY INTO v_is_display 
    FROM PMH_GROUP_CATEGORY 
    WHERE ID = p_id AND STATUS = 3;
    
    IF v_is_display = 2 THEN
        -- Bản ghi đã từng duyệt (Checker từ chối đề xuất sửa đổi trong NEW_DATA): Khôi phục về Đã duyệt (4), xóa NEW_DATA
        UPDATE PMH_GROUP_CATEGORY
        SET STATUS = 4,
            NEW_DATA = NULL,
            IS_ACTIVE = CASE 
                WHEN EFFECTIVE_DATE <= SYSDATE AND (END_EFFECTIVE_DATE IS NULL OR END_EFFECTIVE_DATE >= SYSDATE) THEN 1 
                ELSE 0 
            END,
            UPDATED_BY = p_user,
            UPDATED_DATE = SYSDATE
        WHERE ID = p_id AND STATUS = 3;
    ELSE
        -- Bản ghi chưa từng duyệt (Tạo mới bị từ chối): Chuyển sang Từ chối (5), IS_ACTIVE = 0
        UPDATE PMH_GROUP_CATEGORY
        SET STATUS = 5,
            IS_ACTIVE = 0,
            UPDATED_BY = p_user,
            UPDATED_DATE = SYSDATE
        WHERE ID = p_id AND STATUS = 3;
    END IF;
    
    IF SQL%ROWCOUNT > 0 THEN
        p_status := 1;
        p_message := 'Từ chối duyệt thành công';
    ELSE
        p_status := 0;
        p_message := 'Không tìm thấy bản ghi hoặc bản ghi không ở trạng thái Chờ duyệt';
    END IF;
    COMMIT;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_status := 0;
        p_message := 'Không tìm thấy bản ghi hoặc bản ghi không ở trạng thái Chờ duyệt';
    WHEN OTHERS THEN
        ROLLBACK;
        p_status := -1;
        p_message := SQLERRM;
END;
/

-- 3. Stored Procedure duyệt Cấu phần xử lý
CREATE OR REPLACE PROCEDURE PROC_APPROVE_COMPONENT (
    p_code IN VARCHAR2,
    p_user IN VARCHAR2,
    p_status OUT NUMBER,
    p_message OUT VARCHAR2
) AS
BEGIN
    UPDATE PMH_COMPONENTS
    SET STATUS = 4,
        IS_DISPLAY = 2,
        IS_ACTIVE = CASE 
            WHEN EFFECTIVE_DATE <= SYSDATE AND (END_EFFECTIVE_DATE IS NULL OR END_EFFECTIVE_DATE >= SYSDATE) THEN 1 
            ELSE 0 
        END,
        UPDATED_BY = p_user,
        UPDATED_DATE = SYSDATE
    WHERE COMPONENT_CODE = p_code AND STATUS = 3;

    IF SQL%ROWCOUNT > 0 THEN
        p_status := 1;
        p_message := 'Duyệt thành công';
    ELSE
        p_status := 0;
        p_message := 'Không tìm thấy bản ghi hoặc bản ghi không ở trạng thái Chờ duyệt';
    END IF;
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        p_status := -1;
        p_message := SQLERRM;
END;
/

-- 4. Stored Procedure từ chối Cấu phần xử lý
CREATE OR REPLACE PROCEDURE PROC_REJECT_COMPONENT (
    p_code IN VARCHAR2,
    p_user IN VARCHAR2,
    p_status OUT NUMBER,
    p_message OUT VARCHAR2
) AS
    v_is_display NUMBER;
BEGIN
    SELECT IS_DISPLAY INTO v_is_display 
    FROM PMH_COMPONENTS 
    WHERE COMPONENT_CODE = p_code AND STATUS = 3;

    IF v_is_display = 2 THEN
        -- Cấu phần đã từng duyệt: Khôi phục về Đã duyệt (4), xóa NEW_DATA
        UPDATE PMH_COMPONENTS
        SET STATUS = 4,
            NEW_DATA = NULL,
            IS_ACTIVE = CASE 
                WHEN EFFECTIVE_DATE <= SYSDATE AND (END_EFFECTIVE_DATE IS NULL OR END_EFFECTIVE_DATE >= SYSDATE) THEN 1 
                ELSE 0 
            END,
            UPDATED_BY = p_user,
            UPDATED_DATE = SYSDATE
        WHERE COMPONENT_CODE = p_code AND STATUS = 3;
    ELSE
        -- Cấu phần chưa từng duyệt: Chuyển sang Từ chối (5), IS_ACTIVE = 0
        UPDATE PMH_COMPONENTS
        SET STATUS = 5,
            IS_ACTIVE = 0,
            UPDATED_BY = p_user,
            UPDATED_DATE = SYSDATE
        WHERE COMPONENT_CODE = p_code AND STATUS = 3;
    END IF;

    IF SQL%ROWCOUNT > 0 THEN
        p_status := 1;
        p_message := 'Từ chối duyệt thành công';
    ELSE
        p_status := 0;
        p_message := 'Không tìm thấy bản ghi hoặc bản ghi không ở trạng thái Chờ duyệt';
    END IF;
    COMMIT;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_status := 0;
        p_message := 'Không tìm thấy bản ghi hoặc bản ghi không ở trạng thái Chờ duyệt';
    WHEN OTHERS THEN
        ROLLBACK;
        p_status := -1;
        p_message := SQLERRM;
END;
/
