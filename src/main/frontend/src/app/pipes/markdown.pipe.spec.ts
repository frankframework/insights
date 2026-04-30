import { TestBed } from '@angular/core/testing';
import { DomSanitizer } from '@angular/platform-browser';
import { MarkdownPipe } from './markdown.pipe';

describe('MarkdownPipe', () => {
  let pipe: MarkdownPipe;
  let bypassSpy: jasmine.Spy;

  beforeEach(() => {
    bypassSpy = jasmine.createSpy('bypassSecurityTrustHtml').and.callFake((html: string) => html);

    TestBed.configureTestingModule({
      providers: [{ provide: DomSanitizer, useValue: { bypassSecurityTrustHtml: bypassSpy } }],
    });

    pipe = TestBed.runInInjectionContext(() => new MarkdownPipe());
  });

  const render = (value: string | null | undefined): string => pipe.transform(value) as string;

  describe('null / empty input', () => {
    it('returns empty string for null', () => {
      expect(pipe.transform(null)).toBe('');
    });

    it('returns empty string for undefined', () => {
      expect(pipe.transform(undefined)).toBe('');  // eslint-disable-line unicorn/no-useless-undefined
    });

    it('returns empty string for empty string', () => {
      expect(pipe.transform('')).toBe('');
    });

    it('does not call the sanitizer for empty input', () => {
      pipe.transform(null);
      pipe.transform(undefined); // eslint-disable-line unicorn/no-useless-undefined
      pipe.transform('');

      expect(bypassSpy).not.toHaveBeenCalled();
    });
  });

  describe('markdown rendering', () => {
    it('converts bold markdown to <strong>', () => {
      expect(render('**bold**')).toContain('<strong>bold</strong>');
    });

    it('converts italic markdown to <em>', () => {
      expect(render('_italic_')).toContain('<em>italic</em>');
    });

    it('converts a heading to <h1>', () => {
      expect(render('# Heading')).toContain('<h1>Heading</h1>');
    });

    it('converts inline code', () => {
      expect(render('`code`')).toContain('<code>code</code>');
    });

    it('converts an unordered list item', () => {
      expect(render('- item')).toContain('<li>item</li>');
    });

    it('escapes HTML entities inside code spans', () => {
      expect(render('`<div>`')).toContain('&lt;div&gt;');
    });
  });

  describe('link handling', () => {
    it('adds target="_blank" to links', () => {
      expect(render('[text](https://example.com)')).toContain('target="_blank"');
    });

    it('adds rel="noopener noreferrer" to links', () => {
      expect(render('[text](https://example.com)')).toContain('rel="noopener noreferrer"');
    });

    it('applies target and rel to every link in the output', () => {
      const result = render('[a](https://a.com) and [b](https://b.com)');

      expect((result.match(/target="_blank"/g) ?? []).length).toBe(2);
      expect((result.match(/rel="noopener noreferrer"/g) ?? []).length).toBe(2);
    });
  });

  describe('sanitization', () => {
    it('passes the rendered HTML through bypassSecurityTrustHtml', () => {
      pipe.transform('hello');

      expect(bypassSpy).toHaveBeenCalledOnceWith(jasmine.stringContaining('<p>hello</p>'));
    });
  });
});
