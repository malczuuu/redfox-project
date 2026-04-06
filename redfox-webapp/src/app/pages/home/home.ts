import { Component, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  imports: [MatButtonModule],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home implements OnInit {
  constructor(
    private readonly authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.authService.whoami().subscribe({
      next: (user) => this.router.navigate(['/projects']),
    });
  }

  protected login(): void {
    this.authService.login();
  }
}
