import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { CategoryService } from '../../services/category.service';
import { HttpErrorResponse } from '@angular/common/http';
import { exportToCsv } from '../../../../shared/utils/csv-export.utils';
import { TableColumnDef, computeNextSort } from '../../../../shared/utils/table-column.utils';
import { GroupCategoryResponse } from '../../../../shared/models/group-category.model';
import { ApiResponse, BatchItemResult } from '../../../../shared/models/api-response.model';
import { BaseListComponent } from '../../../../shared/components/base-list/base-list.component';

import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { RejectReasonDialogComponent } from '../../../../shared/components/reject-reason-dialog/reject-reason-dialog';
import { AuditHistoryDialogComponent } from '../../../../shared/components/audit-history-dialog/audit-history-dialog';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, FormsModule, SharedTaigaModule,
    ConfirmDialogComponent, RejectReasonDialogComponent, AuditHistoryDialogComponent
  ],
  templateUrl: './category-list.html',
  styleUrl: './category-list.css'
})
export class CategoryListComponent extends BaseListComponent<GroupCategoryResponse, number> implements OnInit {

  // ── Config ─────────────────────────────────────────────────────────────────
  protected override readonly batchUnit        = 'bản ghi';
  protected override readonly permissionPrefix = 'CATEGORY';

  // ── Service ────────────────────────────────────────────────────────────────
  private categoryService = inject(CategoryService);
  getHistoryFn = (id: number, page: number, size: number) => this.categoryService.getHistory(id, page, size);

  // ── Form ───────────────────────────────────────────────────────────────────
  searchForm: FormGroup = inject(FormBuilder).group({
    paramType: [''], paramValue: [''], paramName: [''], status: [[]], isActive: [[]]
  });

  // ── State ──────────────────────────────────────────────────────────────────
  categories       = signal<GroupCategoryResponse[]>([]);
  viewMode         = signal<'jpa' | 'native'>('jpa');
  activeTabIndex   = 0;
  joinedCategories = signal<GroupCategoryResponse[]>([]);

  // ── Selection (typed signal for template) ──────────────────────────────────
  readonly selectedIds = signal<number[]>([]);
  protected override getSelectedKeys()                              { return this.selectedIds(); }
  protected override setSelectedKeys(keys: number[])               { this.selectedIds.set(keys); }
  protected override updateSelectedKeys(fn: (k: number[]) => number[]) { this.selectedIds.update(fn); }

  // ── Key & list providers ───────────────────────────────────────────────────
  protected override getItemKey(item: GroupCategoryResponse)        { return item.id; }
  protected override getListItems()                                 {
    return this.viewMode() === 'jpa' ? this.categories() : this.joinedCategories();
  }

  // ── Service calls (1 line each) ────────────────────────────────────────────
  protected override executeBatchApprove(keys: number[]): Observable<ApiResponse<BatchItemResult[]>>
                        { return this.categoryService.batchApprove(keys); }
  protected override executeBatchReject(keys: number[], reason: string): Observable<ApiResponse<BatchItemResult[]>>
                        { return this.categoryService.batchReject(keys, reason); }
  protected override executeDelete(key: number): Observable<ApiResponse<unknown>>
                        { return this.categoryService.delete(key); }
  protected override executeSendApproval(key: number): Observable<ApiResponse<unknown>>
                        { return this.categoryService.sendApproval(key); }
  protected override executeCancelApproval(key: number): Observable<ApiResponse<unknown>>
                        { return this.categoryService.cancelApproval(key); }

  // ── Columns ────────────────────────────────────────────────────────────────
  override columns: TableColumnDef[] = [
    { id: 'checkbox',         label: '',                      isFixed: true,  width: 45  },
    { id: 'stt',              label: 'STT',                   isFixed: true,  width: 60  },
    { id: 'paramType',        label: 'Danh mục theo nhóm',    isFixed: true,  width: 175 },
    { id: 'paramValue',       label: 'Giá trị thành phần',    isFixed: true,  width: 155 },
    { id: 'paramName',        label: 'Tên thành phần',        isFixed: true,  width: 170 },
    { id: 'description',      label: 'Mô tả',                 isFixed: false, width: 150 },
    { id: 'componentCode',    label: 'Cấu phần xử lý',        isFixed: false, width: 150 },
    { id: 'effectiveDate',    label: 'Hiệu lực',              isFixed: false, width: 125 },
    { id: 'endEffectiveDate', label: 'Hết hiệu lực',          isFixed: false, width: 125 },
    { id: 'status',           label: 'Trạng thái tham số',    isFixed: false, width: 180 },
    { id: 'isActive',         label: 'Tình trạng hoạt động',  isFixed: false, width: 180 },
    { id: 'actions',          label: 'Thao tác',              isFixed: false, width: 250 }
  ];

