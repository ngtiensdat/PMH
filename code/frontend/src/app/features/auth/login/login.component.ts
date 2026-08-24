import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { LanguageService } from '../../../core/services/language.service';
import { NotificationService } from '../../../shared/components/notification/notification.service';
import { SharedTaigaModule } from '../../../shared/shared-taiga.module';

import { LoginSchema, zodFieldValidator } from '../../../shared/validators/login.schema';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    SharedTaigaModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  public languageService = inject(LanguageService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  isLoading = signal<boolean>(false);
  showPassword = signal<boolean>(false);

  loginForm = this.fb.group({
    username: ['make', [Validators.required, zodFieldValidator(LoginSchema, 'username')]],
    password: ['123', [Validators.required, zodFieldValidator(LoginSchema, 'password')]]
  });

  fillAccount(username: string) {
    this.loginForm.patchValue({
      username: username,
      password: '123'
    });
  }

  onSubmit() {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    const credentials = this.loginForm.value as { username: string; password: string };

    this.authService.login(credentials).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        const welcomePrefix = this.languageService.labels().messages.success.login;
        this.notificationService.success(`${welcomePrefix}: ${res.data.fullName} (${res.data.role})`);
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/categories';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err) => {
        this.isLoading.set(false);
        const fallbackMsg = this.languageService.labels().messages.errorPrefix.loginFailed;
        const errMsg = err.error?.message || fallbackMsg;
        this.notificationService.error(errMsg);
      }
    });
  }
}
