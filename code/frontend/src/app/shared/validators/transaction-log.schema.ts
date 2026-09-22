import { z } from 'zod';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { APP_LABELS_VN } from '../../core/constants/labels';

function parseDate(val: any): Date | null {
  if (!val) return null;
  if (val instanceof Date) return isNaN(val.getTime()) ? null : val;
  if (typeof val === 'string') {
    if (val.trim() === '') return null;
    const d = new Date(val);
    return isNaN(d.getTime()) ? null : d;
  }
  return null;
}

const valMsgs = APP_LABELS_VN.messages.validation;

export const TransactionLogSearchSchema = z.object({
  accountNo: z
    .string()
    .optional()
    .nullable()
    .refine(val => !val || val.length <= 50, {
      message: valMsgs.accountNoMaxLength
    }),

  transactionCode: z
    .string()
    .optional()
    .nullable()
    .refine(val => !val || val.length <= 100, {
      message: valMsgs.transactionCodeMaxLength
    }),

  status: z
    .enum(['', 'SUCCESS', 'FAILED'])
    .optional()
    .nullable(),

  fromDate: z
    .string()
    .optional()
    .nullable(),

  toDate: z
    .string()
    .optional()
    .nullable()

}).refine(data => {
  const start = parseDate(data.fromDate);
  const end   = parseDate(data.toDate);
  if (!start || !end) return true;
  return end.getTime() >= start.getTime();
}, {
  message: valMsgs.toDateMustBeAfterFromDate,
  path: ['toDate']
});

export type TransactionLogSearchFormData = z.infer<typeof TransactionLogSearchSchema>;

function getIssues(error: z.ZodError): any[] {
  return (error as any).issues ?? (error as any).errors ?? [];
}

export function zodSearchFormValidator(schema: z.ZodType<any>): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const rawValue = { ...control.value };
    const result = schema.safeParse(rawValue);
    if (result.success) return null;

    const errors: ValidationErrors = {};
    getIssues(result.error).forEach((err: any) => {
      const path = (err.path as (string | number)[]).join('.');
      errors[path || 'form'] = err.message;
    });
    return errors;
  };
}
