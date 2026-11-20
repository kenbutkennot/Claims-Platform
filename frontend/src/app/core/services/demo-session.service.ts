import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { DemoUser } from '../models/claims';

const STORAGE_KEY = 'claims-demo-user';

@Injectable({ providedIn: 'root' })
export class DemoSessionService {
  readonly users = signal<DemoUser[]>([]);
  readonly current = signal<DemoUser | null>(null);
  readonly ready = signal(false);
  readonly error = signal('');

  constructor(private readonly http: HttpClient) {}

  async initialize(): Promise<void> {
    try {
      const users = await firstValueFrom(this.http.get<DemoUser[]>('/api/demo/users'));
      this.users.set(users);
      const savedId = localStorage.getItem(STORAGE_KEY);
      const selected = users.find((user) => user.id === savedId)
        ?? users.find((user) => user.role === 'CLAIMANT')
        ?? users[0]
        ?? null;
      this.select(selected);
      if (!selected) this.error.set('No demo users are available. Restart the backend to load seed data.');
    } catch {
      this.error.set('The claims API is unavailable. Start the backend, then refresh this page.');
    } finally {
      this.ready.set(true);
    }
  }

  select(user: DemoUser | null): void {
    this.current.set(user);
    if (user) localStorage.setItem(STORAGE_KEY, user.id);
  }
}
