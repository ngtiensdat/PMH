import { Component, OnInit, signal, inject, computed, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { ComponentService } from '../../services/component.service';
import { LanguageService } from '../../../../core/services/language.service';
import { STATUS_MAP, APPROVAL_STATUS_OPTIONS, IS_ACTIVE_OPTIONS, ACTION_PILL_MAP } from '../../../../shared/constants/status.constants';
import { parseDateString } from '../../../../shared/utils/date.utils';
import { NotificationService } from '../../../../shared/components/notification/notification.service';
import { ProcessingComponentResponse } from '../../../../shared/models/component.model';
import { AuditLogItem } from '../../../../shared/models/audit-log.model';
import { HttpErrorResponse } from '@angular/common/http';

import { SharedTaigaModule } from '../../../../shared/shared-taiga.module';

@Component({
  selector: 'app-component-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    SharedTaigaModule
  ],
  templateUrl: './component-list.html',
  styleUrl: './component-list.css'
})
export class ComponentListComponent implements OnInit {
  private fb = inject(FormBuilder);
  private componentService = inject(ComponentService);
  public languageService = inject(LanguageService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  searchForm: FormGroup = inject(FormBuilder).group({
    componentCode: [''],
    componentName: [''],
    status: [''],
    isActive: ['']
  });

  components = signal<ProcessingComponentResponse[]>([]);
  totalElements = signal<number>(0);
  page = signal<number>(0);
  size = signal<number>(10);
  totalPages = computed(() => Math.ceil(this.totalElements() / this.size()) || 1);
  sortField = signal<string>('updatedDate');
  sortDirection = signal<string>('desc');

  selectedCodes = signal<string[]>([]);
  isLoading = signal<boolean>(false);

  componentCodesList: { value: string; label: string }[] = [];
  componentNamesList: { value: string; label: string }[] = [];

  constructor() {
    console.log('[ComponentListComponent] Constructor - searchForm status:', this.searchForm ? 'defined' : 'undefined');
  }

  // Dùng shared STATUS_MAP từ constants
  statusMap = STATUS_MAP;

  // Dropdown items cho bộ lọc (dạng string code)
  readonly statusCodes = ['', '1', '3', '4', '5', '7'];
  readonly activeCodes = ['', '1', '0'];

  stringifyStatus = (val: string): string => {
    const labels = this.languageService.labels();
    const map: Record<string, string> = {
      '': labels.common.all || 'Tất cả',
      '1': labels.common.status.new,
      '3': labels.common.status.pending,
      '4': labels.common.status.approved,
      '5': labels.common.status.rejected,
      '7': labels.common.status.canceled
    };
    return map[val] || val;
  };

  stringifyActive = (val: string): string => {
    const labels = this.languageService.labels();
    const map: Record<string, string> = {
      '': labels.common.all || 'Tất cả',
      '1': labels.common.active,
      '0': labels.common.inactive
    };
    return map[val] || val;
  };

  getColumnLabel(id: string): string {
    const labels = this.languageService.labels();
    const map: Record<string, string> = {
      stt: labels.common.stt,
      componentCode: labels.components.code,
      componentName: labels.components.name,
      messageType: labels.components.messageType,
      connectionMethod: labels.components.connectionMethod,
      checkToken: labels.components.checkToken,
      status: labels.components.paramStatus,
      isActive: labels.components.activeStatus,
      actions: labels.common.actions
    };
    return map[id] || id;
  }

  // Dynamic getters for component code/name dropdown items (plain strings)
  get componentCodeItems(): string[] {
    return [this.languageService.labels().common.selectValue, ...this.componentCodesList.map(o => o.value)];
  }

  get componentNameItems(): string[] {
    return [this.languageService.labels().common.selectValue, ...this.componentNamesList.map(o => o.value)];
  }

  // Dynamic columns definition with generous default widths
  columns = [
    { id: 'checkbox', label: '', isFixed: true, width: 40 },
    { id: 'stt', label: 'STT', isFixed: true, width: 60 },
    { id: 'componentCode', label: 'Mã cấu phần', isFixed: true, width: 160 },
    { id: 'componentName', label: 'Tên cấu phần', isFixed: true, width: 220 },
    { id: 'messageType', label: 'Chuẩn tin điện', isFixed: false, width: 160 },
    { id: 'connectionMethod', label: 'Tên kết nối', isFixed: false, width: 160 },
    { id: 'checkToken', label: 'Kiểm tra Token/ký số', isFixed: false, width: 180 },
    { id: 'status', label: 'Trạng thái tham số', isFixed: false, width: 180 },
    { id: 'isActive', label: 'Tình trạng hoạt động', isFixed: false, width: 180 },
    { id: 'actions', label: 'Thao tác', isFixed: false, width: 250 }
  ];

  get displayColumns() {
    if (this.languageService.userCode() === 'USER01') {
      return this.columns.filter(c => c.id !== 'checkbox');
    }
    return this.columns;
  }

  draggedColumnIndex: number | null = null;
  dragOverColumnIndex: number | null = null;

  // Column resizing implementation
  onResizeStart(event: MouseEvent, index: number) {
    event.stopPropagation();
    event.preventDefault();
    const startX = event.clientX;
    const startWidth = this.columns[index].width;
    
    const onMouseMove = (moveEvent: MouseEvent) => {
      const deltaX = moveEvent.clientX - startX;
      const minWidth = this.columns[index].id === 'checkbox' ? 40 : 80;
      this.columns[index].width = Math.max(minWidth, startWidth + deltaX);
    };
    
    const onMouseUp = () => {
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
    };
    
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  }

  // Column reordering implementation
  onDragStart(index: number) {
    if (this.columns[index].isFixed) return;
    this.draggedColumnIndex = index;
  }

  onDragOver(event: DragEvent, index: number) {
    if (this.columns[index].isFixed || this.draggedColumnIndex === null) return;
    event.preventDefault();
    this.dragOverColumnIndex = index;
  }

  onDragLeave() {
    this.dragOverColumnIndex = null;
  }

  onDrop(index: number) {
    if (this.draggedColumnIndex === null || this.columns[index].isFixed) {
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

  // Column sorting implementation
  toggleSort(colId: string) {
    if (colId === 'checkbox' || colId === 'stt' || colId === 'actions') return;

    const currentField = this.sortField();
    const currentDir = this.sortDirection();

    let newDir = 'asc';
    if (currentField === colId) {
      newDir = currentDir === 'asc' ? 'desc' : 'asc';
    }

    this.sortField.set(colId);
    this.sortDirection.set(newDir);

    this.page.set(0);
    this.loadData();
  }

  ngOnInit() {
    console.log('[ComponentListComponent] ngOnInit - loading filter options and data...');
    this.loadFilterOptions();
    const savedState = this.componentService.getListState();
    if (savedState) {
      this.page.set(savedState.page);
      this.size.set(savedState.size);
      this.searchForm.patchValue(savedState.filters);
    }
    this.loadData();
  }

  private loadFilterOptions() {
    this.componentService.getActiveList().subscribe({
      next: (res) => {
        console.log('[ComponentListComponent] loadFilterOptions success, res:', res);
        const list = res.data || [];
        this.componentCodesList = list.map(c => ({ value: c.componentCode, label: c.componentCode }));
        this.componentNamesList = list.map(c => ({ value: c.componentName, label: c.componentName }));
      },
      error: (err) => {
        console.error('Error loading filter options:', err);
        this.componentCodesList = [];
        this.componentNamesList = [];
      }
    });
  }

  loadData() {
    console.log('[ComponentListComponent] loadData called');
    // Auto save list state
    this.componentService.setListState({
      page: this.page(),
      size: this.size(),
      filters: this.searchForm.value,
      viewMode: 'jpa',
      activeTabIndex: 0
    });

    this.isLoading.set(true);

    const rawFilters = this.searchForm.value;
    const selectPlaceholder = this.languageService.labels().common.selectValue;
    const filters = {
      componentCode: rawFilters.componentCode === selectPlaceholder ? '' : rawFilters.componentCode,
      componentName: rawFilters.componentName === selectPlaceholder ? '' : rawFilters.componentName,
      status: rawFilters.status ? [Number(rawFilters.status)] : [],
      isActive: rawFilters.isActive ? [Number(rawFilters.isActive)] : []
    };
    const sortParam = `${this.sortField()},${this.sortDirection()}`;

    this.componentService.search(filters, this.page(), this.size(), sortParam).subscribe({
      next: (res) => {
        console.log('[ComponentListComponent] search success, res:', res);
        const content = (res.data.content || []).map((item: ProcessingComponentResponse) => {
          if (item.newData) {
            try {
              const parsed = typeof item.newData === 'string' ? JSON.parse(item.newData) : item.newData;
              if (parsed) {
                if (parsed.effectiveDate) parsed.effectiveDate = parseDateString(parsed.effectiveDate);
                if (parsed.endEffectiveDate) parsed.endEffectiveDate = parseDateString(parsed.endEffectiveDate);
                return { ...item, ...parsed };
              }
            } catch (e) {
              console.error('Error parsing component newData:', e);
            }
          }
          return item;
        });
        this.components.set(content);
        this.totalElements.set(res.data.page?.totalElements ?? res.data.totalElements ?? 0);
        this.selectedCodes.set([]);
        this.isLoading.set(false);
      },
      error: (err: any) => {
        this.notificationService.error('Lỗi tải dữ liệu: ' + (err.error?.message || err.message));
        this.isLoading.set(false);
      }
    });
  }

  onSearch() {
    this.page.set(0);
    this.loadData();
  }

  onReset() {
    this.searchForm.reset({
      componentCode: '',
      componentName: '',
      status: '',
      isActive: ''
    });
    this.page.set(0);
    this.loadData();
  }

  setPage(p: number) {
    this.page.set(p);
    this.loadData();
  }

  openAddDialog() {
    this.router.navigate(['/components/add']);
  }

  openEditDialog(item: any) {
    this.router.navigate(['/components/edit', item.componentCode]);
  }

  openCopyDialog(item: any) {
    this.router.navigate(['/components/copy', item.componentCode]);
  }

  onViewDetail(item: any) {
    this.router.navigate(['/components/detail', item.componentCode]);
  }

  // Confirmation dialog state
  isConfirmOpen = false;
  confirmTitle = '';
  confirmMessage = '';
  confirmAction: 'approve' | 'reject' | 'delete' | 'sendApproval' | 'batchApprove' | 'batchReject' | null = null;
  confirmTargetCode: string | null = null;

  onDelete(code: string) {
    this.confirmTitle = 'Xóa cấu phần';
    this.confirmMessage = 'Bạn có chắc chắn muốn xóa cấu phần xử lý này?';
    this.confirmAction = 'delete';
    this.confirmTargetCode = code;
    this.isConfirmOpen = true;
  }

  onSendApproval(code: string) {
    this.confirmTitle = 'Gửi duyệt';
    this.confirmMessage = 'Bạn có chắc chắn muốn gửi duyệt cấu phần này?';
    this.confirmAction = 'sendApproval';
    this.confirmTargetCode = code;
    this.isConfirmOpen = true;
  }

  // Reject Dialog state
  isRejectOpen = false;
  rejectReason = '';
  rejectTargetCodes: string[] = [];

  // History Dialog state
  isHistoryOpen = false;
  historyData = signal<any[]>([]);
  historyTargetName = '';
  historyPage = signal<number>(0);
  readonly historyPageSize = 5;

  paginatedHistoryData = computed(() => {
    const data = this.historyData();
    const pageVal = this.historyPage();
    const sizeVal = this.historyPageSize;
    const startIndex = pageVal * sizeVal;
    return data.slice(startIndex, startIndex + sizeVal);
  });

  historyTotalPages = computed(() => {
    return Math.ceil(this.historyData().length / this.historyPageSize) || 1;
  });

  onBatchApprove() {
    const codes = this.selectedCodes();
    if (codes.length === 0) {
      this.notificationService.warning('Vui lòng chọn ít nhất một cấu phần để duyệt!');
      return;
    }
    this.confirmTitle = 'Phê duyệt';
    this.confirmMessage = codes.length === 1 ? 'Bạn có chắc chắn phê duyệt bản ghi này?' : `Bạn có chắc chắn muốn duyệt hàng loạt ${codes.length} cấu phần đã chọn?`;
    this.confirmAction = 'batchApprove';
    this.isConfirmOpen = true;
  }

  onBatchReject() {
    const codes = this.selectedCodes();
    if (codes.length === 0) {
      this.notificationService.warning('Vui lòng chọn ít nhất một cấu phần để từ chối!');
      return;
    }
    this.rejectTargetCodes = codes;
    this.rejectReason = '';
    this.isRejectOpen = true;
  }

  onRejectReasonInput(val: string) {
    this.rejectReason = val;
  }

  onConfirmReject() {
    const codes = this.rejectTargetCodes;
    const reason = this.rejectReason.trim();
    this.isRejectOpen = false;

    if (codes.length === 0) return;

    this.componentService.batchReject(codes, reason).subscribe({
      next: (res) => {
        const successCount = (res.data || []).filter((r: any) => r['success']).length;
        this.notificationService.success(`Đã từ chối thành công ${successCount}/${codes.length} cấu phần! Lý do: ${reason}`);
        this.loadData();
      },
      error: (err: any) => {
        this.notificationService.error('Lỗi thực hiện từ chối duyệt: ' + (err.error?.message || err.message));
      }
    });
  }

  openHistoryDialog(item: ProcessingComponentResponse) {
    this.historyTargetName = item.componentName || item.componentCode;
    this.componentService.getHistory(item.componentCode).subscribe({
      next: (res) => {
        const mapped = (res.data || []).map((log: AuditLogItem) => {
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
        this.historyPage.set(0);
        this.isHistoryOpen = true;
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.notificationService.error('Không thể tải lịch sử thao tác: ' + (err.error?.message || err.message));
      }
    });
  }

  getActionPillClass(action: string): string {
    return ACTION_PILL_MAP[action] || 'pill-default';
  }

  onConfirmExecute() {
    const action = this.confirmAction;
    const code = this.confirmTargetCode;
    this.isConfirmOpen = false;

    if (action === 'delete' && code) {
      this.componentService.delete(code).subscribe({
        next: () => {
          this.notificationService.success('Xóa thành công!');
          this.loadData();
        },
        error: (err: any) => {
          this.notificationService.error('Không thể xóa: ' + (err.error?.message || err.message));
        }
      });
    } else if (action === 'sendApproval' && code) {
      this.componentService.sendApproval(code).subscribe({
        next: () => {
          this.notificationService.success('Gửi duyệt thành công!');
          this.loadData();
        },
        error: (err: any) => {
          this.notificationService.error('Lỗi gửi duyệt: ' + (err.error?.message || err.message));
        }
      });
    } else if (action === 'batchApprove') {
      const codes = this.selectedCodes();
      this.componentService.batchApprove(codes).subscribe({
        next: (res) => {
          const successCount = (res.data || []).filter((r: any) => r['success']).length;
          this.notificationService.success(`Đã duyệt thành công ${successCount}/${codes.length} cấu phần!`);
          this.loadData();
        },
        error: (err: any) => {
          this.notificationService.error('Lỗi duyệt hàng loạt: ' + (err.error?.message || err.message));
        }
      });
    }
  }

  onExportExcel() {
    this.componentService.exportExcel().subscribe({
      next: (res) => {
        const data = (res.data || []) as unknown as ProcessingComponentResponse[];
        if (data.length === 0) {
          this.notificationService.warning('Không có dữ liệu cấu phần để xuất!');
          return;
        }
        
        let csvContent = 'data:text/csv;charset=utf-8,\uFEFF';
        csvContent += 'Mã cấu phần,Tên cấu phần,Chuẩn tin điện,Phương thức kết nối,Kiểm tra Token,Trạng thái duyệt,Hoạt động,Ngày hiệu lực\n';
        
        data.forEach((row: ProcessingComponentResponse) => {
          const statusLabel = row.status === 4 ? 'Đã duyệt' : 'Chưa duyệt';
          const activeLabel = row.isActive === 1 ? 'Hoạt động' : 'Không hoạt động';
          csvContent += `"${row.componentCode}","${row.componentName}","${row.messageType || ''}","${row.connectionMethod || ''}","${row.checkToken}","${statusLabel}","${activeLabel}","${row.effectiveDate}"\n`;
        });

        const encodedUri = encodeURI(csvContent);
        const link = document.createElement('a');
        link.setAttribute('href', encodedUri);
        link.setAttribute('download', 'Cau_phan_xu_ly_' + new Date().getTime() + '.csv');
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      },
      error: (err: HttpErrorResponse) => {
        this.notificationService.error('Lỗi xuất Excel: ' + (err.error?.message || err.message));
      }
    });
  }

  // Options getters
  get statusOptions() { return APPROVAL_STATUS_OPTIONS; }
  get activeOptions()  { return IS_ACTIVE_OPTIONS; }

  // Toggle selection check
  toggleSelectAll(event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      const codes = this.components().map(c => c.componentCode);
      this.selectedCodes.set(codes);
    } else {
      this.selectedCodes.set([]);
    }
  }

  toggleItemSelection(code: string, event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      this.selectedCodes.update(codes => [...codes, code]);
    } else {
      this.selectedCodes.update(codes => codes.filter(x => x !== code));
    }
  }

  isAllSelected(): boolean {
    const components = this.components();
    if (components.length === 0) return false;
    return this.selectedCodes().length === components.length;
  }
}
