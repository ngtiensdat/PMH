import { NgModule } from '@angular/core';
import {
  TuiButton,
  TuiDialog,
  TuiInput,
  TuiTextfield,
  TuiLabel,
  TuiDropdown,
  TuiCheckbox
} from '@taiga-ui/core';
import {
  TuiTabs,
  TuiPagination,
  TuiChevron,
  TuiSelect,
  TuiDataListWrapper
} from '@taiga-ui/kit';

const TAIGA_COMPONENTS: any[] = [
  TuiButton,
  TuiDialog,
  TuiInput,
  TuiTextfield,
  TuiLabel,
  TuiDropdown,
  TuiCheckbox,
  TuiTabs,
  TuiPagination,
  TuiChevron,
  TuiSelect,
  TuiDataListWrapper
];

@NgModule({
  imports: TAIGA_COMPONENTS,
  exports: TAIGA_COMPONENTS
})
export class SharedTaigaModule {}
