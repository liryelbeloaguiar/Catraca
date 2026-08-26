import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../core/models';
import { UI_TEXT } from './texts';

export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as Partial<ApiError> | null;
    if (body?.message) return body.message;
  }
  return UI_TEXT.genericError;
}
