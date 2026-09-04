export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  size?: number;
  number?: number;
  first?: boolean;
  last?: boolean;
  empty?: boolean;
  page?: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

/** Kết quả từng item trong batch-approve / batch-reject — dùng chung mọi module */
export interface BatchItemResult {
  id?: number;
  code?: string;
  status: string; // "SUCCESS" | "FAILED"
  errorMessage?: string;
}

/** Trạng thái phân trang + filter của list page — dùng chung cho mọi FeatureService */
export interface ListState {
  page: number;
  size: number;
  filters: Record<string, unknown>;
  viewMode: 'jpa' | 'native';
  activeTabIndex: number;
}
