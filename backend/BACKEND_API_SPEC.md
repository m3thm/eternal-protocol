# Eternal Protocol — Backend API Functionality Spec (As-Built)

This document supersedes the original `Full_Functionality_Spec.md` for backend
scope. It describes every backend API endpoint as actually implemented,
including functionality added beyond the original spec during development.
Organized by module to match the group's task split. New/additional items
not present in the original spec are marked **[NEW]**.

---

## Authentication & Authorization Model

**Unified login across three tables.** A single `POST /api/auth/login`
endpoint checks the `Customer`, `Athlete`, and `Admin` tables (in that
order) for a matching email, wraps whichever one matches in a common
`AppUserDetails`, and issues one JWT format carrying a `role` claim
(`CUSTOMER` / `ATHLETE` / `ADMIN`). Guest customers (no `passwordHash` set)
are skipped during this lookup — they have no credentials to log in with.

**JWT contents:** subject (email), `role` claim, `userId` claim, issued-at,
expiry (24h by default). Signed with HS256.

**Route protection** is centralized in one Spring Security filter chain:

| Route pattern | Access |
|---|---|
| `/api/auth/**` | Public |
| `GET /api/products`, `/api/products/**` | Public |
| `POST /api/orders` | Public |
| `GET /api/orders/*/checkout-redirect` | Public |
| `POST /api/orders/*/confirm-payment` | Public (PayHere webhook) |
| `GET /api/orders/*` | Public |
| `GET /api/athletes/validate-code/**` | Public |
| `POST /PUT /DELETE /api/products/**` (write ops) | `ADMIN` |
| `/api/athletes/**` (all other routes) | `ATHLETE` |
| `/api/customers/**` | `CUSTOMER` |
| `/api/admin/**` | `ADMIN` |
| Anything else | Any authenticated user |

**[NEW] Explicit 401 vs. 403 distinction.** Spring Security's default
fallback behaviour for "no token / invalid token" vs. "valid token, wrong
role" is inconsistent across configurations and commonly returns `403` for
both. This was fixed explicitly with a custom `AuthenticationEntryPoint`
(401 — not authenticated at all) and `AccessDeniedHandler` (403 —
authenticated, but insufficient role), both emitting the same JSON error
shape as `GlobalExceptionHandler` (see below), so a missing token and a
wrong-role token are reliably distinguishable by HTTP status.

**Error response shape** (from both `GlobalExceptionHandler` and the
security-layer handlers above) is consistent across every failure in the
API:
```json
{
  "timestamp": "2026-08-08T10:15:30",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found: 99"
}
```

---

## Module 1 — Storefront & Catalog

### `GET /api/products`
**Access:** Public
**Query params:** `colour` (optional), `size` (optional), `styleCode` (optional) — all combine if present.
**[UPDATED]** Multiple `Product` rows sharing the same `styleCode` represent
one "style" with several colour/size variants (e.g. one tee in Black/M and
White/M is two rows, one `styleCode`). Pass `styleCode` to fetch every
variant of one style — this is what a product detail page's colour/size
selector should call. Grouping by an explicit admin-supplied code rather
than matching on `name` avoids silent grouping breakage from typos and
lets two genuinely different products share a name if that ever comes up.
**Response:** `200` — array of:
```json
{ "id": 1, "styleCode": "PROTOCOL-001", "name": "Protocol 001 Oversized Tee",
  "price": 4500.00, "colour": "Black", "size": "M", "stockQty": 50,
  "imageUrl": "...", "description": "..." }
```

### `GET /api/products/{id}`
**Access:** Public
**Response:** `200` — single `ProductDto` as above, or `404` if not found.

### `POST /api/products`
**Access:** `ADMIN`
**Request body:**
```json
{ "styleCode": "PROTOCOL-001", "name": "...", "price": 4500.00, "colour": "Black", "size": "M",
  "stockQty": 50, "imageUrl": "...", "description": "..." }
```
**Validation:** `styleCode`/`name`/`colour`/`size` required and non-blank;
`price` required and must be `> 0`; `stockQty` required and must be `>= 0`;
`imageUrl`/`description` optional.
**Response:** `200` — the created `ProductDto`. `400` on validation failure.

### `PUT /api/products/{id}`
**Access:** `ADMIN`
Same request/validation shape as create. Full replace of all fields.
**Response:** `200` — updated `ProductDto`, or `404` if the product doesn't exist.

