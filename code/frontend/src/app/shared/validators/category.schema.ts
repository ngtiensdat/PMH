import { z } from 'zod';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

// ─── Helper: parse ISO string an toàn ────────────────────────────────────────
function parseDate(val: string | undefined | null): Date | null {
  if (!val || val.trim() === '') return null;
  const d = new Date(val);
  return isNaN(d.getTime()) ? null : d;
}

// ─── Reusable date refinements ────────────────────────────────────────────────

/** Kiểm tra chuỗi là ISO datetime hợp lệ (có cả ngày + giờ) */
function isValidDatetime(val: string): boolean {
  if (!val) return false;
  const d = new Date(val);
  if (isNaN(d.getTime())) return false;
  // Phải có ít nhất thông tin ngày (yyyy-MM-dd hoặc yyyy-MM-ddTHH:mm)
  return val.length >= 10;
}

/** Kiểm tra không phải ngày quá xa trong quá khứ (>100 năm trước) */
function isNotTooFarPast(val: string): boolean {
  const d = parseDate(val);
  if (!d) return true;
  const minYear = new Date().getFullYear() - 100;
  return d.getFullYear() >= minYear;
}

/** Kiểm tra không phải ngày quá xa trong tương lai (>100 năm sau) */
function isNotTooFarFuture(val: string): boolean {
  const d = parseDate(val);
  if (!d) return true;
  const maxYear = new Date().getFullYear() + 100;
  return d.getFullYear() <= maxYear;
}

function isNotPastDate(val: string): boolean {
  const d = parseDate(val);
  if (!d) return true;
  return d.getTime() >= Date.now() - 60000;
}

// ─── Zod v4 Schema ────────────────────────────────────────────────────────────

export const CategorySchema = z.object({
  paramType: z
    .string()
    .min(1, 'Danh mục theo nhóm không được để trống')
    .max(255, 'Danh mục theo nhóm tối đa 255 ký tự'),

  paramValue: z
    .string()
    .min(1, 'Giá trị thành phần không được để trống')
    .max(255, 'Giá trị thành phần tối đa 255 ký tự'),

  paramName: z
    .string()
    .min(1, 'Tên thành phần không được để trống')
    .max(255, 'Tên thành phần tối đa 255 ký tự')
    .refine(val => !/[\^#|*@$`~!%&{}\[\]?<>"'()\/\\:;=,]/.test(val), {
      message: 'Tên thành phần không được chứa khoảng trắng đặc biệt hay ký tự đặc biệt (^, #, |, *, @, $, ...)'
    }),

  description: z
    .string()
    .max(4000, 'Mô tả tối đa 4000 ký tự')
    .optional()
    .or(z.literal('')),

  componentCode: z
    .string()
    .min(1, 'Cấu phần xử lý không được để trống'),

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
  // Cross-field: endEffectiveDate phải SAU effectiveDate (không bằng, phải sau)
  const start = parseDate(data.effectiveDate);
  const end   = parseDate(data.endEffectiveDate);
  if (!start || !end) return true;
  return end.getTime() > start.getTime();
}, {
  message: 'Ngày hết hiệu lực phải sau ngày hiệu lực',
  path: ['endEffectiveDate']
});

export type CategoryFormData = z.infer<typeof CategorySchema>;

// ─── Helper tương thích Zod v3 + v4 ─────────────────────────────────────────
function getIssues(error: z.ZodError): any[] {
  return (error as any).issues ?? (error as any).errors ?? [];
}

// ─── Angular ValidatorFn tích hợp Zod ────────────────────────────────────────

/**
 * Validator cho từng field — lấy message từ Zod schema.
 * Dùng: `zodFieldValidator(CategorySchema, 'effectiveDate')`
 */
export function zodFieldValidator(schema: z.ZodObject<any>, field: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const partialData: Record<string, unknown> = {};

    // Thu thập giá trị toàn form để validate cross-field
    if (control.parent) {
      Object.keys(control.parent.controls).forEach(key => {
        partialData[key] = control.parent!.get(key)?.value;
      });
    }
    partialData[field] = control.value;

    if (Array.isArray(partialData['componentCode'])) {
      partialData['componentCode'] = partialData['componentCode'].join(', ');
    }

    const result = schema.safeParse(partialData);
    if (result.success) return null;

    const fieldError = getIssues(result.error).find((e: any) => e.path[0] === field);
    if (!fieldError) return null;

    return { zodError: fieldError.message };
  };
}

/**
 * Validator toàn bộ FormGroup — bắt lỗi cross-field (date range, etc).
 * Trả về object { 'endEffectiveDate': 'message' } để map vào form errors.
 */
export function zodFormValidator(schema: z.ZodType<any>): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const rawValue = { ...control.value };
    if (Array.isArray(rawValue['componentCode'])) {
      rawValue['componentCode'] = rawValue['componentCode'].join(', ');
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
