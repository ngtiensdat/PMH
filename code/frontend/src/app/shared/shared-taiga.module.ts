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
  TuiDataList
} from '@taiga-ui/core';
import {
  TuiTabs,
  TuiPagination,
  TuiChevron,
  TuiSelect,
  TuiDataListWrapper,
  TuiMultiSelect
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
  TuiTabs,
  TuiPagination,
  TuiChevron,
  TuiSelect,
  TuiDataListWrapper,
  TuiMultiSelect
];

@NgModule({
  imports: TAIGA_COMPONENTS,
  exports: TAIGA_COMPONENTS
})
export class SharedTaigaModule { }