### `DELETE /api/products/{id}`
**Access:** `ADMIN`
**Response:** `204 No Content`, or `404` if the product doesn't exist.

### `[NEW] POST /api/products/upload-image`
**Access:** `ADMIN`
**Not present in the original spec.** Neither the original spec nor the
`Product` entity design specified where product images actually live —
`imageUrl` was just a string field with no upload mechanism behind it. This
endpoint fills that gap: it accepts a `multipart/form-data` file upload
(field name `file`), uploads it to Cloudinary, and returns Cloudinary's
public HTTPS URL. The frontend calls this first, gets a URL back, then
includes that URL as `imageUrl` in a subsequent `POST`/`PUT /api/products`
call — the actual image bytes never touch this backend's own database or
disk; Cloudinary is the permanent store.

**Request:** `multipart/form-data`, field `file`.
**Validation:** file must be present and non-empty; must be `≤ 5MB`; must
be one of `image/jpeg`, `image/png`, `image/webp`, `image/gif`.
**Response:** `200`:
```json
{ "imageUrl": "https://res.cloudinary.com/<cloud>/image/upload/v.../eternal-protocol/products/xyz.png" }
```
`400` on validation failure (wrong type, too large, missing file) or if the
request exceeds the server's multipart size limit (5MB, configured in
`application.yml`). `502 Bad Gateway` if Cloudinary itself is unreachable
or rejects the upload — distinguished from a `400` because in that case the
request was valid but the upstream service failed, not the caller's fault.

---

## Module 2 — Cart, Checkout & Payment

### `GET /api/athletes/validate-code/{code}`
**Access:** Public (shared with Module 3)
**Response:** `200` always (never 404 — an unrecognized code is a valid
"no discount" answer, not an error, so the live checkout preview doesn't
break on a typo):
```json
{ "valid": true, "code": "RISE10", "discountPercentage": 10.00 }
```
Inactive athlete codes also resolve to `"valid": false`.

### `[BREAKING CHANGE] POST /api/orders`
**Access:** Public. If the request carries a valid `CUSTOMER` JWT, the
order is linked to that account (and their persisted cart is cleared on
success); otherwise a fresh guest `Customer` row is created (a guest and a
registered customer are the same entity — only `passwordHash` being set
distinguishes them).

**This endpoint's request/response shape changed from a single-product
model to a multi-item one.** The original spec (and an earlier version of
this backend) modeled checkout as exactly one product/size/colour per
order — which meant a cart with 2 tees + 1 hoodie could only become 3
separate orders and 3 separate PayHere payments for what the customer
thinks is one purchase. `Order` now holds a list of `OrderItem` rows
instead; one request, one order, one payment, any number of distinct
products and quantities.

