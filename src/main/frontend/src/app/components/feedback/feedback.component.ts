import { Component, inject, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { VersionService, BuildInfo } from '../../services/version.service';
import { NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-feedback',
  imports: [NgOptimizedImage],
  templateUrl: './feedback.component.html',
  styleUrl: './feedback.component.scss',
})
export class FeedbackComponent {
  private router = inject(Router);
  private versionService = inject(VersionService);

  private buildInfo: BuildInfo | null = null;
  private selectedText = '';

  constructor() {
    this.versionService.getBuildInformation().subscribe((info) => {
      this.buildInfo = info;
    });
  }

  @HostListener('document:mouseup')
  onTextSelection(): void {
    const selection = globalThis.getSelection();
    this.selectedText = selection?.toString().trim() || '';
  }

  openFeedback(): void {
    const currentPage = this.router.url;
    const version = this.buildInfo?.version || 'Unknown';

    const parameters = new URLSearchParams({
      title: '',
      body: `## Context
- **Page:** \`${currentPage}\`
- **Version:** \`${version}\`
${this.selectedText ? `- **Selected Text:** \`${this.selectedText}\`\n` : ''}
## Description
<!-- Describe the bug or issue -->

`,
    });

    window.open(`https://github.com/frankframework/insights/issues/new?${parameters}`, '_blank');
  }
}
