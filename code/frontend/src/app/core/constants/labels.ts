export const APP_LABELS_VN = {
  // Navigation & User Profile
  navigation: {
    dashboard: 'Tổng quan',
    paramSetting: 'Tham số',
    logout: 'Đăng xuất',
    systemParams: 'Tham số hệ thống',
    processingComponents: 'Tham số Cấu phần xử lý',
    groupCategory: 'Tham số danh mục theo nhóm',
    user: {
      name: 'Quản trị viên',
      role: 'Admin',
      avatar: 'QT'
    }
  },

  // Common UI Texts
  common: {
    home: 'Trang chủ',
    search: 'Tìm kiếm',
    clearFilter: 'Xóa bộ lọc',
    addNew: 'Thêm mới',
    edit: 'Chỉnh sửa',
    copy: 'Sao chép',
    delete: 'Xóa',
    sendApproval: 'Gửi duyệt',
    viewDetail: 'Xem chi tiết',
    exportExcel: 'Xuất Excel',
    batchApprove: 'Duyệt chọn',
    batchReject: 'Từ chối chọn',
    save: 'Lưu lại',
    cancel: 'Hủy',
    close: 'Đóng',
    active: 'Hoạt động',
    inactive: 'Không hoạt động',
    noData: 'Không có dữ liệu hiển thị.',
    actions: 'Thao tác',
    stt: 'STT',
    confirmTitle: 'Xác nhận',
    confirm: 'Xác nhận',
    all: 'Tất cả',
    selectAll: 'Chọn tất cả',
    clearAll: 'Bỏ chọn tất cả',
    selectValue: 'Chọn giá trị',
    reasonReject: 'Lý do từ chối',
    reasonRejectPlaceholder: 'Nhập lý do từ chối',
    user: 'Người dùng',
    datePerform: 'Ngày thực hiện',
    actionHistory: 'Thao tác',
    contentHistory: 'Nội dung',
    historyTitle: 'Lịch sử thao tác',
    enterContent: 'Nhập nội dung',
    approve: 'Duyệt',
    reject: 'Từ chối',
    back: 'Quay lại',
    oldData: 'Dữ liệu cũ',
    newData: 'Dữ liệu mới',
    comparisonDetail: 'Chi tiết đối chiếu',
    status: {
      new: '1 - Tạo mới',
      pending: '3 - Chờ duyệt',
      approved: '4 - Đã phê duyệt',
      rejected: '5 - Từ chối',
      canceled: '7 - Hủy duyệt'
    }
  },

  // Component Module
  components: {
    title: 'Tham số Cấu phần xử lý',
    listTitle: 'Danh sách cấu phần xử lý',
    code: 'Mã cấu phần',
    name: 'Tên cấu phần',
    messageType: 'Chuẩn tin điện',
    connectionMethod: 'Phương thức kết nối',
    checkToken: 'Kiểm tra Token',
    description: 'Mô tả chi tiết',
    effectiveDate: 'Ngày hiệu lực',
    endEffectiveDate: 'Ngày hết hiệu lực',
    paramStatus: 'Trạng thái tham số',
    activeStatus: 'Trạng thái hoạt động',
    createdBy: 'Người tạo',
    createdDate: 'Ngày tạo',
    updatedBy: 'Người cập nhật',
    updatedDate: 'Ngày cập nhật',
    hasChanges: 'Có thay đổi chờ duyệt',
    systemHistory: 'Lịch sử hệ thống'
  },

  // Category Module
  category: {
    title: 'Tham số danh mục theo nhóm',
    listTitle: 'Danh sách tham số',
    groupName: 'Danh mục theo nhóm',
    paramValue: 'Giá trị thành phần',
    paramName: 'Tên thành phần',
    description: 'Mô tả',
    componentCode: 'Cấu phần xử lý',
    effectiveDate: 'Ngày hiệu lực',
    endEffectiveDate: 'Ngày hết hiệu lực',
    paramStatus: 'Trạng thái tham số',
    activeStatus: 'Trạng thái hoạt động'
  },

  // Error & Notification Messages
  messages: {
    success: {
      logout: 'Đã đăng xuất khỏi hệ thống',
      login: 'Đăng nhập thành công',
      delete: 'Xóa thành công!',
      cancelEditSuccess: 'Hủy yêu cầu sửa thành công!',
      sendApproval: 'Gửi duyệt thành công!',
      cancelApproval: 'Hủy duyệt thành công!',
      approve: 'Phê duyệt thành công!',
      update: 'Cập nhật thành công',
      updateAndSendApproval: 'Cập nhật và Gửi duyệt thành công',
      create: 'Thêm mới thành công',
      createAndSendApproval: 'Thêm mới và Gửi duyệt thành công',
      title: 'Thành công!'
    },
    warning: {
      selectAtLeastOneToApprove: 'Vui lòng chọn ít nhất một bản ghi để duyệt!',
      selectAtLeastOneToReject: 'Vui lòng chọn ít nhất một bản ghi để từ chối!',
      selectAtLeastOneComponentToApprove: 'Vui lòng chọn ít nhất một cấu phần để duyệt!',
      selectAtLeastOneComponentToReject: 'Vui lòng chọn ít nhất một cấu phần để từ chối!',
      noDataToExport: 'Không có dữ liệu để xuất!',
      noComponentDataToExport: 'Không có dữ liệu cấu phần để xuất!',
      noFormChange: 'Không có thay đổi nào so với dữ liệu gốc! Không cần gửi duyệt sửa.',
      selfApproveDenied: 'Bạn là người tạo/sửa các bản ghi này nên không được phép tự phê duyệt hoặc từ chối!',
      makerCheckerConflict: 'Không thể thao tác do vi phạm quy tắc Maker-Checker!'
    },
    errorPrefix: {
      loadData: 'Lỗi tải dữ liệu: ',
      loadLinkedData: 'Lỗi tải dữ liệu liên kết: ',
      loadDetail: 'Không thể nạp dữ liệu chi tiết: ',
      reject: 'Lỗi thực hiện từ chối duyệt: ',
      history: 'Không thể tải lịch sử thao tác: ',
      delete: 'Không thể xóa: ',
      executeFailed: 'Thực thi thất bại: ',
      sendApproval: 'Lỗi gửi duyệt: ',
      cancelApproval: 'Lỗi hủy duyệt: ',
      batchApprove: 'Lỗi duyệt hàng loạt: ',
      approveFailed: 'Lỗi khi duyệt: ',
      rejectFailed: 'Lỗi khi từ chối: ',
      exportExcel: 'Lỗi xuất dữ liệu: ',
      loadComponent: 'Không thể nạp dữ liệu cấu phần: ',
      loadCategory: 'Không thể nạp dữ liệu tham số: ',
      update: 'Lỗi cập nhật: ',
      saveAndSendFailed: 'Đã lưu bản ghi, nhưng lỗi gửi duyệt: ',
      create: 'Lỗi thêm mới: ',
      invalidInput: 'Dữ liệu nhập không hợp lệ',
      loginFailed: 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin!'
    },
    validation: {
      invalidData: 'Dữ liệu không hợp lệ',
      usernameRequired: 'Tên đăng nhập không được để trống',
      usernameMaxLength: 'Tên đăng nhập tối đa 50 ký tự',
      passwordRequired: 'Mật khẩu không được để trống',
      passwordMaxLength: 'Mật khẩu tối đa 100 ký tự',

      componentCodeRequired: 'Mã cấu phần không được để trống',
      componentCodeMaxLength: 'Mã cấu phần tối đa 200 ký tự',
      componentCodeRegex: 'Mã cấu phần chỉ gồm chữ in hoa, số và dấu gạch dưới, không chứa tiếng Việt, khoảng trắng hay ký tự đặc biệt',

      componentNameRequired: 'Tên cấu phần không được để trống',
      componentNameMaxLength: 'Tên cấu phần tối đa 150 ký tự',
      componentNameInvalidChars: 'Tên cấu phần không được chứa khoảng trắng đặc biệt hay ký tự đặc biệt (^, #, |, *, @, $, ...)',

      paramTypeRequired: 'Danh mục theo nhóm không được để trống',
      paramTypeMaxLength: 'Danh mục theo nhóm tối đa 255 ký tự',
      paramValueRequired: 'Giá trị thành phần không được để trống',
      paramValueMaxLength: 'Giá trị thành phần tối đa 255 ký tự',
      paramNameRequired: 'Tên thành phần không được để trống',
      paramNameMaxLength: 'Tên thành phần tối đa 255 ký tự',
      paramNameInvalidChars: 'Tên thành phần không được chứa khoảng trắng đặc biệt hay ký tự đặc biệt (^, #, |, *, @, $, ...)',

      descriptionMaxLength: 'Mô tả tối đa 4000 ký tự',
      effectiveDateRequired: 'Ngày hiệu lực không được để trống',
      effectiveDateInvalidFormat: 'Ngày hiệu lực không phải định dạng ngày giờ hợp lệ (yyyy-MM-ddTHH:mm)',
      effectiveDateTooFarPast: 'Ngày hiệu lực không được quá 100 năm trong quá khứ',
      effectiveDateTooFarFuture: 'Ngày hiệu lực không được vượt quá 100 năm trong tương lai',

      endEffectiveDateInvalidFormat: 'Ngày hết hiệu lực không phải định dạng ngày giờ hợp lệ (yyyy-MM-ddTHH:mm)',
      endEffectiveDateTooFarPast: 'Ngày hết hiệu lực không được quá 100 năm trong quá khứ',
      endEffectiveDateTooFarFuture: 'Ngày hết hiệu lực không được vượt quá 100 năm trong tương lai',
      endEffectiveDateMustBeAfter: 'Ngày hết hiệu lực phải sau ngày hiệu lực'
    }
  }
};