**Request body:**
```json
{ "customerName": "Jane Doe", "phone": "0771234567", "address": "123 Main St",
  "items": [
    { "productId": 1, "size": "M", "colour": "Black", "quantity": 2 },
    { "productId": 7, "size": "L", "colour": "Grey", "quantity": 1 }
  ],
  "athleteCode": "RISE10" }
```
`athleteCode` is optional and applies once to the whole order (not per
line — an athlete's discount code discounts the entire purchase). `items`
must be non-empty; each item requires `productId`, `size`, `colour`, and
`quantity` (`>= 1`).

**Business logic, in order:**
1. Look up every `productId` in `items`; `404` if any doesn't exist.
2. **Stock check across ALL items before creating or decrementing
   anything.** If any item's requested quantity exceeds that product's
   `stockQty`, the whole order is rejected with `409 Conflict` listing
   every insufficient item (`"Not enough stock for: \"X\" (colour/size) —
   requested N, only M left; ..."`) — not a partial order with only the
   in-stock items included.
3. If `athleteCode` is present, look it up. An invalid or inactive code is
   **silently ignored** — checkout still proceeds at full price, it does
   not fail the request.
4. Resolve or create the `Customer` row (see above).
5. Compute `subtotalAmount` = sum of `(product.price × quantity)` across
   every item. If a valid athlete code was found, `discountAmount =
   subtotalAmount × commissionRate / 100` (rounded to 2dp, half-up) —
   applied to the order total, not per line.
6. Save the `Order` (status `PENDING`) with all its `OrderItem` rows,
   each snapshotting that product's price at order time (`unitPrice`) so a
   later price change never rewrites what a past customer actually paid.
7. **Decrement `stockQty` per item**, by that item's own quantity. Stock is
   reserved at order-creation time (checkout submission), not at payment
   confirmation — so two customers can't both "buy" the last unit while one
   is still on the PayHere payment page. Known tradeoff: this is a
   read-then-write, not a DB row lock, so it isn't fully race-proof under
   truly simultaneous requests.
8. If a valid athlete code was used, create a linked `Commission` row
   (still one per order, not per item), status `AWAITING_PAYMENT`.
9. If the request came from a logged-in customer, clear their persisted
   cart (`DELETE`-equivalent on every `CartItem` for that customer) — the
   items they just bought shouldn't still show as "in cart" afterward.
10. Call `PaymentService.initiatePayment(...)` to get a payment URL.

**Response:** `200`:
```json
{ "orderId": 100, "totalAmount": 15300.00, "paymentUrl": "http://.../api/orders/100/checkout-redirect" }
```
`404` if any product not found. `409` if any item is out of stock. `400`
on validation failure (including an empty `items` array).

### `[NEW] GET /api/orders/{id}/checkout-redirect`
**Access:** Public.
**Not present in the original spec.** The spec says checkout "initiates
PayHere payment" and returns "a payment redirect URL," but doesn't specify
the mechanism. PayHere's hosted checkout requires an HTML form POST signed
with a merchant-secret hash — not a plain GET link — and that secret must
never reach the browser. So `paymentUrl` from `POST /api/orders` actually
points here: this endpoint returns a tiny auto-submitting HTML page
(`Content-Type: text/html`) containing the signed form, which immediately
POSTs itself to PayHere's real checkout URL. The frontend's
`window.location.href = order.paymentUrl` works unmodified — it just lands
here for a fraction of a second first. PayHere's `items` field on that form
is a plain display string built by joining every order line as
`"Product Name xQuantity"` (e.g. `"Protocol 001 Tee x2, Protocol 002 Hoodie x1"`).

### `POST /api/orders/{id}/confirm-payment`
**Access:** Public — but only meaningfully callable by PayHere itself.
This is PayHere's server-to-server `notify_url` webhook, called directly
by PayHere's servers, not by the frontend. The `notify_url` sent to
PayHere is unique per order (`.../api/orders/{id}/confirm-payment`), which
is how this per-order path shape is possible despite PayHere only ever
being told one URL per transaction.

**[UPDATED] `return_url` and `cancel_url` are also built per-order**, not
from fixed config values — same reasoning as `notify_url`. Both point at
one shared frontend route: `{frontend}/order/{id}/confirmation`, PayHere
redirects the browser here after either a successful payment (`return_url`,
with `?status=success`) or a cancelled/failed one (`cancel_url`, with
`?status=cancelled`). One route handles both outcomes, distinguished only
by that query param.

**This `status` query param is a UX hint only — never proof of payment.**
A customer can land on this URL without having actually paid (manually
typing it, a slow or dropped webhook, etc.), so the frontend confirmation
page must always call `GET /api/orders/{id}` and read the authoritative
`paymentStatus` field from there. That field only ever flips to `PAID`
inside this endpoint, after the md5 signature below has been verified —
never from the browser redirect alone.

**Request:** PayHere posts `application/x-www-form-urlencoded` with
`merchant_id`, `order_id`, `payhere_amount`, `payhere_currency`,
`status_code`, `md5sig`.

**Business logic:**
1. Recompute the expected md5 signature server-side from the merchant
   secret and compare against `md5sig`. **This check is not optional** —
   without it, anyone could POST a fake "payment succeeded" call.
2. Only if the signature is valid **and** `status_code == "2"` (PayHere's
   "successfully completed" code): mark the order `PAID`, and flip its
   linked commission (if any) from `AWAITING_PAYMENT` to `EARNED`.
3. Idempotent — calling this twice on an already-`PAID` order does nothing
   the second time (PayHere's webhook can fire more than once).

**Response:** `200` always (PayHere expects this regardless of whether the
signature check passed internally — a non-200 causes PayHere to retry the
webhook).

### `GET /api/orders/{id}`
**Access:** Public (used by the order confirmation page).
**[UPDATED]** Response shape now carries a line-item array instead of a
single product/size/colour/quantity.
**Response:** `200`:
```json
{ "id": 100,
  "items": [
    { "productId": 1, "productName": "Protocol 001 Tee", "size": "M", "colour": "Black",
      "quantity": 2, "unitPrice": 4500.00, "lineTotal": 9000.00 },
    { "productId": 7, "productName": "Protocol 002 Hoodie", "size": "L", "colour": "Grey",
      "quantity": 1, "unitPrice": 8000.00, "lineTotal": 8000.00 }
  ],
  "subtotalAmount": 17000.00, "discountAmount": 1700.00, "totalAmount": 15300.00,
  "paymentStatus": "PENDING", "athleteCode": "RISE10", "orderDate": "..." }
```
`404` if not found.

### Customer accounts

#### `POST /api/auth/register`
**Access:** Public. Customer self-registration only — athletes and admins
are never self-registered.
**Request:** `{ "name", "email", "password", "phone", "address" }`.
**Validation:** all required; `email` must be valid format; `password`
minimum 6 characters.
**Business logic:** rejects if the email is already registered (`409`).
Creates a `Customer` row with `passwordHash` set (BCrypt), immediately logs
them in (returns a token) — no separate login step needed after registering.
**Response:** `200` — `{ "token", "role": "CUSTOMER", "userId" }`. `409` on
duplicate email. `400` on validation failure.

#### `GET /api/customers/me`
**Access:** `CUSTOMER`. Identity always comes from the JWT via
`SecurityContextHolder`, never a path parameter.
**Response:** `200` — `{ "id", "name", "email", "phone", "address" }`.

#### `[NEW] PUT /api/customers/me`
**Access:** `CUSTOMER`.
**Request:** `{ "name", "phone", "address" }` — all required, non-blank.
Deliberately does **not** cover `email` or `password` — changing a login
email needs a uniqueness re-check, and changing a password needs
current-password verification; both are bigger, separate features than
"let me fix my delivery address," so they're intentionally out of scope
here for now.
**Response:** `200` — updated `CustomerProfileDto` (email unchanged).
`400` on validation failure.

#### `GET /api/customers/me/orders`
**Access:** `CUSTOMER`.
**Response:** `200` — array of `OrderDetailDto` (see shape above), most
recent first. Guest orders never appear here for anyone, since a guest
checkout has no associated login.

#### `GET /api/customers/me/cart`
**Access:** `CUSTOMER`.
**Response:** `200` — array of:
```json
{ "id": 1, "productId": 10, "productName": "...", "price": 4500.00,
  "size": "M", "colour": "Black", "quantity": 2, "imageUrl": "..." }
```

#### `POST /api/customers/me/cart`
**Access:** `CUSTOMER`.
**Request:** `{ "productId", "size", "colour", "quantity" }` — quantity
must be `>= 1`.
**Business logic:** if a line with the same product+size+colour already
exists in this customer's cart, its quantity is **incremented** by the
requested amount rather than creating a duplicate line.
**Response:** `200` — the resulting `CartItemDto`. `404` if the product
doesn't exist.

#### `DELETE /api/customers/me/cart/{itemId}`
**Access:** `CUSTOMER`. Lookup is scoped to `(itemId, callingCustomerId)`
together — a customer attempting to delete another customer's cart item by
guessing an ID gets an ordinary `404`, indistinguishable from the item
simply not existing.
**Response:** `204`, or `404`.

#### `[NEW] PUT /api/customers/me/cart/{itemId}`
**Access:** `CUSTOMER`. Same ownership scoping as the delete endpoint above.
**Not the same as `POST /api/customers/me/cart`** — that endpoint
*increments* an existing line's quantity (or creates a new one); this one
**sets** the quantity to an exact value. This is what a "−"/"+" stepper or
a typed quantity field on the cart page should call — using the `POST`
endpoint for that would keep adding on top of the current quantity instead
of replacing it.
**Request:** `{ "quantity" }` — required, must be `>= 1`.
**Response:** `200` — updated `CartItemDto`. `404` if the item doesn't
belong to the caller. `400` if quantity is missing or `< 1`.

---

## Module 3 — Athlete System

### `GET /api/athletes/dashboard`
**Access:** `ATHLETE`. The athlete ID is always taken from the JWT via
`CurrentUser`, **never** from a URL parameter or request body — this is
deliberate and tested explicitly, so one athlete cannot view another
athlete's commission data by guessing or editing an ID.

**Response:** `200`:
```json
{
  "athleteCode": "RISE10",
  "commissionRate": 10.00,
  "totalOrders": 3,
  "salesGenerated": 13500.00,
  "commissionEarned": 900.00,
  "paidCommission": 450.00,
  "pendingCommission": 450.00,
  "orders": [
    { "orderId": 101,
      "items": [
        { "productId": 1, "productName": "Protocol 001 Tee", "size": "M", "colour": "Black",
          "quantity": 1, "unitPrice": 4500.00, "lineTotal": 4500.00 }
      ],
      "orderDate": "...", "paymentStatus": "PAID",
      "commissionAmount": 450.00, "commissionStatus": "EARNED" }
  ]
}
```
**[UPDATED]** Each order summary now carries its own `items` array (via the
shared `OrderLineItemDto` shape) instead of a single `productName` — an
order using an athlete's code can itself contain multiple products, and the
dashboard needs to show all of them, not just one.
- `salesGenerated` is the sum of every commission-linked order's
  `subtotalAmount` (the full multi-item order value, before discount) —
  not a single product's price.
- `commissionEarned` = sum of `EARNED` + `PAID` commissions (i.e. every
  order where payment was actually confirmed).
- `paidCommission` = `PAID` only (already paid out by admin).
- `pendingCommission` = `EARNED` only (payment confirmed, payout not yet made).

### `GET /api/athletes/validate-code/{code}`
Shared with Module 2 — see above.

**[NEW — naming clarification, not new functionality]** The original spec
uses "pending commission" to describe two different states: (a) an order
placed but payment not yet confirmed, and (b) a commission earned but not
yet paid out. As implemented, these are two distinct enum values —
`AWAITING_PAYMENT` and `EARNED` respectively — specifically so the code
never conflates them under one ambiguous "PENDING" label. The full lifecycle
is `AWAITING_PAYMENT → EARNED → PAID`.

---

## Module 4 — Admin Dashboard

All routes below require `ADMIN` — enforced once, centrally, for the whole
`/api/admin/**` path rather than per-endpoint.

### `GET /api/admin/orders`
**Query params:** `status` (optional — `PENDING` / `PAID` / `CANCELLED`).
**[UPDATED]** Carries a line-item array instead of a single product.
**Response:** `200` — array of:
```json
{ "id": 100, "customerName": "...", "customerPhone": "...", "customerAddress": "...",
  "items": [
    { "productId": 1, "productName": "Protocol 001 Tee", "size": "M", "colour": "Black",
      "quantity": 2, "unitPrice": 4500.00, "lineTotal": 9000.00 }
  ],
  "subtotalAmount": 9000.00, "totalAmount": 9000.00, "discountAmount": 0.00,
  "paymentStatus": "PAID", "athleteCode": "RISE10", "orderDate": "..." }
```
(This is a customer-enriched view distinct from the public `OrderDetailDto`
— it exposes delivery contact info, which only admins should see.)

### `GET /api/admin/athletes`
**Response:** `200` — array of `{ "id", "name", "email", "athleteCode", "commissionRate", "active" }`.

### `POST /api/admin/athletes`
**Request:** `{ "name", "email", "password", "athleteCode", "commissionRate" }`.
**Validation:** all required; `password` min 6 chars; `commissionRate`
between 0–100.
**Business logic:** rejects duplicate email (`409` via
`EmailAlreadyInUseException`) or duplicate athlete code (`400`). Password
is BCrypt-hashed before storage. New athlete is `active: true` by default.
**Response:** `200` — the created `AthleteDto`.

### `PUT /api/admin/athletes/{id}`
**Request:** `{ "commissionRate": nullable, "active": nullable }` — both
fields optional; omitting a field leaves it unchanged (this is a partial
update, not a full replace).
**Response:** `200` — updated `AthleteDto`, or `404` if the athlete doesn't
exist. Deactivating an athlete (`active: false`) takes effect immediately —
they can no longer log in on their next attempt (`CustomUserDetailsService`
checks `active` status at login time).

### `GET /api/admin/commissions`
**Query params:** `status` (optional — `AWAITING_PAYMENT` / `EARNED` / `PAID`).
**Response:** `200` — array of:
```json
{ "id": 50, "athleteName": "...", "athleteCode": "RISE10", "orderId": 100,
  "commissionAmount": 450.00, "commissionStatus": "EARNED", "paidDate": null }
```

### `PUT /api/admin/commissions/{id}/mark-paid`
**Business logic:** only a commission currently in `EARNED` status can be
marked paid — attempting this on `AWAITING_PAYMENT` (payment not even
confirmed yet) or already-`PAID` commissions returns `400` with an
explanatory message, rather than silently succeeding or corrupting state.
On success, sets status to `PAID` and stamps `paidDate` with the current time.
**Response:** `200` — updated `CommissionDto`. `404` if the commission
doesn't exist. `400` if not in `EARNED` status.

### Product management
Create/update/delete live under `/api/products` (Module 1's controller,
per the original spec's own note that these are "consumed by Module 4's
admin UI" despite living in the Product resource's routes) — see Module 1
above.

---

## Summary of all deviations from the original Full_Functionality_Spec.md

| # | What | Why |
|---|---|---|
| 1 | Added `GET /api/orders/{id}/checkout-redirect` | PayHere requires a signed HTML form POST, not a plain link; the spec described the outcome ("payment redirect URL") but not the mechanism, which has a real technical constraint |
| 2 | Added stock tracking + enforcement (`409` on insufficient stock, decrement on order) | `Product.stockQty` existed in the entity design but was never checked or decremented anywhere in the original functionality list — this is normal e-commerce behavior that was simply missing |
| 3 | Split "pending" into `AWAITING_PAYMENT` / `EARNED` / `PAID` | The original spec used "pending" for two different commission states in two different sentences; this is a naming clarification with no behavioral difference from what was described |
| 4 | Explicit 401 vs. 403 error handling | Not mentioned in the original spec at all; added because Spring Security's default behavior here is inconsistent and a REST API should distinguish "not logged in" from "logged in, wrong role" |
| 5 | 4-field error JSON (`timestamp`, `status`, `error`, `message`) vs. the spec's 2-field example | Superset of the spec's illustration, not a contradiction of it |
| 6 | Added `POST /api/products/upload-image` + Cloudinary integration | The spec never addressed *where* product images are actually stored — `imageUrl` was just an unbacked string field. This endpoint provides real file upload/storage, which the site genuinely needs to function |
| 7 | Added `styleCode` field to `Product`, plus a `styleCode` filter on `GET /api/products` | Neither doc specified how variants (same product, different colour/size) should be grouped into one "style" for the Shop page. Name-matching is fragile; this is an explicit, admin-controlled grouping key |
| 8 | Added `PUT /api/customers/me` | Customers had no way to self-edit their delivery details after registering — a normal e-commerce expectation the original spec didn't call out explicitly |
| 9 | Added `PUT /api/customers/me/cart/{itemId}` | The cart only supported add-and-increment or full removal — no way to set an exact quantity, which any real quantity stepper UI needs |
| 10 | `return_url`/`cancel_url` are now built per-order (like `notify_url`), both routing to one shared frontend confirmation page distinguished by a `status` query param | Originally these were static config values with no order ID attached at all — the frontend had no way to know which order a PayHere redirect was even about |
| 11 | **[BREAKING]** `Order` restructured from one-product-per-order to a multi-item model (`items: List<OrderItem>`); `POST /api/orders` request/response and every read DTO that includes order data (`OrderDetailDto`, `AdminOrderDto`, `AthleteOrderSummaryDto`) changed shape accordingly | The single-item model made it structurally impossible to check out a multi-product cart as one order/one payment — a cart with 2 tees + 1 hoodie could only become 3 separate PayHere payments. Fixed at the schema level rather than papered over in the frontend, per explicit direction |
| 12 | Added `@Transactional(readOnly = true)` to every read-path service method that maps an entity with LAZY associations to a DTO (`OrderService`, `AthleteService`, `CartService`) | Pre-existing latent bug, not something introduced by item #11 — but the multi-item change made it certain to surface, since `Order.items` is a `@OneToMany` collection and those are *always* LAZY. Without this, these endpoints work fine under test (Spring's test transaction happens to keep the JPA session open) but throw `LazyInitializationException` against real, unwrapped HTTP traffic from an actual frontend |

No endpoint, business rule, or access-control requirement listed in the
original `Full_Functionality_Spec.md` was omitted, weakened, or altered in
meaning. Every deviation above is additive or clarifying.
