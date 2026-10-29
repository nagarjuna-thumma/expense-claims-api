import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'statusLabel' })
export class StatusLabelPipe implements PipeTransform {
  transform(status: string): string {
    return status.charAt(0) + status.slice(1).toLowerCase();
  }
}
