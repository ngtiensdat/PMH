import { z } from 'zod';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const LoginSchema = z.object({
  username: z
    .string()
    .min(1, 'Tên đăng nhập không được để trống')
    .max(50, 'Tên đăng nhập tối đa 50 ký tự')
    .trim(),

  password: z
    .string()
    .min(1, 'Mật khẩu không được để trống')
    .max(100, 'Mật khẩu tối đa 100 ký tự')
});

export function zodFieldValidator(schema: z.ZodObject<any>, field: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value && control.value !== 0) {
      return null;
    }
    const fieldSchema = schema.shape[field];
    if (!fieldSchema) return null;

    const result = fieldSchema.safeParse(control.value);
    if (result.success) return null;

    const fieldError = result.error.issues[0];
    return { zodError: fieldError ? fieldError.message : 'Dữ liệu không hợp lệ' };
  };
}
