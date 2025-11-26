import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-ff-members',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ff-members.component.html',
  styleUrl: './ff-members.component.scss',
})
export class FfMembersComponent {
  public authService = inject(AuthService);
}
