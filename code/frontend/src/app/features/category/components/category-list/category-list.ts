import { Component, OnInit, signal, inject, computed, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CategoryService } from '../../services/category.service';
import { LanguageService } from '../../../../core/services/language.service';
import { AuthService } from '../../../../core/services/auth.service';
import { STATUS_MAP, APPROVAL_STATUS_OPTIONS, IS_ACTIVE_OPTIONS, ACTION_PILL_MAP } from '../../../../shared/constants/status.constants';
import { ParamStatus, ActiveStatus, DisplayStatus } from '../../../../shared/enums/status.enum';
import { parseDateString } from '../../../../shared/utils/date.utils';
import { NotificationService } from '../../../../shared/components/notification/notification.service';
import { GroupCategoryResponse, BatchItemResult } from '../../../../shared/models/group-category.model';
import { AuditLogItem } from '../../../../shared/models/audit-log.model';
import { HttpErrorResponse } from '@angular/common/http';

import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';

interface MappedHistoryItem {
  user: {
    name: string;
    code: string;
    avatar: string;
  };
  date: string;
  action: string;
  ip: string;
  content: string;
}

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    SharedTaigaModule
  ],
  templateUrl: './category-list.html',
  styleUrl: './category-list.css'
})
export class CategoryListComponent implements OnInit {
  readonly ParamStatus = ParamStatus;
  readonly ActiveStatus = ActiveStatus;
  readonly DisplayStatus = DisplayStatus;

  private fb = inject(FormBuilder);
  private categoryService = inject(CategoryService);
  public languageService = inject(LanguageService);
  public authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  searchForm: FormGroup = inject(FormBuilder).group({
    paramType: [''],
    paramValue: [''],
    paramName: [''],
    status: [[]],
    isActive: [[]]
  });

  categories = signal<GroupCategoryResponse[]>([]);
  totalElements = signal<number>(0);
  page = signal<number>(0);
  size = signal<number>(10);
  totalPages = computed(() => Math.ceil(this.totalElements() / this.size()) || 1);
  sortField = signal<string>('updatedDate');
  sortDirection = signal<string>('desc');

  viewMode = signal<'jpa' | 'native'>('jpa');
  activeTabIndex = 0;

  joinedCategories = signal<GroupCategoryResponse[]>([]);
  selectedIds = signal<number[]>([]);
  isLoading = signal<boolean>(false);

  statusMap = STATUS_MAP;

  constructor() {
    console.log('[CategoryListComponent] Constructor - searchForm status:', this.searchForm ? 'defined' : 'undefined');
    console.log('[CategoryListComponent] Constructor - viewMode:', this.viewMode());
  }

  readonly statusCodes = ['1', '3', '4', '5', '7'];
  readonly activeCodes = ['1', '0'];

  readonly stringifyStatus = (val: string): string => {
    if (!val) return '';
    const labels = this.languageService.labels();
    const map: Record<string, string> = {
      '1': labels.common.status.new,
      '3': labels.common.status.pending,
      '4': labels.common.status.approved,
      '5': labels.common.status.rejected,
      '7': labels.common.status.canceled
    };
    return map[String(val)] || String(val);
  };

  readonly stringifyActive = (val: string): string => {
    if (val === null || val === undefined || val === '') return '';
    const labels = this.languageService.labels();
    const map: Record<string, string> = {
      '1': labels.common.active,
      '0': labels.common.inactive
    };
    return map[String(val)] || String(val);
  };

  getColumnLabel(id: string): string {
    const labels = this.languageService.labels();
    const map: Record<string, string> = {
      stt: labels.common.stt,
      paramType: labels.category.groupName,
      paramValue: labels.category.paramValue,
      paramName: labels.category.paramName,
      description: labels.category.description,
      componentCode: labels.category.componentCode,
      effectiveDate: labels.category.effectiveDate,
      endEffectiveDate: labels.category.endEffectiveDate,
      status: labels.category.paramStatus,
      isActive: labels.category.activeStatus,
      actions: labels.common.actions
    };
    return map[id] || id;
  }

