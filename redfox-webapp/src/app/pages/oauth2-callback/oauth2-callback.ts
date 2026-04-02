import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-oauth2-callback',
  template: '<p>Signing in...</p>',
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
