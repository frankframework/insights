import { Routes } from '@angular/router';
import { ReleaseGraphComponent } from './pages/release-graph/release-graph.component';
import { NotFoundComponent } from './components/not-found/not-found.component';
import { ReleaseRoadmapComponent } from './pages/release-roadmap/release-roadmap.component';

export const routes: Routes = [
  { path: '', redirectTo: 'graph', pathMatch: 'full' },
  { path: 'graph', component: ReleaseGraphComponent },
  { path: 'roadmap', component: ReleaseRoadmapComponent },
  { path: 'not-found', component: NotFoundComponent },
  { path: '**', redirectTo: 'not-found' },
];
