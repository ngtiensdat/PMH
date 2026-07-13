import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/models/api-response.model';
import { ProcessingComponentResponse, ProcessingComponentRequest } from '../../../shared/models/component.model';
import { AuditLogItem } from '../../../shared/models/audit-log.model';

@Injectable({
  providedIn: 'root'
})
export class ComponentService {
  private apiUrl = `${environment.apiBase}/api/components`;

  constructor(private http: HttpClient) {}

  private getHeaders(username: string = 'USER01'): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'X-Username': username
    });
  }

  search(
    filters: { componentCode?: string; componentName?: string; messageType?: string[]; connectionMethod?: string[]; status?: number[]; isActive?: number[] },
    page: number = 0,
    size: number = 10,
    sort: string = 'updatedDate,desc'
  ): Observable<ApiResponse<PageResponse<ProcessingComponentResponse>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (filters.componentCode) params = params.set('componentCode', filters.componentCode);
    if (filters.componentName) params = params.set('componentName', filters.componentName);
    if (filters.messageType) {
      filters.messageType.forEach((mt: string) => { params = params.append('messageType', mt); });
    }
    if (filters.connectionMethod) {
      filters.connectionMethod.forEach((cm: string) => { params = params.append('connectionMethod', cm); });
    }

    if (filters.status && filters.status.length > 0) {
      filters.status.forEach((s: number) => { params = params.append('status', s.toString()); });
    }
    if (filters.isActive && filters.isActive.length > 0) {
      filters.isActive.forEach((a: number) => { params = params.append('isActive', a.toString()); });
    }

    return this.http.get<ApiResponse<PageResponse<ProcessingComponentResponse>>>(`${this.apiUrl}/search`, { params });
  }

  getByCode(code: string): Observable<ApiResponse<ProcessingComponentResponse>> {
    return this.http.get<ApiResponse<ProcessingComponentResponse>>(`${this.apiUrl}/${code}`);
  }

  getActiveList(): Observable<ApiResponse<ProcessingComponentResponse[]>> {
    return this.http.get<ApiResponse<ProcessingComponentResponse[]>>(`${this.apiUrl}/active-list`);
  }

  create(dto: ProcessingComponentRequest, username: string = 'USER01'): Observable<ApiResponse<ProcessingComponentResponse>> {
    return this.http.post<ApiResponse<ProcessingComponentResponse>>(this.apiUrl, dto, { headers: this.getHeaders(username) });
  }

  update(code: string, dto: ProcessingComponentRequest, username: string = 'USER01'): Observable<ApiResponse<ProcessingComponentResponse>> {
    return this.http.put<ApiResponse<ProcessingComponentResponse>>(`${this.apiUrl}/${code}`, dto, { headers: this.getHeaders(username) });
  }

  delete(code: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${code}`);
  }

  sendApproval(code: string, username: string = 'USER01'): Observable<ApiResponse<ProcessingComponentResponse>> {
    return this.http.post<ApiResponse<ProcessingComponentResponse>>(`${this.apiUrl}/${code}/send-approval`, {}, { headers: this.getHeaders(username) });
  }

  exportExcel(): Observable<ApiResponse<Record<string, unknown>[]>> {
    return this.http.get<ApiResponse<Record<string, unknown>[]>>(`${this.apiUrl}/export`);
  }

  batchApprove(codes: string[], username: string = 'APPROVER'): Observable<ApiResponse<Record<string, unknown>[]>> {
    return this.http.post<ApiResponse<Record<string, unknown>[]>>(`${this.apiUrl}/batch-approve`, codes, { headers: this.getHeaders(username) });
  }

  batchReject(codes: string[], reason?: string, username: string = 'APPROVER'): Observable<ApiResponse<Record<string, unknown>[]>> {
    let params = new HttpParams();
    if (reason) params = params.set('reason', reason);
    return this.http.post<ApiResponse<Record<string, unknown>[]>>(`${this.apiUrl}/batch-reject`, codes, {
      params,
      headers: this.getHeaders(username)
    });
  }

  getHistory(code: string): Observable<ApiResponse<AuditLogItem[]>> {
    return this.http.get<ApiResponse<AuditLogItem[]>>(`${environment.apiBase}/api/audit-log/component/${code}`);
  }
}
