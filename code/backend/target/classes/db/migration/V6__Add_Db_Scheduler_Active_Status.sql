-- ================================================================================
-- V6: CHUYỂN CRON JOB QUÉT CẬP NHẬT IS_ACTIVE SANG ORACLE DBMS_SCHEDULER
-- ================================================================================

-- 1. Tạo Stored Procedure cập nhật IS_ACTIVE cho cả 2 bảng
CREATE OR REPLACE PROCEDURE PROC_UPDATE_ACTIVE_STATUS AS
BEGIN
    -- Cập nhật IS_ACTIVE cho PMH_GROUP_CATEGORY bằng câu lệnh CASE WHEN tối ưu
    UPDATE PMH_GROUP_CATEGORY
    SET IS_ACTIVE = CASE 
        WHEN EFFECTIVE_DATE <= SYSDATE AND (END_EFFECTIVE_DATE IS NULL OR END_EFFECTIVE_DATE >= SYSDATE) THEN 1
        ELSE 0
    END
    WHERE IS_ACTIVE <> (
        CASE 
            WHEN EFFECTIVE_DATE <= SYSDATE AND (END_EFFECTIVE_DATE IS NULL OR END_EFFECTIVE_DATE >= SYSDATE) THEN 1
            ELSE 0
        END
    );

    -- Cập nhật IS_ACTIVE cho PMH_COMPONENTS bằng câu lệnh CASE WHEN tối ưu
    UPDATE PMH_COMPONENTS
    SET IS_ACTIVE = CASE 
        WHEN EFFECTIVE_DATE <= SYSDATE AND (END_EFFECTIVE_DATE IS NULL OR END_EFFECTIVE_DATE >= SYSDATE) THEN 1
        ELSE 0
    END
    WHERE IS_ACTIVE <> (
        CASE 
            WHEN EFFECTIVE_DATE <= SYSDATE AND (END_EFFECTIVE_DATE IS NULL OR END_EFFECTIVE_DATE >= SYSDATE) THEN 1
            ELSE 0
        END
    );

    COMMIT;
END PROC_UPDATE_ACTIVE_STATUS;
/

-- 2. Đăng ký Oracle Job chạy tự động 5 giây/lần bằng DBMS_SCHEDULER
BEGIN
    BEGIN
        DBMS_SCHEDULER.DROP_JOB(job_name => 'JOB_AUTO_UPDATE_ACTIVE_STATUS', force => TRUE);
    EXCEPTION
        WHEN OTHERS THEN NULL;
    END;

    DBMS_SCHEDULER.CREATE_JOB (
        job_name        => 'JOB_AUTO_UPDATE_ACTIVE_STATUS',
        job_type        => 'STORED_PROCEDURE',
        job_action      => 'PROC_UPDATE_ACTIVE_STATUS',
        start_date      => SYSTIMESTAMP,
        repeat_interval => 'FREQ=SECONDLY; INTERVAL=5',
        enabled         => TRUE,
        comments        => 'Cronjob tu dong quet va cap nhat IS_ACTIVE cho Payment Hub moi 5s'
    );
END;
/
