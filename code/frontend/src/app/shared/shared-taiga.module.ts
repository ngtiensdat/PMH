import { NgModule } from '@angular/core';
import {
  TuiButton,
  TuiIcon,
  TuiDialog,
  TuiInput,
  TuiTextfield,
  TuiLabel,
  TuiDropdown,
  TuiCheckbox,
  TuiLoader,
  TuiDataList,
  TuiCalendar
} from '@taiga-ui/core';
import {
  TuiTabs,
  TuiPagination,
  TuiChevron,
  TuiSelect,
  TuiDataListWrapper,
  TuiMultiSelect,
  TuiInputDateTime
} from '@taiga-ui/kit';

const TAIGA_COMPONENTS: any[] = [
  TuiButton,
  TuiIcon,
  TuiDialog,
  TuiInput,
  TuiTextfield,
  TuiLabel,
  TuiDropdown,
  TuiCheckbox,
  TuiLoader,
  TuiDataList,
  TuiCalendar,
  TuiTabs,
  TuiPagination,
  TuiChevron,
  TuiSelect,
  TuiDataListWrapper,
  TuiMultiSelect,
  TuiInputDateTime
];

@NgModule({
  imports: TAIGA_COMPONENTS,
  exports: TAIGA_COMPONENTS
})
export class SharedTaigaModule { }
