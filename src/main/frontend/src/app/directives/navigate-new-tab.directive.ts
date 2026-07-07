import { Directive, HostListener, Input, inject } from '@angular/core';
import { Location } from '@angular/common';
import { Params, Router, RouterLink } from '@angular/router';

@Directive({
  // eslint-disable-next-line @angular-eslint/directive-selector
  selector: '[routerLink], [ffNewTab], [ffNewTabToggle]',
  standalone: true,
})
export class NavigateNewTabDirective {
  @Input() ffNewTab: string | null = null;
  @Input() ffNewTabQuery: Params | null = null;
  @Input() ffNewTabToggle: string[] | null = null;

  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly routerLink = inject(RouterLink, { optional: true, self: true });

  @HostListener('auxclick', ['$event'])
  onAuxClick(event: MouseEvent): void {
    if (event.button !== 1) return;

    const url = this.resolveUrl();
    if (!url) return;

    event.preventDefault();
    event.stopPropagation();
    window.open(this.location.prepareExternalUrl(url), '_blank', 'noopener');
  }

  @HostListener('mousedown', ['$event'])
  onMouseDown(event: MouseEvent): void {
    if (event.button === 1 && this.resolveUrl()) {
      event.preventDefault();
    }
  }

  private resolveUrl(): string | null {
    return this.resolveFromRouterLink() ?? this.resolveFromToggle() ?? this.resolveFromTarget();
  }

  private resolveFromRouterLink(): string | null {
    const tree = this.routerLink?.urlTree;
    return tree ? this.router.serializeUrl(tree) : null;
  }

  private resolveFromToggle(): string | null {
    if (!this.ffNewTabToggle?.length) return null;

    const tree = this.router.parseUrl(this.router.url);
    tree.queryParams = this.toggleParams(tree.queryParams, this.ffNewTabToggle);
    return this.router.serializeUrl(tree);
  }

  private resolveFromTarget(): string | null {
    if (!this.ffNewTab) return null;

    const tree = this.router.parseUrl(this.ffNewTab);
    if (this.ffNewTabQuery) {
      tree.queryParams = { ...tree.queryParams, ...this.ffNewTabQuery };
    }
    return this.router.serializeUrl(tree);
  }

  private toggleParams(current: Params, keys: string[]): Params {
    const next: Params = { ...current };
    for (const key of keys) {
      if (key in next) delete next[key];
      else next[key] = '';
    }

    return next;
  }
}
