-- ================================================================================
-- V11: THÊM QUYỀN XEM NHẬT KÝ GIAO DỊCH (TRANSACTION_VIEW)
-- ================================================================================

INSERT INTO PMH_PERMISSIONS (PERMISSION_CODE, PERMISSION_NAME, MODULE, DESCRIPTION) VALUES ('TRANSACTION_VIEW', 'Xem nhật ký giao dịch', 'TRANSACTION', 'Quyền xem danh sách và chi tiết nhật ký giao dịch');

INSERT INTO PMH_ROLE_PERMISSIONS (ROLE_ID, PERMISSION_ID) SELECT r.ID, p.ID FROM PMH_ROLES r, PMH_PERMISSIONS p WHERE r.ROLE_CODE IN ('ROLE_MAKER', 'ROLE_CHECKER', 'ROLE_ADMIN') AND p.PERMISSION_CODE = 'TRANSACTION_VIEW';

COMMIT;
