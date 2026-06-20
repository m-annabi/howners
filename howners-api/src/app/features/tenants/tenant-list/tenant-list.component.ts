import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { User } from '../../../core/models/user.model';

@Component({
  selector: 'app-tenant-list',
  templateUrl: './tenant-list.component.html',
  styleUrls: ['./tenant-list.component.scss']
})
export class TenantListComponent implements OnInit {
  loading = true;
  tenants: User[] = [];

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    this.http.get<User[]>(`${environment.apiUrl}/rentals/my-tenants`).subscribe({
      next: (t) => { this.tenants = t; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  openProfile(id: string): void {
    this.router.navigate(['/tenants', id]);
  }

  initials(u: User): string {
    return ((u.firstName?.[0] || '') + (u.lastName?.[0] || '')).toUpperCase();
  }
}
