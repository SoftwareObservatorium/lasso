///
/// LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
/// Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
///
/// This file is part of LASSO.
///
/// LASSO is free software: you can redistribute it and/or modify
/// it under the terms of the GNU General Public License as published by
/// the Free Software Foundation, either version 3 of the License, or
/// (at your option) any later version.
///
/// LASSO is distributed in the hope that it will be useful,
/// but WITHOUT ANY WARRANTY; without even the implied warranty of
/// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/// GNU General Public License for more details.
///
/// You should have received a copy of the GNU General Public License
/// along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
///

import { Component, OnInit, inject } from '@angular/core';

import { FormBuilder, Validators } from '@angular/forms';

import { AuthenticationService } from '../service/authentication.service';
import { ActivatedRoute, Router } from '@angular/router';

import { first } from 'rxjs/operators';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  private fb = inject(FormBuilder);
  loginForm = this.fb.group({
    username: [null, Validators.required],
    password: [null, Validators.required]
  });

  hide = true;
  loading = false;
  error = ''

  returnUrl: string = '/';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authenticationService: AuthenticationService
  ) { 
      // redirect to home if already logged in
      if (this.authenticationService.currentUserValue) { 
          this.router.navigate(['/']);
      }
  }

  ngOnInit(): void {
      // get return url from route parameters or default to '/'
      this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
  }

  onSubmit(): void {
    console.log("login called");
    // stop here if form is invalid
    if (this.loginForm.invalid) {
        return;
    }

    this.loading = true;
    this.authenticationService.login(this.loginForm.controls.username.value, this.loginForm.controls.password.value)
        .pipe(first())
        .subscribe({
          error: (e) => {this.error = e;
            console.log(this.error);
            this.loading = false;},
          complete: () => {this.router.navigate([this.returnUrl])} 
      });
  }
}
