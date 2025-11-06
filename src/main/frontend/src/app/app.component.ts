import { Component, inject, OnInit } from '@angular/core';
import {
  RouterOutlet,
  Router,
  NavigationEnd,
  NavigationCancel,
  NavigationError,
  NavigationStart,
} from '@angular/router';
import { LoaderComponent } from './components/loader/loader.component';
import { HeaderComponent } from './pages/header/header.component';
import { TooltipComponent } from './pages/release-roadmap/issue-bar/tooltip/tooltip.component';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, LoaderComponent, HeaderComponent, TooltipComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  title = 'FF! Insights';

  public loading = false;

  private router = inject(Router);
  private authService = inject(AuthService);

  constructor() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) this.loading = true;
      if (event instanceof NavigationEnd || event instanceof NavigationCancel || event instanceof NavigationError)
        this.loading = false;
    });
  }

  ngOnInit(): void {
    // Always check auth status on init to detect if user has an active session
    // This is safe because 401 (not logged in) won't show an error, only 403 (not authorized) will
    this.authService.checkAuthStatus().subscribe();
  }
}
