import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../core/models';

export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as Partial<ApiError> | null;
    if (body?.message) return body.message;
  }
  return 'Não foi possível concluir a operação. Tente novamente.';
}
