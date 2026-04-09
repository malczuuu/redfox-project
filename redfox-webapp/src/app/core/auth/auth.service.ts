import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AccessToken } from '../common/common.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authServerUrl = 'http://localhost:8483';
  private readonly clientId = 'webapp-client';
  private readonly redirectUri = 'http://localhost:8482/oauth2/callback';

  private accessToken?: AccessToken;

  constructor(private readonly http: HttpClient) {}

  async login(): Promise<void> {
    const codeVerifier = this.generateCodeVerifier();
    const codeChallenge = await this.generateCodeChallenge(codeVerifier);

    sessionStorage.setItem('pkce_code_verifier', codeVerifier);

    const params = new URLSearchParams({
      response_type: 'code',
      client_id: this.clientId,
      redirect_uri: this.redirectUri,
      scope: 'openid profile',
      code_challenge: codeChallenge,
      code_challenge_method: 'S256',
    });

    window.location.href = `${this.authServerUrl}/oauth2/authorize?${params.toString()}`;
  }

  getAccessTokenValue(): string | undefined {
    return this.accessToken?.accessToken;
  }

  exchangeCode(code: string): Observable<void> {
    const codeVerifier = sessionStorage.getItem('pkce_code_verifier') || '';
    sessionStorage.removeItem('pkce_code_verifier');

    return this.http
      .post<void>('/auth/token', {
        code,
        redirectUri: this.redirectUri,
        codeVerifier,
      })
      .pipe(
        tap((response: any) => {
          this.accessToken = response as AccessToken;
        }),
      );
  }

  refreshToken(): Observable<void> {
    return this.http.post<void>('/auth/refresh', {}).pipe(
      tap((response: any) => {
        this.accessToken = response as AccessToken;
      }),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>('/auth/logout', {}).pipe(
      tap(() => {
        this.accessToken = undefined;
      }),
    );
  }

  logoutFromAuthServer(): void {
    const params = new URLSearchParams({
      redirect_uri: window.location.origin,
    });
    window.location.href = `${this.authServerUrl}/api/logout?${params.toString()}`;
  }

  isAuthenticated(): boolean {
    return !!this.accessToken;
  }

  whoami(): Observable<any> {
    return this.http.get('/api/v1/whoami');
  }

  private generateCodeVerifier(): string {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return this.base64UrlEncode(array);
  }

  private async generateCodeChallenge(verifier: string): Promise<string> {
    const encoder = new TextEncoder();
    const data = encoder.encode(verifier);
    const digest = await crypto.subtle.digest('SHA-256', data);
    return this.base64UrlEncode(new Uint8Array(digest));
  }

  private base64UrlEncode(buffer: Uint8Array): string {
    const base64 = btoa(String.fromCharCode(...buffer));
    return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }
}
