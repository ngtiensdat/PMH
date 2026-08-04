import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/models/api-response.model';
import { GroupCategoryResponse, GroupCategoryRequest } from '../../../shared/models/group-category.model';
import { AuditLogItem } from '../../../shared/models/audit-log.model';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private apiUrl = `${environment.apiBase}/api/group-category`;

  constructor(private http: HttpClient) {}

  private getHeaders(username?: string): HttpHeaders {
    const activeUser = localStorage.getItem('app_usercode') || 'USER01';
    const finalUser = (username === 'USER01' || username === 'APPROVER' || !username) ? activeUser : username;
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'X-Username': finalUser
    });
  }

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

  create(dto: GroupCategoryRequest, username: string = 'USER01'): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.http.post<ApiResponse<GroupCategoryResponse>>(this.apiUrl, dto, { headers: this.getHeaders(username) });
  }

  update(id: number, dto: GroupCategoryRequest, username: string = 'USER01'): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.http.put<ApiResponse<GroupCategoryResponse>>(`${this.apiUrl}/${id}`, dto, { headers: this.getHeaders(username) });
  }

  delete(id: number, username: string = 'USER01'): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`, { headers: this.getHeaders(username) });
  }

  sendApproval(id: number, username: string = 'USER01'): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.http.post<ApiResponse<GroupCategoryResponse>>(`${this.apiUrl}/${id}/send-approval`, {}, { headers: this.getHeaders(username) });
  }

  cancelApproval(id: number, username: string = 'USER01'): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.http.post<ApiResponse<GroupCategoryResponse>>(`${this.apiUrl}/${id}/cancel-approval`, {}, { headers: this.getHeaders(username) });
  }

  // --- DẠNG 2: NATIVE QUERY ---

  getComplexList(): Observable<ApiResponse<GroupCategoryResponse[]>> {
    return this.http.get<ApiResponse<GroupCategoryResponse[]>>(`${this.apiUrl}/complex-list`);
  }

  exportExcel(): Observable<ApiResponse<Record<string, unknown>[]>> {
    return this.http.get<ApiResponse<Record<string, unknown>[]>>(`${this.apiUrl}/export`);
  }

  // --- DẠNG 3: STORED PROCEDURE ---

  batchApprove(ids: number[], username: string = 'APPROVER'): Observable<ApiResponse<Record<string, unknown>[]>> {
    return this.http.post<ApiResponse<Record<string, unknown>[]>>(`${this.apiUrl}/batch-approve`, ids, { headers: this.getHeaders(username) });
  }

  batchReject(ids: number[], reason?: string, username: string = 'APPROVER'): Observable<ApiResponse<Record<string, unknown>[]>> {
    let params = new HttpParams();
    if (reason) params = params.set('reason', reason);
    return this.http.post<ApiResponse<Record<string, unknown>[]>>(`${this.apiUrl}/batch-reject`, ids, {
      params,
      headers: this.getHeaders(username)
    });
  }

  getHistory(id: number): Observable<ApiResponse<AuditLogItem[]>> {
    return this.http.get<ApiResponse<AuditLogItem[]>>(`${environment.apiBase}/api/audit-log/group-category/${id}`);
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