  columns = [
    { id: 'checkbox', label: '', isFixed: true, width: 45 },
    { id: 'stt', label: 'STT', isFixed: true, width: 60 },
    { id: 'paramType', label: 'Danh mục theo nhóm', isFixed: true, width: 175 },
    { id: 'paramValue', label: 'Giá trị thành phần', isFixed: true, width: 155 },
    { id: 'paramName', label: 'Tên thành phần', isFixed: true, width: 170 },
    { id: 'description', label: 'Mô tả', isFixed: false, width: 150 },
    { id: 'componentCode', label: 'Cấu phần xử lý', isFixed: false, width: 150 },
    { id: 'effectiveDate', label: 'Hiệu lực', isFixed: false, width: 125 },
    { id: 'endEffectiveDate', label: 'Hết hiệu lực', isFixed: false, width: 125 },
    { id: 'status', label: 'Trạng thái tham số', isFixed: false, width: 180 },
    { id: 'isActive', label: 'Tình trạng hoạt động', isFixed: false, width: 180 },
    { id: 'actions', label: 'Thao tác', isFixed: false, width: 250 }
  ];

  get displayColumns() {
    if (this.languageService.isMaker()) {
      return this.columns.filter(c => c.id !== 'checkbox');
    }
    return this.columns;
  }

  isLastColumn(colId: string): boolean {
    const cols = this.displayColumns;
    return cols.length > 0 && cols[cols.length - 1].id === colId;
  }

  draggedColumnIndex: number | null = null;
  dragOverColumnIndex: number | null = null;
  isResizing = false;
  justResized = false;

  onResizeStart(event: MouseEvent, col: any) {
    event.stopPropagation();
    event.preventDefault();
    this.isResizing = true;
    this.justResized = true;
    const startX = event.clientX;
    const startWidth = col.width;

    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';

    let rafId: number | null = null;

    const onMouseMove = (moveEvent: MouseEvent) => {
      const deltaX = moveEvent.clientX - startX;
      const minWidth = col.id === 'checkbox' ? 40 : 80;
      const newWidth = Math.max(minWidth, startWidth + deltaX);

      if (rafId) cancelAnimationFrame(rafId);
      rafId = requestAnimationFrame(() => {
        col.width = newWidth;
        this.cdr.markForCheck();
      });
    };

    const onMouseUp = () => {
      if (rafId) cancelAnimationFrame(rafId);
      this.isResizing = false;
      setTimeout(() => {
        this.justResized = false;
      }, 150);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
      this.cdr.detectChanges();
    };

    document.addEventListener('mousemove', onMouseMove, { passive: true });
    document.addEventListener('mouseup', onMouseUp, { once: true });
  }

  onDragStart(colId: string, event: DragEvent) {
    const index = this.columns.findIndex(c => c.id === colId);
    if (this.isResizing || this.justResized || index === -1 || this.columns[index].isFixed) {
      event.preventDefault();
      return;
    }
    this.draggedColumnIndex = index;
  }

  onDragOver(event: DragEvent, colId: string) {
    const index = this.columns.findIndex(c => c.id === colId);
    if (index === -1 || this.columns[index].isFixed || this.draggedColumnIndex === null) return;
    event.preventDefault();
    this.dragOverColumnIndex = index;
  }

  onDragLeave() {
    this.dragOverColumnIndex = null;
  }

  onDrop(colId: string) {
    const index = this.columns.findIndex(c => c.id === colId);
    if (this.draggedColumnIndex === null || index === -1 || this.columns[index].isFixed) {
      this.draggedColumnIndex = null;
      this.dragOverColumnIndex = null;
      return;
    }

    const draggedCol = this.columns[this.draggedColumnIndex];
    const updated = [...this.columns];
    updated.splice(this.draggedColumnIndex, 1);
    updated.splice(index, 0, draggedCol);
    this.columns = updated;

    this.draggedColumnIndex = null;
    this.dragOverColumnIndex = null;
  }

