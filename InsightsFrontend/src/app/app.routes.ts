import { Routes } from '@angular/router';
import { ReleaseGraphComponent } from './pages/release-graph/release-graph.component';

export const routes: Routes = [
  { path: '', redirectTo: 'graph', pathMatch: 'full' },
  { path: '**', redirectTo: 'graph' },
  { path: 'graph', component: ReleaseGraphComponent }
];
