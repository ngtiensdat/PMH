import { z } from 'zod';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { APP_LABELS_VN } from '../../core/constants/labels';

export const LoginSchema = z.object({
  username: z
    .string()
    .min(1, APP_LABELS_VN.messages.validation.usernameRequired)
    .max(50, APP_LABELS_VN.messages.validation.usernameMaxLength)
    .trim(),

  password: z
    .string()
    .min(1, APP_LABELS_VN.messages.validation.passwordRequired)
    .max(100, APP_LABELS_VN.messages.validation.passwordMaxLength)
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
    return { zodError: fieldError ? fieldError.message : APP_LABELS_VN.messages.validation.invalidData };
  };
}
