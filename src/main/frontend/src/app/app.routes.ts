import { Routes } from '@angular/router';
import { ReleaseGraphComponent } from './pages/release-graph/release-graph.component';
import { NotFoundComponent } from './components/not-found/not-found.component';
import { ReleaseRoadmapComponent } from './pages/release-roadmap/release-roadmap.component';
import { ReleaseDetailsComponent } from './pages/release-details/release-details.component';
import { ReleaseManageComponent } from './pages/release-manage/release-manage.component';
import { FrankFrameworkMemberGuard } from './guards/frankframework-member.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'graph', pathMatch: 'full' },
  { path: 'graph', component: ReleaseGraphComponent },
  { path: 'graph/:id', component: ReleaseDetailsComponent },
  { path: 'roadmap', component: ReleaseRoadmapComponent },
  { path: 'release-manage/:id', component: ReleaseManageComponent, canActivate: [FrankFrameworkMemberGuard] },
  { path: 'not-found', component: NotFoundComponent },
  { path: '**', redirectTo: 'not-found' },
];
