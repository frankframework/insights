import { Routes } from '@angular/router';
import { ReleaseGraphComponent } from './pages/release-graph/release-graph.component';
import { NotFoundComponent } from './components/not-found/not-found.component';
import { ReleaseRoadmapComponent } from './pages/release-roadmap/release-roadmap.component';
import { ReleaseDetailsComponent } from './pages/release-details/release-details.component';
import { ReleaseManageComponent } from './pages/release-manage/release-manage.component';
import { BusinessValueManageComponent } from './pages/release-manage/business-value-manage/business-value-manage.component';
import { VulnerabilityImpactManageComponent } from './pages/release-manage/vulnerability-impact-manage/vulnerability-impact-manage.component';
import { FrankFrameworkMemberGuard } from './guards/frankframework-member.guard';
import { CveOverviewComponent } from './pages/cve-overview/cve-overview.component';

export const routes: Routes = [
  { path: '', redirectTo: 'graph', pathMatch: 'full' },
  {
    path: 'graph',
    children: [
      { path: '', component: ReleaseGraphComponent },
      { path: ':id', component: ReleaseDetailsComponent },
    ],
  },
  { path: 'roadmap', component: ReleaseRoadmapComponent },
  { path: 'cve-overview', component: CveOverviewComponent },
  { path: 'cve-overview/:cveId', component: CveOverviewComponent },
  {
    path: 'vulnerabilities/manage',
    component: VulnerabilityImpactManageComponent,
    canActivate: [FrankFrameworkMemberGuard],
  },
  {
    path: 'vulnerabilities/manage/:cveId',
    component: VulnerabilityImpactManageComponent,
    canActivate: [FrankFrameworkMemberGuard],
  },
  {
    path: 'release-manage/:id',
    canActivate: [FrankFrameworkMemberGuard],
    children: [
      { path: '', component: ReleaseManageComponent },
      { path: 'business-values', component: BusinessValueManageComponent },
      { path: 'business-values/:businessValueId', component: BusinessValueManageComponent },
    ],
  },
  { path: 'not-found', component: NotFoundComponent },
  { path: '**', redirectTo: 'not-found' },
];
