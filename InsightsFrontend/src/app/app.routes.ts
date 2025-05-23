import { Routes } from '@angular/router';
import { ReleaseGraphComponent } from './pages/release-graph/release-graph.component';
import { NotFoundComponent } from './components/not-found/not-found.component';
import { RoadmapComponent } from './pages/roadmap/roadmap.component';

export const routes: Routes = [
  { path: '', redirectTo: 'graph', pathMatch: 'full' },
  { path: 'graph', component: ReleaseGraphComponent },
  { path: 'roadmap', component: RoadmapComponent },
  { path: 'not-found', component: NotFoundComponent },
  { path: '**', redirectTo: 'not-found' },
];
