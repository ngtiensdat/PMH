import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/models/api-response.model';
import { GroupCategoryResponse, GroupCategoryRequest, BatchItemResult } from '../../../shared/models/group-category.model';
import { AuditLogItem } from '../../../shared/models/audit-log.model';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private apiUrl = `${environment.apiBase}/api/group-category`;

  constructor(private http: HttpClient) {}

  // --- DẠNG 1: JPA & JPA SPECIFICATION ---

  search(
    filters: { paramType?: string; paramValue?: string; paramName?: string; status?: number[]; isActive?: number[] },
    page: number = 0,
    size: number = 10,
    sort: string = 'updatedDate,desc'
  ): Observable<ApiResponse<PageResponse<GroupCategoryResponse>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (filters.paramType) params = params.set('paramType', filters.paramType);
    if (filters.paramValue) params = params.set('paramValue', filters.paramValue);
    if (filters.paramName) params = params.set('paramName', filters.paramName);

    if (filters.status && filters.status.length > 0) {
      filters.status.forEach((s: number) => { params = params.append('status', s.toString()); });
    }
    if (filters.isActive && filters.isActive.length > 0) {
      filters.isActive.forEach((a: number) => { params = params.append('isActive', a.toString()); });
    }

    return this.http.get<ApiResponse<PageResponse<GroupCategoryResponse>>>(`${this.apiUrl}/search`, { params });
  }

  getById(id: number): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.http.get<ApiResponse<GroupCategoryResponse>>(`${this.apiUrl}/${id}`);
  }

  create(dto: GroupCategoryRequest): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.http.post<ApiResponse<GroupCategoryResponse>>(this.apiUrl, dto);
  }

  update(id: number, dto: GroupCategoryRequest): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.http.put<ApiResponse<GroupCategoryResponse>>(`${this.apiUrl}/${id}`, dto);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`);
  }

  sendApproval(id: number): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.http.post<ApiResponse<GroupCategoryResponse>>(`${this.apiUrl}/${id}/send-approval`, {});
  }

  cancelApproval(id: number): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.http.post<ApiResponse<GroupCategoryResponse>>(`${this.apiUrl}/${id}/cancel-approval`, {});
  }

  // --- DẠNG 2: NATIVE QUERY ---

  getComplexList(): Observable<ApiResponse<GroupCategoryResponse[]>> {
    return this.http.get<ApiResponse<GroupCategoryResponse[]>>(`${this.apiUrl}/complex-list`);
  }

  exportExcel(): Observable<ApiResponse<Record<string, unknown>[]>> {
    return this.http.get<ApiResponse<Record<string, unknown>[]>>(`${this.apiUrl}/export`);
  }

  // --- DẠNG 3: STORED PROCEDURE ---

  batchApprove(ids: number[]): Observable<ApiResponse<BatchItemResult[]>> {
    return this.http.post<ApiResponse<BatchItemResult[]>>(`${this.apiUrl}/batch-approve`, ids);
  }

  batchReject(ids: number[], reason?: string): Observable<ApiResponse<BatchItemResult[]>> {
    let params = new HttpParams();
    if (reason) params = params.set('reason', reason);
    return this.http.post<ApiResponse<BatchItemResult[]>>(`${this.apiUrl}/batch-reject`, ids, { params });
  }

  getHistory(id: number, page: number = 0, size: number = 5): Observable<ApiResponse<PageResponse<AuditLogItem>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<ApiResponse<PageResponse<AuditLogItem>>>(`${environment.apiBase}/api/audit-log/group-category/${id}`, { params });
  }

  // Cache state for list pagination and filters
  private listState: {
    page: number;
    size: number;
    filters: any;
    viewMode: 'jpa' | 'native';
    activeTabIndex: number;
  } | null = null;

  setListState(state: any) {
    this.listState = state;
  }

  getListState() {
    return this.listState;
  }

  clearListState() {
    this.listState = null;
  }
}