  toggleSort(colId: string) {
    if (this.isResizing || this.justResized || colId === 'checkbox' || colId === 'stt' || colId === 'actions') return;

    const currentField = this.sortField();
    const currentDir = this.sortDirection();

    let newField = colId;
    let newDir = 'asc';

    if (currentField === colId) {
      if (currentDir === 'asc') {
        newDir = 'desc';
      } else {
        if (colId !== 'updatedDate') {
          newField = 'updatedDate';
          newDir = 'desc';
        } else {
          newDir = 'asc';
        }
      }
    }

    this.sortField.set(newField);
    this.sortDirection.set(newDir);

    if (this.viewMode() === 'jpa') {
      this.page.set(0);
      this.loadData();
    } else {
      const list = [...this.joinedCategories()];
      list.sort((a: GroupCategoryResponse, b: GroupCategoryResponse) => {
        let valA = a[newField as keyof GroupCategoryResponse];
        let valB = b[newField as keyof GroupCategoryResponse];
        if (valA === undefined || valA === null) valA = '';
        if (valB === undefined || valB === null) valB = '';

        if (valA === valB) return 0;

        if (typeof valA === 'string' || typeof valB === 'string') {
          return newDir === 'asc'
            ? String(valA).localeCompare(String(valB))
            : String(valB).localeCompare(String(valA));
        } else {
          return newDir === 'asc'
            ? (valA > valB ? 1 : -1)
            : (valB > valA ? 1 : -1);
        }
      });
      this.joinedCategories.set(list);
    }
  }

  trackById(index: number, item: GroupCategoryResponse): any {
    return item.id;
  }

  trackByHistoryId(index: number, item: any): any {
    return item.id;
  }

  trackByColId(index: number, col: any): any {
    return col.id;
  }

  ngOnInit() {
    console.log('[CategoryListComponent] ngOnInit - loading data...');
    const savedState = this.categoryService.getListState();
    if (savedState) {
      this.viewMode.set(savedState.viewMode);
      this.activeTabIndex = savedState.activeTabIndex;
      this.page.set(savedState.page);
      this.size.set(savedState.size);
      this.searchForm.patchValue(savedState.filters);
    }
    this.loadData();
  }