export const APP_LABELS_EN = {
  // Navigation & User Profile
  navigation: {
    dashboard: 'Dashboard',
    paramSetting: 'Parameters',
    logout: 'Logout',
    systemParams: 'System Parameters',
    processingComponents: 'Processing Components',
    groupCategory: 'Group Category Parameters',
    user: {
      name: 'Hoang Van Thuan',
      role: 'Specialist',
      avatar: 'HVT'
    }
  },

  // Common UI Texts
  common: {
    home: 'Home',
    search: 'Search',
    clearFilter: 'Clear Filter',
    addNew: 'Add New',
    edit: 'Edit',
    copy: 'Copy',
    delete: 'Delete',
    sendApproval: 'Send Approval',
    viewDetail: 'View Detail',
    exportExcel: 'Export Excel',
    batchApprove: 'Approve Selected',
    batchReject: 'Reject Selected',
    save: 'Save',
    cancel: 'Cancel',
    close: 'Close',
    active: 'Active',
    inactive: 'Inactive',
    noData: 'No data to display.',
    actions: 'Actions',
    stt: 'No.',
    confirmTitle: 'Confirmation',
    confirm: 'Confirm',
    all: 'All',
    selectAll: 'Select all',
    clearAll: 'Clear all',
    selectValue: 'Select value',
    reasonReject: 'Reason for rejection',
    reasonRejectPlaceholder: 'Enter reason for rejection',
    user: 'User',
    datePerform: 'Date performed',
    actionHistory: 'Action',
    contentHistory: 'Content',
    historyTitle: 'Action History',
    enterContent: 'Enter content',
    approve: 'Approve',
    reject: 'Reject',
    back: 'Back',
    oldData: 'Old Data',
    newData: 'New Data',
    comparisonDetail: 'Comparison Details',
    status: {
      new: '1 - New',
      pending: '3 - Pending',
      approved: '4 - Approved',
      rejected: '5 - Rejected',
      canceled: '7 - Canceled'
    }
  },

  // Component Module
  components: {
    title: 'Processing Component Parameters',
    listTitle: 'Processing Component List',
    code: 'Component Code',
    name: 'Component Name',
    messageType: 'Message Type',
    connectionMethod: 'Connection Method',
    checkToken: 'Check Token',
    description: 'Detailed Description',
    effectiveDate: 'Effective Date',
    endEffectiveDate: 'End Effective Date',
    paramStatus: 'Parameter Status',
    activeStatus: 'Active Status',
    createdBy: 'Created By',
    createdDate: 'Created Date',
    updatedBy: 'Updated By',
    updatedDate: 'Updated Date',
    hasChanges: 'Has pending changes',
    systemHistory: 'System History'
  },

  // Category Module
  category: {
    title: 'Group Category Parameters',
    listTitle: 'Parameter List',
    groupName: 'Group Category',
    paramValue: 'Component Value',
    paramName: 'Component Name',
    description: 'Description',
    componentCode: 'Processing Component',
    effectiveDate: 'Effective Date',
    endEffectiveDate: 'End Effective Date',
    paramStatus: 'Parameter Status',
    activeStatus: 'Active Status'
  },

  // Error & Notification Messages
  messages: {
    success: {
      logout: 'Logged out successfully',
      login: 'Logged in successfully',
      delete: 'Deleted successfully!',
      cancelEditSuccess: 'Canceled edit request successfully!',
      sendApproval: 'Sent for approval successfully!',
      cancelApproval: 'Canceled approval successfully!',
      approve: 'Approved successfully!',
      update: 'Updated successfully',
      updateAndSendApproval: 'Updated and Sent for approval successfully',
      create: 'Created successfully',
      createAndSendApproval: 'Created and Sent for approval successfully',
      title: 'Success!'
    },
    warning: {
      selectAtLeastOneToApprove: 'Please select at least one record to approve!',
      selectAtLeastOneToReject: 'Please select at least one record to reject!',
      selectAtLeastOneComponentToApprove: 'Please select at least one component to approve!',
      selectAtLeastOneComponentToReject: 'Please select at least one component to reject!',
      noDataToExport: 'No data to export!',
      noComponentDataToExport: 'No component data to export!',
      noFormChange: 'No changes compared to original data!',
      selfApproveDenied: 'You created or edited these records and cannot self-approve or self-reject them!',
      makerCheckerConflict: 'Cannot perform action due to Maker-Checker rule violation!'
    },
    errorPrefix: {
      loadData: 'Error loading data: ',
      loadLinkedData: 'Error loading linked data: ',
      loadDetail: 'Cannot load detail data: ',
      reject: 'Error rejecting item: ',
      history: 'Cannot load action history: ',
      delete: 'Cannot delete item: ',
      executeFailed: 'Execution failed: ',
      sendApproval: 'Error sending for approval: ',
      cancelApproval: 'Error canceling approval: ',
      batchApprove: 'Error batch approving: ',
      approveFailed: 'Error approving: ',
      rejectFailed: 'Error rejecting: ',
      exportExcel: 'Error exporting Excel: ',
      loadComponent: 'Cannot load component data: ',
      loadCategory: 'Cannot load category data: ',
      update: 'Error updating: ',
      saveAndSendFailed: 'Saved, but failed to send for approval: ',
      create: 'Error creating item: ',
      invalidInput: 'Invalid input data',
      loginFailed: 'Login failed. Please check credentials!'
    },
    validation: {
      invalidData: 'Invalid data',
      usernameRequired: 'Username is required',
      usernameMaxLength: 'Username maximum 50 characters',
      passwordRequired: 'Password is required',
      passwordMaxLength: 'Password maximum 100 characters',

      componentCodeRequired: 'Component Code is required',
      componentCodeMaxLength: 'Component Code maximum 200 characters',
      componentCodeRegex: 'Component Code can only contain uppercase letters, numbers, and underscores',

      componentNameRequired: 'Component Name is required',
      componentNameMaxLength: 'Component Name maximum 150 characters',
      componentNameInvalidChars: 'Component Name cannot contain special characters',

      paramTypeRequired: 'Group category is required',
      paramTypeMaxLength: 'Group category maximum 255 characters',
      paramValueRequired: 'Component value is required',
      paramValueMaxLength: 'Component value maximum 255 characters',
      paramNameRequired: 'Component name is required',
      paramNameMaxLength: 'Component name maximum 255 characters',
      paramNameInvalidChars: 'Component name cannot contain special characters',

      descriptionMaxLength: 'Description maximum 4000 characters',
      effectiveDateRequired: 'Effective Date is required',
      effectiveDateInvalidFormat: 'Effective Date format is invalid (yyyy-MM-ddTHH:mm)',
      effectiveDateTooFarPast: 'Effective Date cannot be more than 100 years in the past',
      effectiveDateTooFarFuture: 'Effective Date cannot exceed 100 years in the future',

      endEffectiveDateInvalidFormat: 'End Effective Date format is invalid (yyyy-MM-ddTHH:mm)',
      endEffectiveDateTooFarPast: 'End Effective Date cannot be more than 100 years in the past',
      endEffectiveDateTooFarFuture: 'End Effective Date cannot exceed 100 years in the future',
      endEffectiveDateMustBeAfter: 'End Effective Date must be after Effective Date'
    }
  }
};
