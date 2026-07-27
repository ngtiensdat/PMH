import { z } from 'zod';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

// ─── Helper: parse ISO string an toàn ────────────────────────────────────────
function parseDate(val: string | undefined | null): Date | null {
  if (!val || val.trim() === '') return null;
  const d = new Date(val);
  return isNaN(d.getTime()) ? null : d;
}

// ─── Reusable date refinements ────────────────────────────────────────────────

function isValidDatetime(val: string): boolean {
  if (!val) return false;
  const d = new Date(val);
  if (isNaN(d.getTime())) return false;
  return val.length >= 10;
}

function isNotTooFarPast(val: string): boolean {
  const d = parseDate(val);
  if (!d) return true;
  return d.getFullYear() >= (new Date().getFullYear() - 100);
}

function isNotTooFarFuture(val: string): boolean {
  const d = parseDate(val);
  if (!d) return true;
  return d.getFullYear() <= (new Date().getFullYear() + 100);
}

function isNotPastDate(val: string): boolean {
  const d = parseDate(val);
  if (!d) return true;
  return d.getTime() >= Date.now() - 60000;
}

// ─── Zod v4 Schema ────────────────────────────────────────────────────────────

export const ComponentSchema = z.object({
  componentCode: z
    .string()
    .min(1, 'Mã cấu phần không được để trống')
    .max(200, 'Mã cấu phần tối đa 200 ký tự')
    .regex(/^[A-Z0-9_]+$/, 'Mã cấu phần chỉ gồm chữ in hoa, số và dấu gạch dưới, không chứa tiếng Việt, khoảng trắng hay ký tự đặc biệt'),

  componentName: z
    .string()
    .min(1, 'Tên cấu phần không được để trống')
    .max(150, 'Tên cấu phần tối đa 150 ký tự')
    .refine(val => !/[\^#|*@$`~!%&{}\[\]?<>"'()\/\\:;=,]/.test(val), {
      message: 'Tên cấu phần không được chứa khoảng trắng đặc biệt hay ký tự đặc biệt (^, #, |, *, @, $, ...)'
    }),

  messageType: z
    .string()
    .max(150, 'Chuẩn tin điện tối đa 150 ký tự')
    .optional()
    .or(z.literal('')),

  connectionMethod: z
    .string()
    .max(100, 'Phương thức kết nối tối đa 100 ký tự')
    .optional()
    .or(z.literal('')),

  checkToken: z
    .string()
    .refine(val => val === 'Y' || val === 'N', {
      message: 'Kiểm tra Token chỉ nhận giá trị Y hoặc N'
    }),

  description: z
    .string()
    .max(4000, 'Mô tả tối đa 4000 ký tự')
    .optional()
    .or(z.literal('')),

  isActive: z.number(),

  effectiveDate: z
    .string()
    .min(1, 'Ngày hiệu lực không được để trống')
    .refine(isValidDatetime, {
      message: 'Ngày hiệu lực không phải định dạng ngày giờ hợp lệ (yyyy-MM-ddTHH:mm)'
    })
    .refine(isNotPastDate, {
      message: 'Ngày hiệu lực không được là ngày trong quá khứ'
    })
    .refine(isNotTooFarFuture, {
      message: 'Ngày hiệu lực không được vượt quá 100 năm trong tương lai'
    }),

  endEffectiveDate: z
    .string()
    .optional()
    .refine(val => !val || isValidDatetime(val), {
      message: 'Ngày hết hiệu lực không phải định dạng ngày giờ hợp lệ (yyyy-MM-ddTHH:mm)'
    })
    .refine(val => !val || isNotPastDate(val), {
      message: 'Ngày hết hiệu lực không được là ngày trong quá khứ'
    })
    .refine(val => !val || isNotTooFarFuture(val), {
      message: 'Ngày hết hiệu lực không được vượt quá 100 năm trong tương lai'
    })

}).refine(data => {
  const start = parseDate(data.effectiveDate);
  const end = parseDate(data.endEffectiveDate);
  if (!start || !end) return true;
  return end.getTime() > start.getTime();
}, {
  message: 'Ngày hết hiệu lực phải sau ngày hiệu lực',
  path: ['endEffectiveDate']
});

export type ComponentFormData = z.infer<typeof ComponentSchema>;

// ─── Helper tương thích Zod v3 + v4 ─────────────────────────────────────────
function getIssues(error: z.ZodError): any[] {
  return (error as any).issues ?? (error as any).errors ?? [];
}

// ─── Angular ValidatorFn tích hợp Zod ────────────────────────────────────────

export function zodFieldValidator(schema: z.ZodObject<any>, field: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const partialData: Record<string, unknown> = {};

    if (control.parent) {
      Object.keys(control.parent.controls).forEach(key => {
        partialData[key] = control.parent!.get(key)?.value;
      });
    }
    partialData[field] = control.value;

    if (partialData['checkToken'] !== undefined && typeof partialData['checkToken'] === 'boolean') {
      partialData['checkToken'] = partialData['checkToken'] ? 'Y' : 'N';
    }
    if (Array.isArray(partialData['messageType'])) {
      partialData['messageType'] = partialData['messageType'].join(', ');
    }
    if (Array.isArray(partialData['connectionMethod'])) {
      partialData['connectionMethod'] = partialData['connectionMethod'].join(', ');
    }

    const result = schema.safeParse(partialData);
    if (result.success) return null;

    const fieldError = getIssues(result.error).find((e: any) => e.path[0] === field);
    if (!fieldError) return null;

    return { zodError: fieldError.message };
  };
}

export function zodFormValidator(schema: z.ZodType<any>): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const rawValue = { ...control.value };
    if (rawValue['checkToken'] !== undefined && typeof rawValue['checkToken'] === 'boolean') {
      rawValue['checkToken'] = rawValue['checkToken'] ? 'Y' : 'N';
    }
    if (Array.isArray(rawValue['messageType'])) {
      rawValue['messageType'] = rawValue['messageType'].join(', ');
    }
    if (Array.isArray(rawValue['connectionMethod'])) {
      rawValue['connectionMethod'] = rawValue['connectionMethod'].join(', ');
    }
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
