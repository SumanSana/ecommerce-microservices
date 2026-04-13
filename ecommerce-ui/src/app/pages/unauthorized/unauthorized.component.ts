import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './unauthorized.component.html',
  styleUrls: ['./unauthorized.component.scss']
})
export class UnauthorizedComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  message: string | null = null;

  ngOnInit(): void {
    this.message = this.route.snapshot.queryParamMap.get('message');
  }


  navigateToHome(): void {
    this.router.navigate(['/']);
  }
}