import { Component, Signal, WritableSignal, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { VersionService, BuildInfo } from '../../services/version.service';
import { NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-feedback',
  imports: [NgOptimizedImage],
  templateUrl: './feedback.component.html',
  host: { '(document:mouseup)': 'onTextSelection()' },
})
export class FeedbackComponent {
  private readonly router = inject(Router);
  private readonly versionService = inject(VersionService);

  private readonly buildInfo: Signal<BuildInfo | null>;
  private readonly selectedText: WritableSignal<string> = signal<string>('');

  constructor() {
    this.buildInfo = toSignal(this.versionService.getBuildInformation(), { initialValue: null });
  }

  onTextSelection(): void {
    const selection = globalThis.getSelection();
    this.selectedText.set(selection?.toString().trim() || '');
  }

  openFeedback(): void {
    const currentPage = this.router.url;
    const version = this.buildInfo()?.version || 'Unknown';
    const selectedText = this.selectedText();

    const parameters = new URLSearchParams({
      title: '',
      body: `## Context
- **Page:** \`${currentPage}\`
- **Version:** \`${version}\`
${selectedText ? `- **Selected Text:** \`${selectedText}\`\n` : ''}
## Description
<!-- Describe the bug or issue -->

`,
    });

    window.open(`https://github.com/frankframework/insights/issues/new?${parameters}`, '_blank');
  }
}