  private readonly columnLabelMap = computed<Record<string, string>>(() => {
    const l = this.languageService.labels();
    return {
      stt: l.common.stt,  paramType: l.category.groupName,
      paramValue: l.category.paramValue,  paramName: l.category.paramName,
      description: l.category.description,  componentCode: l.category.componentCode,
      effectiveDate: l.category.effectiveDate,  endEffectiveDate: l.category.endEffectiveDate,
      status: l.category.paramStatus,  isActive: l.category.activeStatus,  actions: l.common.actions
    };
  });

  getColumnLabel(id: string): string { return this.columnLabelMap()[id] || id; }

  // ── History dialog ─────────────────────────────────────────────────────────
  historyTargetId: number | null = null;

  openHistoryDialog(item: GroupCategoryResponse): void {
    this.historyTargetId   = item.id;
    this.historyTargetName = item.paramName || item.paramValue;
    this.isHistoryOpen     = true;
  }

  trackById(_: number, item: GroupCategoryResponse): number { return item.id; }
  trackByHistoryId(_: number, item: { id?: number }): number | undefined { return item.id; }

  // ── Lifecycle ──────────────────────────────────────────────────────────────
  override ngOnInit(): void {
    const saved = this.categoryService.getListState();
    if (saved) {
      this.viewMode.set(saved.viewMode);
      this.activeTabIndex = saved.activeTabIndex;
      this.page.set(saved.page);
      this.size.set(saved.size);
      this.searchForm.patchValue(saved.filters);
    }
    this.loadData();
  }

  // ── Data loading ───────────────────────────────────────────────────────────
  private buildCategoryFilters() {
    const raw = this.searchForm.value;
    return {
      paramType: raw.paramType,  paramValue: raw.paramValue,  paramName: raw.paramName,
      status: this.toNumberArray(raw.status),  isActive: this.toNumberArray(raw.isActive)
    };
  }

