import { Component, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-layout',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout implements OnInit, OnDestroy {
  @ViewChild('sidenav') sidenav!: MatSidenav;

  protected readonly sidenavOpened = signal(true);
  protected readonly sidenavMode = signal<'side' | 'over'>('side');

  private breakpointSub!: Subscription;

  constructor(private readonly breakpointObserver: BreakpointObserver) {}

  ngOnInit(): void {
    this.breakpointSub = this.breakpointObserver
      .observe([
        Breakpoints.HandsetPortrait,
        Breakpoints.HandsetLandscape,
        Breakpoints.TabletPortrait,
      ])
      .subscribe((result) => {
        if (result.matches) {
          this.sidenavMode.set('over');
          this.sidenavOpened.set(false);
        } else {
          this.sidenavMode.set('side');
          this.sidenavOpened.set(true);
        }
      });
  }

  ngOnDestroy(): void {
    this.breakpointSub.unsubscribe();
  }

  protected toggleSidenav(): void {
    this.sidenav.toggle();
  }
}
