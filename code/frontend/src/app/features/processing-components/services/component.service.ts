import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse, BatchItemResult } from '../../../shared/models/api-response.model';
import { ProcessingComponentResponse, ProcessingComponentRequest } from '../../../shared/models/component.model';
import { AuditLogItem } from '../../../shared/models/audit-log.model';
import { BaseFeatureService } from '../../../shared/services/base-feature.service';

@Injectable({
  providedIn: 'root'
})
export class ComponentService extends BaseFeatureService {
  private apiUrl = `${environment.apiBase}/api/components`;
  private activeComponentsCache = new Map<string, ApiResponse<ProcessingComponentResponse[]>>();

  constructor(private http: HttpClient) { super(); }

  // --- DẠNG 1: JPA & JPA SPECIFICATION ---

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
    if (filters.messageType)      filters.messageType.forEach(mt      => { params = params.append('messageType',      mt); });
    if (filters.connectionMethod) filters.connectionMethod.forEach(cm => { params = params.append('connectionMethod', cm); });
    if (filters.status?.length)   filters.status.forEach(s            => { params = params.append('status',   s.toString()); });
    if (filters.isActive?.length) filters.isActive.forEach(a          => { params = params.append('isActive', a.toString()); });

    return this.http.get<ApiResponse<PageResponse<ProcessingComponentResponse>>>(`${this.apiUrl}/search`, { params });
  }

  getByCode(code: string): Observable<ApiResponse<ProcessingComponentResponse>> {
    return this.http.get<ApiResponse<ProcessingComponentResponse>>(`${this.apiUrl}/${code}`);
  }

  getActiveList(status?: number): Observable<ApiResponse<ProcessingComponentResponse[]>> {
    const cacheKey = status !== undefined && status !== null ? status.toString() : 'all';
    if (this.activeComponentsCache.has(cacheKey)) {
      return of(this.activeComponentsCache.get(cacheKey)!);
    }
    let params = new HttpParams();
    if (status !== undefined && status !== null) params = params.set('status', status.toString());
    return this.http.get<ApiResponse<ProcessingComponentResponse[]>>(`${this.apiUrl}/active-list`, { params }).pipe(
      tap(res => this.activeComponentsCache.set(cacheKey, res))
    );
  }

  create(dto: ProcessingComponentRequest): Observable<ApiResponse<ProcessingComponentResponse>> {
    return this.http.post<ApiResponse<ProcessingComponentResponse>>(this.apiUrl, dto);
  }

  update(code: string, dto: ProcessingComponentRequest): Observable<ApiResponse<ProcessingComponentResponse>> {
    return this.http.put<ApiResponse<ProcessingComponentResponse>>(`${this.apiUrl}/${code}`, dto);
  }

  delete(code: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${code}`);
  }

  sendApproval(code: string): Observable<ApiResponse<ProcessingComponentResponse>> {
    return this.http.post<ApiResponse<ProcessingComponentResponse>>(`${this.apiUrl}/${code}/send-approval`, {});
  }

  cancelApproval(code: string): Observable<ApiResponse<ProcessingComponentResponse>> {
    return this.http.post<ApiResponse<ProcessingComponentResponse>>(`${this.apiUrl}/${code}/cancel-approval`, {});
  }

  // --- DẠNG 2: NATIVE QUERY ---

  exportExcel(): Observable<ApiResponse<Record<string, unknown>[]>> {
    return this.http.get<ApiResponse<Record<string, unknown>[]>>(`${this.apiUrl}/export`);
  }

  // --- DẠNG 3: STORED PROCEDURE ---

  batchApprove(codes: string[]): Observable<ApiResponse<BatchItemResult[]>> {
    return this.http.post<ApiResponse<BatchItemResult[]>>(`${this.apiUrl}/batch-approve`, codes);
  }

  batchReject(codes: string[], reason?: string): Observable<ApiResponse<BatchItemResult[]>> {
    let params = new HttpParams();
    if (reason) params = params.set('reason', reason);
    return this.http.post<ApiResponse<BatchItemResult[]>>(`${this.apiUrl}/batch-reject`, codes, { params });
  }

  getHistory(code: string, page: number = 0, size: number = 5): Observable<ApiResponse<PageResponse<AuditLogItem>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<ApiResponse<PageResponse<AuditLogItem>>>(`${environment.apiBase}/api/audit-log/component/${code}`, { params });
  }
}
