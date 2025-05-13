import { Component } from '@angular/core';
import {ReleaseGraphComponent} from "./pages/release-graph/release-graph.component";

@Component({
  selector: 'app-root',
	imports: [
		ReleaseGraphComponent
	],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'InsightsFrontend';
}
