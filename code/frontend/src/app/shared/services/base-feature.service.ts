import { ListState } from '../models/api-response.model';

/**
 * Abstract base cho các Feature Service có quản lý list state (pagination + filters).
 * Subclass chỉ cần extend — không cần implement gì thêm.
 */
export abstract class BaseFeatureService {
  private listState: ListState | null = null;

  setListState(state: ListState): void   { this.listState = state; }
  getListState(): ListState | null       { return this.listState; }
  clearListState(): void                 { this.listState = null; }
}
