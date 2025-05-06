import { Component } from '@angular/core';
import {ReleaseCoordinatePlaneComponent} from "./pages/release-coordinate-plane/release-coordinate-plane.component";

@Component({
  selector: 'app-root',
	imports: [
		ReleaseCoordinatePlaneComponent
	],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'InsightsFrontend';
}
