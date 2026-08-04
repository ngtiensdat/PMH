import { Component, OnInit, signal, inject, computed, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CategoryService } from '../../services/category.service';
import { LanguageService } from '../../../../core/services/language.service';
import { STATUS_MAP, APPROVAL_STATUS_OPTIONS, IS_ACTIVE_OPTIONS, ACTION_PILL_MAP } from '../../../../shared/constants/status.constants';
import { parseDateString } from '../../../../shared/utils/date.utils';
import { NotificationService } from '../../../../shared/components/notification/notification.service';
import { GroupCategoryResponse } from '../../../../shared/models/group-category.model';
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
  private fb = inject(FormBuilder);
  private categoryService = inject(CategoryService);
  public languageService = inject(LanguageService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  searchForm: FormGroup = inject(FormBuilder).group({
    paramType: [''],
    paramValue: [''],
    paramName: [''],
    status: [''],
    isActive: ['']
  });

  categories = signal<GroupCategoryResponse[]>([]);
  totalElements = signal<number>(0);
  page = signal<number>(0);
  size = signal<number>(10);
  totalPages = computed(() => Math.ceil(this.totalElements() / this.size()) || 1);
  sortField = signal<string>('updatedDate');
  sortDirection = signal<string>('desc');

  viewMode = signal<'jpa' | 'native'>('jpa');
  activeTabIndex = 0; // Index 0: JPA, 1: Native Query

  joinedCategories = signal<GroupCategoryResponse[]>([]);
  selectedIds = signal<number[]>([]);
  isLoading = signal<boolean>(false);

  // Dùng shared STATUS_MAP từ constants
  statusMap = STATUS_MAP;

  constructor() {
    console.log('[CategoryListComponent] Constructor - searchForm status:', this.searchForm ? 'defined' : 'undefined');
    console.log('[CategoryListComponent] Constructor - viewMode:', this.viewMode());
  }

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

  // Dynamic columns definition with generous default widths
  columns = [
    { id: 'checkbox', label: '', isFixed: true, width: 40 },
    { id: 'stt', label: 'STT', isFixed: true, width: 60 },
    { id: 'paramType', label: 'Danh mục theo nhóm', isFixed: true, width: 185 },
    { id: 'paramValue', label: 'Giá trị thành phần', isFixed: true, width: 160 },
    { id: 'paramName', label: 'Tên thành phần', isFixed: true, width: 200 },
    { id: 'description', label: 'Mô tả', isFixed: false, width: 220 },
    { id: 'componentCode', label: 'Cấu phần xử lý', isFixed: false, width: 150 },
    { id: 'effectiveDate', label: 'Ngày hiệu lực', isFixed: false, width: 180 },
    { id: 'endEffectiveDate', label: 'Ngày hết hiệu lực', isFixed: false, width: 180 },
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
  isResizing = false;

  // Column resizing implementation
  onResizeStart(event: MouseEvent, col: any) {
    event.stopPropagation();
    event.preventDefault();
    this.isResizing = true;
    const startX = event.clientX;
    const startWidth = col.width;
    
    const onMouseMove = (moveEvent: MouseEvent) => {
      const deltaX = moveEvent.clientX - startX;
      const minWidth = col.id === 'checkbox' ? 40 : 80;
      col.width = Math.max(minWidth, startWidth + deltaX);
    };
    
    const onMouseUp = () => {
      this.isResizing = false;
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
    };
    
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  }

  // Column reordering implementation
  onDragStart(colId: string, event: DragEvent) {
    const index = this.columns.findIndex(c => c.id === colId);
    if (this.isResizing || index === -1 || this.columns[index].isFixed) {
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

  // Column sorting implementation
  toggleSort(colId: string) {
    if (colId === 'checkbox' || colId === 'stt' || colId === 'actions') return;

    const currentField = this.sortField();
    const currentDir = this.sortDirection();

    let newField = colId;
    let newDir = 'asc';

    if (currentField === colId) {
      if (currentDir === 'asc') {
        newDir = 'desc';
      } else {
        // If it is already desc, reset to default (updatedDate, desc)
        // Unless we are already sorting by updatedDate, in which case we toggle back to asc
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
    // Auto save list state
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
    const filters = {
      paramType: rawFilters.paramType,
      paramValue: rawFilters.paramValue,
      paramName: rawFilters.paramName,
      status: rawFilters.status ? [Number(rawFilters.status)] : [],
      isActive: rawFilters.isActive ? [Number(rawFilters.isActive)] : []
    };
    const sortParam = `${this.sortField()},${this.sortDirection()}`;

    this.categoryService.search(filters, this.page(), this.size(), sortParam).subscribe({
      next: (res) => {
        console.log('[CategoryListComponent] search success, res:', res);
        const content = (res.data.content || []).map((item: GroupCategoryResponse) => {
          if (item.newData) {
            try {
              const parsed = typeof item.newData === 'string' ? JSON.parse(item.newData) : item.newData;
              if (parsed) {
                if (parsed.effectiveDate) parsed.effectiveDate = parseDateString(parsed.effectiveDate);
                if (parsed.endEffectiveDate) parsed.endEffectiveDate = parseDateString(parsed.endEffectiveDate);
                return { ...item, ...parsed };
              }
            } catch (e) {
              console.error('Error parsing category newData:', e);
            }
          }
          return item;
        });
        this.categories.set(content);
        this.totalElements.set(res.data.page?.totalElements ?? res.data.totalElements ?? 0);
        this.selectedIds.set([]);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.notificationService.error('Lỗi tải dữ liệu: ' + (err.error?.message || err.message));
        this.isLoading.set(false);
      }
    });
  }

  loadJoinedData() {
    this.categoryService.getComplexList().subscribe({
      next: (res) => {
        console.log('[CategoryListComponent] loadJoinedData success, res:', res);
        const content = (res.data || []).map((item: GroupCategoryResponse) => {
          if (item.newData) {
            try {
              const parsed = typeof item.newData === 'string' ? JSON.parse(item.newData) : item.newData;
              if (parsed) {
                if (parsed.effectiveDate) parsed.effectiveDate = parseDateString(parsed.effectiveDate);
                if (parsed.endEffectiveDate) parsed.endEffectiveDate = parseDateString(parsed.endEffectiveDate);
                return { ...item, ...parsed };
              }
            } catch (e) {
              console.error('Error parsing joined category newData:', e);
            }
          }
          return item;
        });
        this.joinedCategories.set(content);
        this.selectedIds.set([]);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.notificationService.error('Lỗi tải dữ liệu liên kết: ' + (err.error?.message || err.message));
        this.isLoading.set(false);
      }
    });
  }

  onTabChange(index: number) {
    console.log('[CategoryListComponent] onTabChange - index:', index);
    if (index === undefined || index === null || index < 0) return;
    this.activeTabIndex = index;
    const targetMode = index === 0 ? 'jpa' : 'native';
    console.log('[CategoryListComponent] onTabChange - targetMode:', targetMode, 'current viewMode:', this.viewMode());
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
    this.router.navigate(['/categories/add']);
  }

  openEditDialog(item: GroupCategoryResponse) {
    this.router.navigate(['/categories/edit', item.id]);
  }

  openCopyDialog(item: GroupCategoryResponse) {
    this.router.navigate(['/categories/copy', item.id], { state: { data: item } });
  }

  // Confirmation dialog state
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

  // Reject Dialog state
  isRejectOpen = false;
  rejectReason = '';
  rejectTargetIds: number[] = [];

  // History Dialog state
  isHistoryOpen = false;
  historyData = signal<MappedHistoryItem[]>([]);
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
    const ids = this.selectedIds();
    if (ids.length === 0) {
      this.notificationService.warning('Vui lòng chọn ít nhất một bản ghi để duyệt!');
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
      this.notificationService.warning('Vui lòng chọn ít nhất một bản ghi để từ chối!');
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
        const successCount = (res.data || []).filter((r: Record<string, unknown>) => r['success']).length;
        this.notificationService.success(`Đã từ chối thành công ${successCount}/${ids.length} bản ghi! Lý do: ${reason}`);
        this.loadData();
      },
      error: (err: HttpErrorResponse) => {
        this.notificationService.error('Lỗi thực hiện từ chối duyệt: ' + (err.error?.message || err.message));
        this.isLoading.set(false);
      }
    });
  }

  openHistoryDialog(item: GroupCategoryResponse) {
    this.historyTargetName = item.paramName || item.paramValue;
    this.categoryService.getHistory(item.id).subscribe({
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
      error: (err: HttpErrorResponse) => {
        this.notificationService.error('Không thể tải lịch sử thao tác: ' + (err.error?.message || err.message));
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

    if (action === 'delete' && id !== null) {
      this.categoryService.delete(id).subscribe({
        next: () => {
          this.notificationService.success('Xóa thành công!');
          this.loadData();
        },
        error: (err: HttpErrorResponse) => {
          this.notificationService.error('Không thể xóa: ' + (err.error?.message || err.message));
          this.isLoading.set(false);
        }
      });
    } else if (action === 'sendApproval' && id !== null) {
      this.categoryService.sendApproval(id).subscribe({
        next: () => {
          this.notificationService.success('Gửi duyệt thành công!');
          this.loadData();
        },
        error: (err: HttpErrorResponse) => {
          this.notificationService.error('Lỗi gửi duyệt: ' + (err.error?.message || err.message));
          this.isLoading.set(false);
        }
      });
    } else if (action === 'cancelApproval' && id !== null) {
      this.categoryService.cancelApproval(id).subscribe({
        next: () => {
          this.notificationService.success('Hủy duyệt thành công!');
          this.loadData();
        },
        error: (err: HttpErrorResponse) => {
          this.notificationService.error('Lỗi hủy duyệt: ' + (err.error?.message || err.message));
          this.isLoading.set(false);
        }
      });
    } else if (action === 'batchApprove') {
      const ids = this.selectedIds();
      this.categoryService.batchApprove(ids).subscribe({
        next: (res) => {
          const successCount = (res.data || []).filter((r: Record<string, unknown>) => r['success']).length;
          this.notificationService.success(`Đã duyệt thành công ${successCount}/${ids.length} bản ghi!`);
          this.loadData();
        },
        error: (err: HttpErrorResponse) => {
          this.notificationService.error('Lỗi thực hiện duyệt hàng loạt: ' + (err.error?.message || err.message));
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
          this.notificationService.warning('Không có dữ liệu để xuất!');
          return;
        }
        
        let csvContent = 'data:text/csv;charset=utf-8,\uFEFF';
        csvContent += 'ID,Danh mục theo nhóm,Giá trị thành phần,Tên thành phần,Mô tả,Trạng thái duyệt,Hoạt động,Ngày hiệu lực\n';
        
        data.forEach((row: GroupCategoryResponse) => {
          const statusLabel = row.status === 4 ? 'Đã duyệt' : 'Chưa duyệt';
          const activeLabel = row.isActive === 1 ? 'Hoạt động' : 'Không hoạt động';
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
        this.notificationService.error('Lỗi xuất dữ liệu: ' + (err.error?.message || err.message));
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
