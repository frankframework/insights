import {
  AfterViewInit,
  Directive,
  ElementRef,
  EventEmitter,
  inject,
  Injector,
  Input,
  OnDestroy,
  Output,
  afterNextRender,
} from '@angular/core';

@Directive({
  selector: '[appInfiniteScroll]',
  standalone: true,
})
export class InfiniteScrollDirective implements AfterViewInit, OnDestroy {
  @Input({ required: true }) scrollContainer!: HTMLElement;
  @Input() isLoading = false;
  @Input() isLastPage = false;
  @Output() readonly loadMore = new EventEmitter<void>();

  private readonly el = inject(ElementRef<HTMLElement>);
  private readonly injector = inject(Injector);
  private observer!: IntersectionObserver;

  ngAfterViewInit(): void {
    this.observer = new IntersectionObserver(() => this.maybeLoadMore(), {
      root: this.scrollContainer,
      rootMargin: '0px 0px 200px 0px',
    });
    this.observer.observe(this.el.nativeElement);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  scrollToTop(behavior: ScrollBehavior = 'instant'): void {
    this.scrollContainer?.scrollTo({ top: 0, behavior });
  }

  checkAfterRender(): void {
    afterNextRender(() => this.maybeLoadMore(), { injector: this.injector });
  }

  private maybeLoadMore(): void {
    if (this.isLoading || this.isLastPage) return;

    const container = this.scrollContainer;
    const sentinel = this.el.nativeElement;
    if (!container || !sentinel) return;

    const sentinelWithinReach = sentinel.getBoundingClientRect().top <= container.getBoundingClientRect().bottom + 200;
    if (sentinelWithinReach) this.loadMore.emit();
  }
}
