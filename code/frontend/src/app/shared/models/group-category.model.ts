export interface GroupCategoryResponse {
  id: number;
  paramName: string;
  paramValue: string;
  paramType: string;
  description: string;
  componentCode: string;
  status: number;
  isActive: number;
  isDisplay: number;
  newData?: string;
  effectiveDate: string;
  endEffectiveDate?: string;
  createdBy: string;
  createdDate: string;
  updatedBy: string;
  updatedDate: string;
  componentName?: string; // Tên cấu phần từ bảng liên kết (nếu có)
  // Dynamic mapped fields parsed from newData
  effectiveDateParsed?: Date;
  endEffectiveDateParsed?: Date;
}

export interface GroupCategoryRequest {
  id?: number;
  paramName: string;
  paramValue: string;
  paramType: string;
  description?: string;
  componentCode: string;
  status?: number;
  isActive?: number;
  isDisplay?: number;
  newData?: string;
  effectiveDate: string;
  endEffectiveDate?: string;
}
