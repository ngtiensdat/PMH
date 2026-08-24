import { z } from 'zod';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { APP_LABELS_VN } from '../../core/constants/labels';

// ─── Helper: parse ISO string an toàn ────────────────────────────────────────
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

// ─── Reusable date refinements ────────────────────────────────────────────────

function isValidDatetime(val: any): boolean {
  if (!val) return true;
  return parseDate(val) !== null;
}

function isNotTooFarPast(val: any): boolean {
  if (!val) return true;
  const d = parseDate(val);
  if (!d) return true;
  return d.getFullYear() >= (new Date().getFullYear() - 100);
}

function isNotTooFarFuture(val: any): boolean {
  if (!val) return true;
  const d = parseDate(val);
  if (!d) return true;
  return d.getFullYear() <= (new Date().getFullYear() + 100);
}

const valMsgs = APP_LABELS_VN.messages.validation;

// ─── Zod v4 Schema ────────────────────────────────────────────────────────────

export const CategorySchema = z.object({
  paramType: z
    .string()
    .min(1, valMsgs.paramTypeRequired)
    .max(255, valMsgs.paramTypeMaxLength),

  paramValue: z
    .string()
    .min(1, valMsgs.paramValueRequired)
    .max(255, valMsgs.paramValueMaxLength),

  paramName: z
    .string()
    .min(1, valMsgs.paramNameRequired)
    .max(255, valMsgs.paramNameMaxLength)
    .refine(val => !/[\^#|*@$`~!%&{}\[\]?<>"'()\/\\:;=,]/.test(val), {
      message: valMsgs.paramNameInvalidChars
    }),

  description: z
    .any()
    .optional()
    .nullable()
    .refine(val => !val || (typeof val === 'string' && val.length <= 4000), {
      message: valMsgs.descriptionMaxLength
    }),

  componentCode: z
    .any()
    .refine(val => {
      if (Array.isArray(val)) return val.length > 0;
      if (typeof val === 'string') return val.trim().length > 0;
      return false;
    }, { message: valMsgs.componentCodeRequired }),

  isActive: z.number().optional().nullable(),

  effectiveDate: z
    .string()
    .min(1, valMsgs.effectiveDateRequired)
    .refine(isValidDatetime, {
      message: valMsgs.effectiveDateInvalidFormat
    })
    .refine(isNotTooFarPast, {
      message: valMsgs.effectiveDateTooFarPast
    })
    .refine(isNotTooFarFuture, {
      message: valMsgs.effectiveDateTooFarFuture
    }),

  endEffectiveDate: z
    .any()
    .optional()
    .nullable()
    .refine(val => !val || isValidDatetime(val), {
      message: valMsgs.endEffectiveDateInvalidFormat
    })
    .refine(val => !val || isNotTooFarPast(val), {
      message: valMsgs.endEffectiveDateTooFarPast
    })
    .refine(val => !val || isNotTooFarFuture(val), {
      message: valMsgs.endEffectiveDateTooFarFuture
    })

}).refine(data => {
  const start = parseDate(data.effectiveDate);
  const end   = parseDate(data.endEffectiveDate);
  if (!start || !end) return true;
  return end.getTime() > start.getTime();
}, {
  message: valMsgs.endEffectiveDateMustBeAfter,
  path: ['endEffectiveDate']
});

export type CategoryFormData = z.infer<typeof CategorySchema>;

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
