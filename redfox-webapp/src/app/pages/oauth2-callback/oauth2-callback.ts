import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-oauth2-callback',
  imports: [MatProgressSpinnerModule],
  template: '<div class="callback-container"><mat-spinner diameter="48"></mat-spinner></div>',
  styles:
    '.callback-container { display: flex; align-items: center; justify-content: center; height: 100vh; }',
})
export class OAuth2Callback implements OnInit {
  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly authService: AuthService,
  ) {}

  ngOnInit(): void {
    const code = this.route.snapshot.queryParamMap.get('code');
    if (code) {
      this.authService.exchangeCode(code).subscribe({
        next: () => this.router.navigate(['/projects']),
        error: () => this.router.navigate(['/']),
      });
    } else {
      this.router.navigate(['/']);
    }
  }
}
