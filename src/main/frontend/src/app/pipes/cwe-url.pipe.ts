import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'cweUrl', standalone: true, pure: true })
export class CweUrlPipe implements PipeTransform {
  transform(cwe: string): string {
    const match = cwe.match(/(\d+)/);
    return match ? `https://cwe.mitre.org/data/definitions/${match[1]}.html` : '#';
  }
}
