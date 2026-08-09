# Eternal Protocol — Frontend

React + TypeScript + Vite + Tailwind v4 frontend for the Eternal Protocol
storefront, built against `BACKEND_API_SPEC.md` (multi-item order model).

## Setup

```bash
npm install
cp .env.example .env.local   # then set VITE_API_BASE_URL to your Spring Boot backend
npm run dev
```

Build for production:

```bash
npm run build   # outputs to dist/
npm run preview # preview the production build locally
```

## What's implemented

- **Storefront**: home, shop (with colour/size filters, grouped by `styleCode`), product detail with variant selection, guest + logged-in cart.
- **Checkout**: multi-item order submission, athlete discount code live preview, PayHere redirect.
- **Order confirmation**: polls `GET /api/orders/:id` until payment clears — never trusts the `?status=` redirect param alone.
- **Auth**: login, register, role-based landing routes, guest-cart-merge-on-login.
- **Customer account**: editable profile, order history.
- **Athlete dashboard**: stats + per-order commission breakdown.
- **Admin**: products (with Cloudinary image upload), orders, athletes (incl. inline commission-rate edit and activate/deactivate), commissions (mark-paid).
- Role-based route guards, centralized 401/403 handling, toast notifications, shared status-label mapping.

## Known follow-ups

- **Delivery fee display**: currently just static copy ("payable to the courier on delivery") per the client's requirements doc — no calculated delivery estimate. Confirm with the client whether that's still the intended behavior.
- **Home page "New arrivals" rail**: currently just the first 8 styles from the catalog — there's no real "featured"/"bestseller" flag on the backend yet. Worth adding one if the catalog grows.
- **Bundle size**: single JS chunk is ~495KB (~155KB gzipped). Fine for now given the catalog size; if it grows, route-based code-splitting (lazy-loading the `/admin/*` and `/athlete/*` bundles, which a customer never needs) is a straightforward next step.
- Styling is intentionally minimal/functional in places (esp. admin) — polish pass once you have real product photography and brand assets.
