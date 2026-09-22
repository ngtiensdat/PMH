-- ================================================================================
-- V13: THÊM QUYỀN XUẤT FILE NHẬT KÝ GIAO DỊCH (TRANSACTION_EXPORT)
-- ================================================================================

INSERT INTO PMH_PERMISSIONS (PERMISSION_CODE, PERMISSION_NAME, MODULE, DESCRIPTION)
VALUES ('TRANSACTION_EXPORT', 'Xuất file nhật ký giao dịch', 'TRANSACTION', 'Quyền xuất dữ liệu nhật ký giao dịch sang file CSV');

INSERT INTO PMH_ROLE_PERMISSIONS (ROLE_ID, PERMISSION_ID)
SELECT r.ID, p.ID
FROM PMH_ROLES r, PMH_PERMISSIONS p
WHERE r.ROLE_CODE IN ('ROLE_MAKER', 'ROLE_CHECKER', 'ROLE_ADMIN')
  AND p.PERMISSION_CODE = 'TRANSACTION_EXPORT';

COMMIT;