  override loadData(): void {
    this.categoryService.setListState({
      page: this.page(), size: this.size(), filters: this.searchForm.value,
      viewMode: this.viewMode(), activeTabIndex: this.activeTabIndex
    });
    this.isLoading.set(true);

    if (this.viewMode() === 'native') { this.loadJoinedData(); return; }

    this.categoryService.search(this.buildCategoryFilters(), this.page(), this.size(),
      `${this.sortField()},${this.sortDirection()}`).subscribe({
      next: (res) => {
        this.categories.set(res.data.content || []);
        this.totalElements.set(res.data.page?.totalElements ?? res.data.totalElements ?? 0);
        this.selectedIds.set([]);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status !== 401 && this.authService.isLoggedIn()) {
          const prefix = this.languageService.labels().messages?.errorPrefix?.loadData || 'Lỗi tải dữ liệu: ';
          this.notificationService.error(prefix + (err.error?.message || err.message));
        }
        this.isLoading.set(false);
      }
    });
  }

  loadJoinedData(): void {
    this.categoryService.getComplexList().subscribe({
      next: (res) => {
        this.joinedCategories.set(res.data || []);
        this.selectedIds.set([]);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const prefix = this.languageService.labels().messages?.errorPrefix?.loadLinkedData || 'Lỗi tải dữ liệu liên kết: ';
        this.notificationService.error(prefix + (err.error?.message || err.message));
        this.isLoading.set(false);
      }
    });
  }

  // ── View mode (JPA / Native) ───────────────────────────────────────────────
  onTabChange(index: number): void {
    if (index === undefined || index === null || index < 0) return;
    this.activeTabIndex = index;
    const targetMode = index === 0 ? 'jpa' : 'native';
    if (this.viewMode() !== targetMode) this.switchViewMode(targetMode);
  }

  switchViewMode(mode: 'jpa' | 'native'): void {
    this.viewMode.set(mode); this.page.set(0); this.loadData();
  }

  // ── Sort override — category has native/jpa split ─────────────────────────
  override toggleSort(colId: string): void {
    if (this.isResizing || this.justResized || colId === 'checkbox' || colId === 'stt' || colId === 'actions') return;
    const res = computeNextSort(this.sortField(), this.sortDirection(), colId);
    this.sortField.set(res.sortField);
    this.sortDirection.set(res.sortDirection);

    if (this.viewMode() === 'jpa') {
      this.page.set(0); this.loadData();
    } else {
      this.joinedCategories.set(this.sortJoinedCategories([...this.joinedCategories()], res.sortField, res.sortDirection));
    }
  }

  private sortJoinedCategories(list: GroupCategoryResponse[], field: string, dir: string): GroupCategoryResponse[] {
    return list.sort((a, b) => {
      let valA: unknown = (a as unknown as Record<string, unknown>)[field];
      let valB: unknown = (b as unknown as Record<string, unknown>)[field];
      if (field === 'updatedDate') { valA = valA || a.createdDate || ''; valB = valB || b.createdDate || ''; }
      const hasA = valA !== undefined && valA !== null && valA !== '';
      const hasB = valB !== undefined && valB !== null && valB !== '';
      if (!hasA && !hasB) return 0;
      if (!hasA) return 1;
      if (!hasB) return -1;
      if (valA === valB) return 0;
      if (typeof valA === 'string' || typeof valB === 'string') {
        return dir === 'asc' ? String(valA).localeCompare(String(valB)) : String(valB).localeCompare(String(valA));
      }
      const numA = Number(valA); const numB = Number(valB);
      return dir === 'asc' ? (numA > numB ? 1 : -1) : (numB > numA ? 1 : -1);
    });
  }

  // ── Search / Reset ─────────────────────────────────────────────────────────
  override onSearch(): void { this.page.set(0); this.loadData(); }

  onReset(): void {
    this.searchForm.reset({ paramType: '', paramValue: '', paramName: '', status: [], isActive: [] });
    this.page.set(0); this.loadData();
  }

  // ── Navigation ─────────────────────────────────────────────────────────────
  openAddDialog():                              void { this.router.navigate(['/categories/add']); }
  openEditDialog(item: GroupCategoryResponse):  void { this.router.navigate(['/categories/edit', item.id]); }
  openCopyDialog(item: GroupCategoryResponse):  void { this.router.navigate(['/categories/copy', item.id], { state: { data: item } }); }
  onViewDetail(item: GroupCategoryResponse):    void { this.router.navigate(['/categories/detail', item.id], { state: { data: item } }); }

  // ── Export ─────────────────────────────────────────────────────────────────
  onExportExcel(): void {
    this.categoryService.exportExcel().subscribe({
      next: (res) => {
        const data = (res.data || []) as unknown as GroupCategoryResponse[];
        if (data.length === 0) {
          this.notificationService.warning(this.languageService.labels().messages?.warning?.noDataToExport || 'Không có dữ liệu để xuất!');
          return;
        }
        exportToCsv(
          data,
          ['ID', 'Danh mục theo nhóm', 'Giá trị thành phần', 'Tên thành phần', 'Mô tả', 'Trạng thái duyệt', 'Hoạt động', 'Ngày hiệu lực'],
          (row) => [
            row.id, row.paramType, row.paramValue, row.paramName, row.description || '',
            row.status === this.ParamStatus.APPROVED ? 'Đã duyệt' : 'Chưa duyệt',
            row.isActive === this.ActiveStatus.ACTIVE ? 'Hoạt động' : 'Không hoạt động',
            row.effectiveDate
          ],
          'Danh_muc_theo_nhom'
        );
      },
      error: (err: HttpErrorResponse) => {
        const prefix = this.languageService.labels().messages?.errorPrefix?.exportExcel || 'Lỗi xuất dữ liệu: ';
        this.notificationService.error(prefix + (err.error?.message || err.message));
      }
    });
  }
}
