# Frontend

React + TypeScript + Vite + Tailwind v4 storefront for Eternal Protocol, built
against the backend's REST API (see [`../backend/BACKEND_API_SPEC.md`](../backend/BACKEND_API_SPEC.md)).

## Stack

- React 19, TypeScript, Vite
- Tailwind CSS v4 (via `@tailwindcss/vite`)
- Zustand, client-side state (auth session, cart, toasts)
- TanStack Query, server-state fetching/caching
- React Router v7, routing + role-based route guards
- Axios, HTTP client, centralized in `api/client.ts`
- `oxlint`, linting

## Running the frontend

The backend must be running first (see [`../backend/README.md`](../backend/README.md))
— the frontend is just a client for that API and won't do anything useful on its own.

```bash
cd frontend
npm install
cp .env.example .env.local   # set VITE_API_BASE_URL, see below
npm run dev                   # → http://localhost:5173
```

**Production build:**

```bash
npm run build     # type-checks (tsc -b) then builds to dist/
npm run preview   # serve the production build locally to sanity-check it
```

**Linting:** `npm run lint` (runs `oxlint`).

## Environment variables

Only one, in `.env.local` (copy from `.env.example`):

| Variable | What it's for |
|---|---|
| `VITE_API_BASE_URL` | Base URL of the Spring Boot backend, no trailing slash. Defaults to `http://localhost:8080` for local dev, every request in `api/client.ts` is built relative to this. Change it if your backend runs on a different port/host, or when pointing at a deployed backend. |

## Project structure

```
src/
├── api/            # One file per backend resource, thin Axios wrappers around
│                    # the REST endpoints (auth.ts, products.ts, orders.ts, ...).
│                    # client.ts holds the shared Axios instance: base URL, auth
│                    # header injection, and centralized 401/403 handling.
├── components/
│   ├── layout/      # Navbar, Footer, AdminNav, UserMenu, CategoryNavDropdown,
│   │                # shared chrome around every page.
│   └── ui/          # Reusable presentational components: Button, FormField,
│                    # DataTable, ProductCard, StatCard, StatusBadge, etc.
├── features/        # One folder per feature area, grouping page + feature-local
│   ├── auth/        # logic together:
│   ├── account/      # - auth: login/register
│   ├── admin/         # - account: profile, order history
│   ├── athlete/         # - admin: product/order/athlete/commission management
│   ├── cart/               # - athlete: the athlete's own dashboard
│   ├── checkout/             # - cart / checkout: guest+logged-in cart, checkout flow
│   └── shop/                   # - shop: storefront browsing, product detail
├── pages/           # Standalone pages that don't need their own feature folder
│                    # (HomePage, ConfirmationPage, NotFoundPage, ForbiddenPage).
├── routes/          # RequireRole.tsx, route guard that redirects based on auth
│                    # state / role before a protected page renders.
├── stores/          # Zustand stores: auth.ts (session/JWT), cart.ts (client-side
│                    # cart + server sync), toast.ts (notification queue).
├── lib/             # Small framework-agnostic helpers: format.ts (currency/date
│                    # formatting), status.ts (shared status→label/colour mapping),
│                    # categories.ts (static category metadata), useClickOutside.ts.
├── types/           # Shared TypeScript types mirroring the backend DTOs.
├── App.tsx          # Route table.
└── main.tsx         # Entry point, mounts React, wraps the app in providers.
```

## What's implemented

- **Storefront**, home page, shop page with colour/size filters (grouped by
  `styleCode`), product detail page with variant selection, guest + logged-in cart.
- **Checkout**, multi-item order submission, athlete discount code live preview,
  PayHere redirect.
- **Order confirmation**, polls `GET /api/orders/:id` until payment actually clears;
  never trusts the `?status=` redirect query param alone (that param can be spoofed
  by the user, so it's only used to decide *when* to start polling, not as proof of
  payment).
- **Auth**, login, register, role-based landing routes, guest-cart-merge-on-login.
- **Customer account**, editable profile, order history.
- **Athlete dashboard**, stats + per-order commission breakdown.
- **Admin**, products (with Cloudinary image upload), orders, athletes (including
  inline commission-rate edit and activate/deactivate), commissions (mark-paid).
- Role-based route guards, centralized 401/403 handling, toast notifications, shared
  status-label mapping.

## Current limitations

- **Delivery fee display**, currently static copy ("payable to the courier on
  delivery") per the client's requirements doc, no calculated delivery estimate.
- **Home page "New arrivals" rail**, currently just the first 8 styles from the
  catalog; there's no real "featured"/"bestseller" flag on the backend yet.

