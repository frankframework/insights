import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'cweUrl', standalone: true, pure: true })
export class CweUrlPipe implements PipeTransform {
  transform(cwe: string): string {
    const m = cwe.match(/(\d+)/);
    return m ? `https://cwe.mitre.org/data/definitions/${m[1]}.html` : '#';
  }
}
