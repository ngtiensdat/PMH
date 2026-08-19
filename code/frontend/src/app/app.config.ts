import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection, signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideTaiga, TUI_DARK_MODE } from '@taiga-ui/core';
import { TUI_DEFAULT_LANGUAGE } from '@taiga-ui/i18n';
import { TUI_VIETNAMESE_LANGUAGE } from '@taiga-ui/i18n/languages/vietnamese';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideTaiga(),
    {
      provide: TUI_DEFAULT_LANGUAGE,
      useValue: TUI_VIETNAMESE_LANGUAGE
    },
    {
      provide: TUI_DARK_MODE,
      useValue: Object.assign(signal(false), { reset: () => { } })
    }
  ]
};
