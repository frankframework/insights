import { Component, inject } from '@angular/core';
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
import { TooltipComponent } from './components/tooltip/tooltip.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, LoaderComponent, HeaderComponent, TooltipComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  title = 'FF! Insights';

  public loading = false;

  private router = inject(Router);

  constructor() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) this.loading = true;
      if (event instanceof NavigationEnd || event instanceof NavigationCancel || event instanceof NavigationError)
        this.loading = false;
    });
  }
}
