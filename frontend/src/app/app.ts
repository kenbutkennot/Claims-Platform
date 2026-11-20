import { Component, OnInit } from '@angular/core';
import { TitleCasePipe } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { DemoSessionService } from './core/services/demo-session.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TitleCasePipe],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  constructor(readonly session: DemoSessionService, private readonly router: Router) {}

  ngOnInit(): void {
    void this.session.initialize();
  }

  selectUser(id: string): void {
    const user = this.session.users().find((item) => item.id === id) ?? null;
    this.session.select(user);
    if (user) {
      void this.router.navigate([user.role === 'CLAIMANT' ? '/claims' : '/dashboard']);
    }
  }
}
