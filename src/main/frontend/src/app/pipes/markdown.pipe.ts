import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';

@Pipe({ name: 'markdown', standalone: true })
export class MarkdownPipe implements PipeTransform {
  private sanitizer = inject(DomSanitizer);

  transform(value: string | null | undefined): SafeHtml {
    if (!value) return '';
    const html = (marked(value) as string).replaceAll('<a ', '<a target="_blank" rel="noopener noreferrer" ');
    return this.sanitizer.bypassSecurityTrustHtml(html);
  }
}
