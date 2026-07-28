import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { ThemeService } from './core/theme.service';
import { AuthService } from './core/auth.service';
import { LoginDialogComponent } from './auth/login-dialog.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatDialogModule
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'FatigueTracker';
  protected readonly themeService = inject(ThemeService);
  protected readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);

  openLogin(): void {
    this.dialog.open(LoginDialogComponent, { autoFocus: 'dialog' });
  }

  logout(): void {
    this.auth.logout();
  }
}
