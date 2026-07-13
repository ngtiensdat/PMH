export interface ProcessingComponentResponse {
  componentCode: string;
  componentName: string;
  messageType: string;
  connectionMethod: string;
  checkToken: string;
  description: string;
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
}

export interface ProcessingComponentRequest {
  componentCode: string;
  componentName: string;
  messageType?: string;
  connectionMethod?: string;
  checkToken: string;
  description?: string;
  status?: number;
  isActive?: number;
  isDisplay?: number;
  newData?: string;
  effectiveDate: string;
  endEffectiveDate?: string;
}
