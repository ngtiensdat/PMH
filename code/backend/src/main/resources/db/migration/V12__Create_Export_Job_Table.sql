-- ================================================================================
-- V12: THÊM INDEX TỐI ƯU HIỆU NĂNG CHO BẢNG EXPORT_JOB (Bảng đã có sẵn)
-- Bảng EXPORT_JOB đã được Mentor tạo sẵn trên Oracle DB với 14 cột.
-- Script này CHỈ tạo Index để tối ưu query tìm job theo userId + status.
-- ================================================================================

-- Index hỗ trợ 3 query chính:
--   1. findActiveJobByUserId:   WHERE USER_ID = ? AND STATUS IN ('PENDING', 'PROCESSING')
--   2. findRecentJobsByUserId:  WHERE USER_ID = ? AND CREATED_AT >= ?
--   3. findStuckJobs:           WHERE STATUS IN (...) AND LAST_HEARTBEAT_AT < ?
CREATE INDEX IDX_EXPORT_JOB_USER_STATUS ON EXPORT_JOB (USER_ID, STATUS, EXPIRES_AT);

COMMIT;