  loadData() {
    console.log('[CategoryListComponent] loadData - viewMode:', this.viewMode());
    this.categoryService.setListState({
      page: this.page(),
      size: this.size(),
      filters: this.searchForm.value,
      viewMode: this.viewMode(),
      activeTabIndex: this.activeTabIndex
    });

    this.isLoading.set(true);

    if (this.viewMode() === 'native') {
      this.loadJoinedData();
      return;
    }

    const rawFilters = this.searchForm.value;

    let statusList: number[] = [];
    if (Array.isArray(rawFilters.status)) {
      statusList = rawFilters.status.map((s: any) => Number(s)).filter((n: number) => !isNaN(n));
    } else if (rawFilters.status) {
      statusList = [Number(rawFilters.status)];
    }

    let activeList: number[] = [];
    if (Array.isArray(rawFilters.isActive)) {
      activeList = rawFilters.isActive.map((a: any) => Number(a)).filter((n: number) => !isNaN(n));
    } else if (rawFilters.isActive) {
      activeList = [Number(rawFilters.isActive)];
    }

    const filters = {
      paramType: rawFilters.paramType,
      paramValue: rawFilters.paramValue,
      paramName: rawFilters.paramName,
      status: statusList,
      isActive: activeList
    };
    const sortParam = `${this.sortField()},${this.sortDirection()}`;

    this.categoryService.search(filters, this.page(), this.size(), sortParam).subscribe({
      next: (res) => {
        console.log('[CategoryListComponent] search success, res:', res);
        const content = res.data.content || [];
        this.categories.set(content);
        this.totalElements.set(res.data.page?.totalElements ?? res.data.totalElements ?? 0);
        this.selectedIds.set([]);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status !== 401 && err.status !== 403 && this.authService.isLoggedIn()) {
          const prefix = this.languageService.labels().messages?.errorPrefix?.loadData || 'Lỗi tải dữ liệu: ';
          this.notificationService.error(prefix + (err.error?.message || err.message));
        }
        this.isLoading.set(false);
      }
    });
  }

  loadJoinedData() {
    this.categoryService.getComplexList().subscribe({
      next: (res) => {
        console.log('[CategoryListComponent] loadJoinedData success, res:', res);
        const content = res.data || [];
        this.joinedCategories.set(content);
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

  onTabChange(index: number) {
    if (index === undefined || index === null || index < 0) return;
    this.activeTabIndex = index;
    const targetMode = index === 0 ? 'jpa' : 'native';
    if (this.viewMode() !== targetMode) {
      this.switchViewMode(targetMode);
    }
  }

  switchViewMode(mode: 'jpa' | 'native') {
    this.viewMode.set(mode);
    this.page.set(0);
    this.loadData();
  }

  onSearch() {
    this.page.set(0);
    this.loadData();
  }

  onReset() {
    this.searchForm.reset({
      paramType: '',
      paramValue: '',
      paramName: '',
      status: [],
      isActive: []
    });
    this.page.set(0);
    this.loadData();
  }

  setPage(p: number) {
    this.page.set(p);
    this.loadData();
  }

  openAddDialog() {
    this.router.navigate(['/categories/add']);
  }

  openEditDialog(item: GroupCategoryResponse) {
    this.router.navigate(['/categories/edit', item.id]);
  }

  openCopyDialog(item: GroupCategoryResponse) {
    this.router.navigate(['/categories/copy', item.id], { state: { data: item } });
  }

  isConfirmOpen = false;
  confirmTitle = '';
  confirmMessage = '';
  confirmAction: 'approve' | 'reject' | 'delete' | 'sendApproval' | 'cancelApproval' | 'batchApprove' | 'batchReject' | null = null;
  confirmTargetId: number | null = null;

  onDelete(id: number) {
    this.confirmTitle = 'Xóa bản ghi';
    this.confirmMessage = 'Bạn có chắc chắn muốn xóa bản ghi này?';
    this.confirmAction = 'delete';
    this.confirmTargetId = id;
    this.isConfirmOpen = true;
  }

  onSendApproval(id: number) {
    this.confirmTitle = 'Gửi duyệt';
    this.confirmMessage = 'Bạn có chắc chắn muốn gửi duyệt bản ghi này?';
    this.confirmAction = 'sendApproval';
    this.confirmTargetId = id;
    this.isConfirmOpen = true;
  }

  onCancelApproval(id: number) {
    this.confirmTitle = 'Hủy duyệt';
    this.confirmMessage = 'Bạn có chắc chắn muốn hủy duyệt bản ghi này? Trạng thái sẽ chuyển về Hủy duyệt (7).';
    this.confirmAction = 'cancelApproval';
    this.confirmTargetId = id;
    this.isConfirmOpen = true;
  }

  onViewDetail(item: GroupCategoryResponse) {
    this.router.navigate(['/categories/detail', item.id], { state: { data: item } });
  }

  isRejectOpen = false;
  rejectReason = '';
  rejectTargetIds: number[] = [];

  isHistoryOpen = false;
  historyTargetId: number | null = null;
  historyData = signal<MappedHistoryItem[]>([]);
  historyTargetName = '';
  historyPage = signal<number>(0);
  readonly historyPageSize = 5;
  historyTotalPages = signal<number>(1);

  paginatedHistoryData = computed(() => this.historyData());

  onBatchApprove() {
    const ids = this.selectedIds();
    if (ids.length === 0) {
      const warnMsg = this.languageService.labels().messages?.warning?.selectAtLeastOneToApprove || 'Vui lòng chọn ít nhất một bản ghi để duyệt!';
      this.notificationService.warning(warnMsg);
      return;
    }
    this.confirmTitle = 'Phê duyệt';
    this.confirmMessage = ids.length === 1 ? 'Bạn có chắc chắn phê duyệt bản ghi này?' : `Bạn có chắc chắn muốn duyệt hàng loạt ${ids.length} bản ghi đã chọn?`;
    this.confirmAction = 'batchApprove';
    this.isConfirmOpen = true;
  }

  onBatchReject() {
    const ids = this.selectedIds();
    if (ids.length === 0) {
      const warnMsg = this.languageService.labels().messages?.warning?.selectAtLeastOneToReject || 'Vui lòng chọn ít nhất một bản ghi để từ chối!';
      this.notificationService.warning(warnMsg);
      return;
    }
    this.rejectTargetIds = ids;
    this.rejectReason = '';
    this.isRejectOpen = true;
  }

  onRejectReasonInput(val: string) {
    this.rejectReason = val;
  }

  onConfirmReject() {
    const ids = this.rejectTargetIds;
    const reason = this.rejectReason.trim();
    this.isRejectOpen = false;

    if (ids.length === 0) return;
    this.isLoading.set(true);

    this.categoryService.batchReject(ids, reason).subscribe({
      next: (res) => {
        const successCount = (res.data || []).filter((r: BatchItemResult) => r.status === 'SUCCESS').length;
        this.notificationService.success(`Đã từ chối thành công ${successCount}/${ids.length} bản ghi! Lý do: ${reason}`);
        this.loadData();
      },
      error: (err: HttpErrorResponse) => {
        const prefix = this.languageService.labels().messages?.errorPrefix?.reject || 'Lỗi thực hiện từ chối duyệt: ';
        this.notificationService.error(prefix + (err.error?.message || err.message));
        this.isLoading.set(false);
      }
    });
  }

  openHistoryDialog(item: GroupCategoryResponse) {
    this.historyTargetId = item.id;
    this.historyTargetName = item.paramName || item.paramValue;
    this.historyPage.set(0);
    this.loadHistoryData();
  }

  onHistoryPageChange(page: number) {
    this.historyPage.set(page);
    this.loadHistoryData();
  }

  loadHistoryData() {
    if (this.historyTargetId === null) return;
    this.categoryService.getHistory(this.historyTargetId, this.historyPage(), this.historyPageSize).subscribe({
      next: (res) => {
        const pageData = res.data;
        const mapped = (pageData.content || []).map((log: AuditLogItem) => {
          const name = log.performedBy || 'SYSTEM';
          const avatar = name.substring(0, 2).toUpperCase();

          const d = new Date(log.actionDate);
          const pad = (n: number) => n.toString().padStart(2, '0');
          const dateStr = isNaN(d.getTime()) ? '-' :
            `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;

          return {
            user: {
              name: name,
              code: name === 'SYSTEM' ? 'Hệ thống' : 'Mã CB: ' + name,
              avatar: avatar
            },
            date: dateStr,
            action: log.action,
            ip: log.ipAddress || '127.0.0.1',
            content: log.description
          };
        });
        this.historyData.set(mapped);

        const total = pageData.page?.totalPages ?? pageData.totalPages ?? 1;
        this.historyTotalPages.set(total);

        this.isHistoryOpen = true;
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        const prefix = this.languageService.labels().messages?.errorPrefix?.history || 'Không thể tải lịch sử thao tác: ';
        this.notificationService.error(prefix + (err.error?.message || err.message));
      }
    });
  }

  getActionPillClass(action: string): string {
    return ACTION_PILL_MAP[action] || 'pill-default';
  }

  onConfirmExecute() {
    const action = this.confirmAction;
    const id = this.confirmTargetId;
    this.isConfirmOpen = false;
    this.isLoading.set(true);

    const msgs = this.languageService.labels().messages;

    if (action === 'delete' && id !== null) {
      this.categoryService.delete(id).subscribe({
        next: () => {
          this.notificationService.success(msgs?.success?.delete || 'Xóa thành công!');
          this.loadData();
        },
        error: (err: HttpErrorResponse) => {
          this.notificationService.error((msgs?.errorPrefix?.delete || 'Không thể xóa: ') + (err.error?.message || err.message));
          this.isLoading.set(false);
        }
      });
    } else if (action === 'sendApproval' && id !== null) {
      this.categoryService.sendApproval(id).subscribe({
        next: () => {
          this.notificationService.success(msgs?.success?.sendApproval || 'Gửi duyệt thành công!');
          this.loadData();
        },
        error: (err: HttpErrorResponse) => {
          this.notificationService.error((msgs?.errorPrefix?.sendApproval || 'Lỗi gửi duyệt: ') + (err.error?.message || err.message));
          this.isLoading.set(false);
        }
      });
    } else if (action === 'cancelApproval' && id !== null) {
      this.categoryService.cancelApproval(id).subscribe({
        next: () => {
          this.notificationService.success(msgs?.success?.cancelApproval || 'Hủy duyệt thành công!');
          this.loadData();
        },
        error: (err: HttpErrorResponse) => {
          this.notificationService.error((msgs?.errorPrefix?.cancelApproval || 'Lỗi hủy duyệt: ') + (err.error?.message || err.message));
          this.isLoading.set(false);
        }
      });
    } else if (action === 'batchApprove') {
      const ids = this.selectedIds();
      this.categoryService.batchApprove(ids).subscribe({
        next: (res) => {
          const successCount = (res.data || []).filter((r: BatchItemResult) => r.status === 'SUCCESS').length;
          this.notificationService.success(`Đã duyệt thành công ${successCount}/${ids.length} bản ghi!`);
          this.loadData();
        },
        error: (err: HttpErrorResponse) => {
          this.notificationService.error((msgs?.errorPrefix?.batchApprove || 'Lỗi duyệt hàng loạt: ') + (err.error?.message || err.message));
          this.isLoading.set(false);
        }
      });
    }
  }

  onExportExcel() {
    this.categoryService.exportExcel().subscribe({
      next: (res) => {
        const data = (res.data || []) as unknown as GroupCategoryResponse[];
        if (data.length === 0) {
          const warnMsg = this.languageService.labels().messages?.warning?.noDataToExport || 'Không có dữ liệu để xuất!';
          this.notificationService.warning(warnMsg);
          return;
        }

        let csvContent = 'data:text/csv;charset=utf-8,\uFEFF';
        csvContent += 'ID,Danh mục theo nhóm,Giá trị thành phần,Tên thành phần,Mô tả,Trạng thái duyệt,Hoạt động,Ngày hiệu lực\n';

        data.forEach((row: GroupCategoryResponse) => {
          const statusLabel = row.status === ParamStatus.APPROVED ? 'Đã duyệt' : 'Chưa duyệt';
          const activeLabel = row.isActive === ActiveStatus.ACTIVE ? 'Hoạt động' : 'Không hoạt động';
          csvContent += `"${row.id}","${row.paramType}","${row.paramValue}","${row.paramName}","${row.description || ''}","${statusLabel}","${activeLabel}","${row.effectiveDate}"\n`;
        });

        const encodedUri = encodeURI(csvContent);
        const link = document.createElement('a');
        link.setAttribute('href', encodedUri);
        link.setAttribute('download', 'Danh_muc_theo_nhom_' + new Date().getTime() + '.csv');
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      },
      error: (err: HttpErrorResponse) => {
        const prefix = this.languageService.labels().messages?.errorPrefix?.exportExcel || 'Lỗi xuất dữ liệu: ';
        this.notificationService.error(prefix + (err.error?.message || err.message));
      }
    });
  }

  get statusOptions() { return APPROVAL_STATUS_OPTIONS; }
  get activeOptions() { return IS_ACTIVE_OPTIONS; }

  toggleSelectAll(event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      const ids = this.categories().map(c => c.id);
      this.selectedIds.set(ids);
    } else {
      this.selectedIds.set([]);
    }
  }

  toggleItemSelection(id: number, event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      this.selectedIds.update(ids => [...ids, id]);
    } else {
      this.selectedIds.update(ids => ids.filter(x => x !== id));
    }
  }

  isAllSelected(): boolean {
    const categories = this.categories();
    if (categories.length === 0) return false;
    return this.selectedIds().length === categories.length;
  }
}
