import { Component, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  imports: [MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home implements OnInit {
  protected loading = signal(true);

  constructor(
    private readonly authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.authService.whoami().subscribe({
      next: () => this.router.navigate(['/projects']),
      error: () => this.loading.set(false),
    });
  }

  protected login(): void {
    this.authService.login();
  }
}
