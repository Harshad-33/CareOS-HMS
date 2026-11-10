# CareOS-HMS — Frontend Architecture

## 1. Feature-Based Angular Structure

```text
frontend/src/app/
├── core/
│   ├── constants/       # APP_CONSTANTS, NAVIGATION_ITEMS
│   ├── interceptors/    # httpFoundationInterceptor (X-Correlation-Id, loading, error normalization)
│   ├── layout/          # AppShell (header, responsive sidebar, breadcrumb bar, main router-outlet)
│   ├── models/          # ApiResponse, ApiErrorResponse, HealthStatus, NavItem, BreadcrumbItem
│   └── services/        # ApiClientService, LoadingService, NotificationService, BreadcrumbService, HealthService
├── shared/
│   ├── components/      # PageHeader, LoadingIndicator, EmptyState, ErrorState, ConfirmDialog, ModulePlaceholder
│   ├── directives/      # TrimInputDirective
│   ├── models/          # ConfirmDialogData, PagedResponse, PageQuery
│   └── pipes/           # SafeEmptyPipe
└── features/
    ├── dashboard/       # Workspace Overview (live /api/v1/health verification)
    └── [future modules] # Lazy-loaded module route boundaries:
        hospital, users, employees, patients, appointments, doctors,
        admissions, clinical, laboratory, pharmacy, inventory, billing, reports
```

## 2. Architectural Conventions

- **Standalone Components & Signals:** All components are `standalone: true` and use Angular signals (`signal`, `computed`, `input`, `output`) and modern `@if` / `@for` control flow.
- **Lazy-Loaded Routing:** Every feature module in `app.routes.ts` is loaded via `loadChildren` to keep the initial bundle small and enforce clean feature isolation.
- **Centralized HTTP Communication:** Components never call `HttpClient` directly with raw URLs. Domain services inject `ApiClientService`, which prefixes `environment.apiBaseUrl` and removes empty query parameters.
- **HTTP Interceptor Pipeline:** `httpFoundationInterceptor` attaches `X-Correlation-Id`, tracks active request count in `LoadingService`, and normalizes `HttpErrorResponse` into `NormalizedHttpError` (preserving backend `correlationId` for supportability).
- **Responsive Healthcare UI:** Built on Angular Material 21 (`azure` M3 theme) with shared spacing tokens in `src/styles/_tokens.scss` and reusable `.careos-btn`, `.careos-form-field`, and `.careos-surface-card` classes in `src/styles/_components.scss`. `AppShell` automatically switches between docked `side` mode on desktop (`>= 960px`) and overlay drawer mode on tablet/mobile (`< 960px`).

## 3. Shared UI Building Blocks

| Component / Artifact | Purpose |
|---|---|
| `PageHeaderComponent` | Consistent page title, subtitle, phase/status badge, and `[page-actions]` slot |
| `LoadingIndicatorComponent` | Accessible inline spinner or top progress bar (`mode="inline" | "bar"`) |
| `EmptyStateComponent` | Clear empty/placeholder guidance with optional action button |
| `ErrorStateComponent` | Accessible error card displaying user-safe message, reference correlation ID, and retry action |
| `ConfirmDialogComponent` | Material confirmation dialog (`primary` or `warn` severity) |
| `TrimInputDirective` | Automatically trims leading/trailing whitespace on blur |
| `SafeEmptyPipe` | Displays `—` for null, undefined, or blank text values |

## 4. Adding a New Frontend Module (Future Phases)

1. Replace the placeholder route in `src/app/features/<module>/<module>.routes.ts` with the module's child routes (e.g., list, detail, create/edit).
2. Create module-scoped `services/`, `models/`, and `components/` inside `src/app/features/<module>/`.
3. Inject `ApiClientService` (never raw URLs) and reuse `PageHeaderComponent`, `LoadingIndicatorComponent`, `EmptyStateComponent`, `ErrorStateComponent`, and `ConfirmDialogComponent`.
4. Add `*.spec.ts` unit tests covering service HTTP calls, components, and routes.
