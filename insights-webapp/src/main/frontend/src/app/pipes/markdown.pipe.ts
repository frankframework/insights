import { Pipe, PipeTransform } from '@angular/core';
import DOMPurify, { Config as DOMPurifyConfig } from 'dompurify';
import { marked } from 'marked';

const DOMPURIFY_CONFIG: DOMPurifyConfig = {
  USE_PROFILES: { html: true },
  ADD_ATTR: ['target'],
  FORBID_TAGS: ['script', 'style', 'iframe', 'frame', 'object', 'embed', 'noscript', 'xmp', 'noembed', 'noframes'],
  FORCE_BODY: true,
};

@Pipe({ name: 'markdown', standalone: true })
export class MarkdownPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) return '';
    const raw = (marked(value) as string).replaceAll('<a ', '<a target="_blank" rel="noopener noreferrer" ');
    return DOMPurify.sanitize(raw, DOMPURIFY_CONFIG);
  }
}
