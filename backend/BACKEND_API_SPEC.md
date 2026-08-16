# Backend API Reference

Every backend REST endpoint as currently implemented, organized by the four module
splits used to divide work across the group (see the root and backend READMEs for
how these map to files/classes).

---

## Authentication & Authorization Model

This applies across every module below.

**Unified login across three tables.** A single `POST /api/auth/login` endpoint
checks the `Customer`, `Athlete`, and `Admin` tables (in that order) for a matching
email, wraps whichever one matches in a common `AppUserDetails`, and issues one JWT
format carrying a `role` claim (`CUSTOMER` / `ATHLETE` / `ADMIN`). Guest customers (no
`passwordHash` set) are skipped during this lookup, they have no credentials to log
in with.

**JWT contents:** subject (email), `role` claim, `userId` claim, issued-at, expiry
(24h by default). Signed with HS256.

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

**401 vs. 403 are distinguished explicitly.** A custom `AuthenticationEntryPoint`
returns `401` for "no token / invalid token", and a custom `AccessDeniedHandler`
returns `403` for "valid token, wrong role", both emit the same JSON error shape as
`GlobalExceptionHandler` (below), so the two failure modes are reliably
distinguishable by HTTP status alone.

**Error response shape** is consistent across every failure in the API:
```json
{
  "timestamp": "2026-08-08T10:15:30",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found: 99"
}
```

---

## Authentication & Customer Account

### `POST /api/auth/login`
**Access:** Public.<br>
**Request:** `{ "email", "password" }`.<br>
**Response:** `200`, `{ "token", "role", "userId" }`. `401` on invalid credentials.

### `POST /api/auth/register`
**Access:** Public. Customer self-registration only, athletes and admins are never
self-registered (they're created by an admin).<br>
**Request:** `{ "name", "email", "password", "phone", "address" }`.<br>
**Validation:** all required; `email` must be valid format; `password` minimum 6
characters.<br>
**Business logic:** rejects if the email is already registered (`409`). Creates a
`Customer` row with `passwordHash` set (BCrypt) and immediately logs them in, no
separate login call needed right after registering.<br>
**Response:** `200`, `{ "token", "role": "CUSTOMER", "userId" }`. `409` on duplicate
email. `400` on validation failure.

### `GET /api/customers/me`
**Access:** `CUSTOMER`. Identity always comes from the JWT via
`SecurityContextHolder`, never a path parameter.<br>
**Response:** `200`, `{ "id", "name", "email", "phone", "address" }`.

### `PUT /api/customers/me`
**Access:** `CUSTOMER`.<br>
**Request:** `{ "name", "phone", "address" }`, all required, non-blank. This does
**not** cover `email` or `password`, changing a login email needs a uniqueness
re-check, and changing a password needs current-password verification; both are
separate concerns from editing a delivery profile, so they're intentionally not part
of this endpoint.<br>
**Response:** `200`, updated `CustomerProfileDto` (email unchanged). `400` on
validation failure.

---

## Shop & Product Catalog

### `GET /api/products`
**Access:** Public.<br>
**Query params:** `colour`, `size`, `styleCode`, `category` (one of
`MENS`/`WOMENS`/`ACCESSORIES`), `subCategory`, all optional, all combine if present.
Multiple `Product` rows sharing the same `styleCode` represent one "style" with
several colour/size variants (e.g. one tee in Black/M and White/M is two rows, one
`styleCode`); pass `styleCode` to fetch every variant of one style, which is what a
product detail page's colour/size selector calls. `category`/`subCategory` are what
the storefront's MEN'S/WOMEN'S/ACCESSORIES navbar sections (and their subcategory
dropdowns) filter by, e.g. `?category=MENS&subCategory=Hoodies`.<br>
**Response:** `200`, array of:
```json
{ "id": 1, "styleCode": "PROTOCOL-001", "name": "Protocol 001 Oversized Tee",
  "category": "MENS", "subCategory": "T-Shirts",
  "price": 4500.00, "colour": "Black", "size": "M", "stockQty": 50,
  "imageUrl": "...", "description": "..." }
```

### `GET /api/products/{id}`
**Access:** Public.<br>
**Response:** `200`, single `ProductDto` as above, or `404` if not found.

### `GET /api/products/subcategories`
**Access:** Public.<br>
**Query params:** `category` (required, one of `MENS`/`WOMENS`/`ACCESSORIES`).
Powers the navbar's per-category dropdown: returns the distinct `subCategory` values
that currently have at least one product in that category, so the dropdown only ever
shows subcategories that actually exist right now. There's no fixed, hardcoded list
of subcategories, whatever an admin types into a product's subcategory field is
what shows up here once at least one product uses it.<br>
**Response:** `200`:
```json
["Bottoms", "Hoodies", "T-Shirts"]
```

### `POST /api/products`
**Access:** `ADMIN`.<br>
**Request body:**
```json
{ "styleCode": "PROTOCOL-001", "name": "...", "category": "MENS", "subCategory": "T-Shirts",
  "price": 4500.00, "colour": "Black", "size": "M",
  "stockQty": 50, "imageUrl": "...", "description": "..." }
```
**Validation:** `styleCode`/`name`/`category`/`subCategory`/`colour`/`size` required
and non-blank; `price` required and must be `> 0`; `stockQty` required and must be
`>= 0`; `imageUrl`/`description` optional.
**`imageUrl` is normally derived automatically, not sent by the admin UI.** If this
style+colour already has a photo gallery (see below) with a cover photo set,
`imageUrl` on the saved `Product` row is overwritten with that cover photo's URL
regardless of what's in the request body. The request-supplied `imageUrl` is only
used as a fallback when no gallery cover exists yet for that style+colour.<br>
**Response:** `200`, the created `ProductDto`. `400` on validation failure.

### `PUT /api/products/{id}`
**Access:** `ADMIN`.
Same request/validation shape as create, including the `imageUrl` gallery-cover-wins
behaviour described above. Full replace of all fields.<br>
**Response:** `200`, updated `ProductDto`, or `404` if the product doesn't exist.

### `DELETE /api/products/{id}`
**Access:** `ADMIN`.
Deleting a `Product` row (one size+colour combination) also:
- Clears any live shopping-cart entries referencing it first (carts are just
  in-progress shopper state, so this is safe to clear rather than block on).
- If this was the *last* remaining size for its style+colour, purges that colour's
  entire photo gallery, both the `ProductImage` rows and their underlying
  Cloudinary assets. If other sizes of the same colour still exist, the gallery is
  left alone since they still display it.

If the product appears in any **past order**, the delete is refused with `409
Conflict` and a message naming the product and suggesting the admin set its stock to
`0` instead, hard-deleting a product that was actually sold would otherwise either
be blocked by a database foreign-key constraint or, if forced through, silently
corrupt what a customer actually paid for.<br>
**Response:** `204 No Content` on success, `404` if the product doesn't exist, `409`
if it's referenced by a past order.

### `POST /api/products/upload-image`
**Access:** `ADMIN`.
Accepts a `multipart/form-data` file upload (field name `file`), uploads it to
Cloudinary, and returns Cloudinary's public HTTPS URL **and** its `public_id`. The
frontend calls this first to get both back, then attaches them to a style+colour's
photo gallery via `POST /api/products/style/{styleCode}/images` below, the actual
image bytes never touch this backend's own database or disk; Cloudinary is the
permanent store. The `public_id` is kept (on the `ProductImage` row) purely so a
later delete can remove the exact Cloudinary asset instead of trying to
reverse-engineer one out of the URL string.

**Request:** `multipart/form-data`, field `file`.<br>
**Validation:** file must be present and non-empty; must be `≤ 5MB`; must be one of
`image/jpeg`, `image/png`, `image/webp`, `image/gif`.<br>
**Response:** `200`:
```json
{ "imageUrl": "https://res.cloudinary.com/<cloud>/image/upload/v.../eternal-protocol/products/xyz.png",
  "publicId": "eternal-protocol/products/xyz" }
```
`400` on validation failure (wrong type, too large, missing file) or if the request
exceeds the server's multipart size limit (5MB, configured in `application.yml`).
`502 Bad Gateway` if Cloudinary itself is unreachable or rejects the upload.

### Product Image Gallery
Each style+colour combination can have multiple photos (a product-page carousel
showing different angles of the same item), with one of them designated the cover
photo shown on the shop grid. Photos are keyed on `(styleCode, colour)` rather than
on an individual `Product` row: a style+colour can have several `Product` rows (one
per size), and all of them share the same photos, so uploading once here covers
every size.

**`GET /api/products/style/{styleCode}/images`**<br>
**Access:** Public.
Every photo for every colour of this style, ordered by colour then by carousel
position.<br>
**Response:** `200`, array of:
```json
{ "id": 1, "colour": "Black", "imageUrl": "...", "sortOrder": 0, "isCover": true }
```
The frontend groups this flat list by `colour` client-side (e.g. for the
product-detail-page carousel, which swaps its whole image set when the shopper picks
a different colour).

**`POST /api/products/style/{styleCode}/images`**<br>
**Access:** `ADMIN`. Call `/api/products/upload-image` first to get
`imageUrl`/`publicId`, then attach it here.<br>
**Request body:** `{ "colour": "Black", "imageUrl": "...", "publicId": "...", "sortOrder": 0 }`
(`publicId` and `sortOrder` optional, `sortOrder` defaults to appending to the end
of this colour's gallery). The first photo uploaded for a new colour is
automatically marked as its cover (`isCover: true`); the admin can change this
afterward via the cover endpoint below.<br>
**Response:** `200`, the created `ProductImageDto`.

**`PUT /api/products/images/{imageId}/cover`**<br>
**Access:** `ADMIN`.
Marks this gallery photo as the cover for its style+colour, the one shown as
`Product.imageUrl` on the shop grid and as the PDP carousel's first frame,
un-marking whichever photo held that role before (exactly one cover per style+colour
at all times).<br>
**Response:** `200`, the updated `ProductImageDto`.

**`DELETE /api/products/images/{imageId}`**<br>
**Access:** `ADMIN`.
Deletes the gallery row **and** its underlying Cloudinary asset (via `public_id`). If
the deleted photo was the cover, the next photo in carousel order is automatically
promoted to cover so the colour is never left with photos but no cover.<br>
**Response:** `204 No Content`.

**`PUT /api/products/style/{styleCode}/images/reorder`**<br>
**Access:** `ADMIN`. Query param: `colour`.<br>
**Request body:** `{ "orderedImageIds": [3, 1, 2] }`, every image ID for that
colour, in the new desired carousel order.<br>
**Response:** `200`, the updated `ProductImageDto[]` in the new order.

---

## Cart, Checkout & Orders

### `GET /api/customers/me/cart`
**Access:** `CUSTOMER`.<br>
**Response:** `200`, array of:
```json
{ "id": 1, "productId": 10, "productName": "...", "price": 4500.00,
  "size": "M", "colour": "Black", "quantity": 2, "imageUrl": "..." }
```

### `POST /api/customers/me/cart`
**Access:** `CUSTOMER`.<br>
**Request:** `{ "productId", "size", "colour", "quantity" }`, quantity must be
`>= 1`.<br>
**Business logic:** if a line with the same product+size+colour already exists in
this customer's cart, its quantity is **incremented** by the requested amount
rather than creating a duplicate line.<br>
**Response:** `200`, the resulting `CartItemDto`. `404` if the product doesn't
exist.

### `PUT /api/customers/me/cart/{itemId}`
**Access:** `CUSTOMER`. Lookup is scoped to `(itemId, callingCustomerId)` together,
a customer attempting to touch another customer's cart item by guessing an ID gets
an ordinary `404`, indistinguishable from the item simply not existing.
**Not the same as `POST /api/customers/me/cart`**, that endpoint *increments* an
existing line's quantity; this one **sets** the quantity to an exact value. This is
what a "−"/"+" stepper or a typed quantity field on the cart page should call.<br>
**Request:** `{ "quantity" }`, required, must be `>= 1`.<br>
**Response:** `200`, updated `CartItemDto`. `404` if the item doesn't belong to the
caller. `400` if quantity is missing or `< 1`.

### `DELETE /api/customers/me/cart/{itemId}`
**Access:** `CUSTOMER`. Same ownership scoping as above.<br>
**Response:** `204`, or `404`.

### `GET /api/customers/me/orders`
**Access:** `CUSTOMER`.<br>
**Response:** `200`, array of `OrderDetailDto` (see shape under `GET
/api/orders/{id}` below), most recent first. Guest orders never appear here for
anyone, since a guest checkout has no associated login.

### `POST /api/orders`
**Access:** Public. If the request carries a valid `CUSTOMER` JWT, the order is
linked to that account (and their persisted cart is cleared on success); otherwise a
fresh guest `Customer` row is created (a guest and a registered customer are the same
entity, only `passwordHash` being set distinguishes them).

An order holds a list of `OrderItem` rows, one request, one order, one payment, any
number of distinct products and quantities in a single checkout.

**Request body:**
```json
{ "customerName": "Jane Doe", "phone": "0771234567", "address": "123 Main St",
  "items": [
    { "productId": 1, "size": "M", "colour": "Black", "quantity": 2 },
    { "productId": 7, "size": "L", "colour": "Grey", "quantity": 1 }
  ],
  "athleteCode": "RISE10" }
```
`athleteCode` is optional and applies once to the whole order (an athlete's discount
code discounts the entire purchase, not individual lines). `items` must be
non-empty; each item requires `productId`, `size`, `colour`, and `quantity` (`>= 1`).

**Business logic, in order:**
1. Look up every `productId` in `items`; `404` if any doesn't exist.
2. **Stock check across all items before creating or decrementing anything.** If any
   item's requested quantity exceeds that product's `stockQty`, the whole order is
   rejected with `409 Conflict` listing every insufficient item, not a partial
   order with only the in-stock items included.
3. If `athleteCode` is present, look it up. An invalid or inactive code is **silently
   ignored**, checkout still proceeds at full price, it does not fail the request.
4. Resolve or create the `Customer` row (see above).
5. Compute `subtotalAmount` = sum of `(product.price × quantity)` across every item.
   If a valid athlete code was found, `discountAmount = subtotalAmount ×
   commissionRate / 100` (rounded to 2dp, half-up), applied to the order total, not
   per line.
6. Save the `Order` (status `PENDING`) with all its `OrderItem` rows, each
   snapshotting that product's price at order time (`unitPrice`) so a later price
   change never rewrites what a past customer actually paid.
7. **Decrement `stockQty` per item**, by that item's own quantity. Stock is reserved
   at order-creation time (checkout submission), not at payment confirmation, so two
   customers can't both "buy" the last unit while one is still on the PayHere payment
   page. Known tradeoff: this is a read-then-write, not a DB row lock, so it isn't
   fully race-proof under truly simultaneous requests.
8. If a valid athlete code was used, create a linked `Commission` row (one per order,
   not per item), status `AWAITING_PAYMENT`.
9. If the request came from a logged-in customer, clear their persisted cart, the
   items they just bought shouldn't still show as "in cart" afterward.
10. Call `PaymentService.initiatePayment(...)` to get a payment URL.

**Response:** `200`:
```json
{ "orderId": 100, "totalAmount": 15300.00, "paymentUrl": "http://.../api/orders/100/checkout-redirect" }
```
`404` if any product not found. `409` if any item is out of stock. `400` on
validation failure (including an empty `items` array).

### `GET /api/orders/{id}/checkout-redirect`
**Access:** Public.
PayHere's hosted checkout requires an HTML form POST signed with a merchant-secret
hash, not a plain GET link, and that secret must never reach the browser. So
`paymentUrl` from `POST /api/orders` actually points here: this endpoint returns a
tiny auto-submitting HTML page (`Content-Type: text/html`) containing the signed
form, which immediately POSTs itself to PayHere's real checkout URL. The frontend's
`window.location.href = order.paymentUrl` works unmodified, it just lands here for a
fraction of a second first. PayHere's `items` field on that form is a plain display
string built by joining every order line as `"Product Name xQuantity"` (e.g.
`"Protocol 001 Tee x2, Protocol 002 Hoodie x1"`).

### `POST /api/orders/{id}/confirm-payment`
**Access:** Public, but only meaningfully callable by PayHere itself. This is
PayHere's server-to-server `notify_url` webhook, called directly by PayHere's
servers, not by the frontend. The `notify_url` sent to PayHere is unique per order
(`.../api/orders/{id}/confirm-payment`).

`return_url` and `cancel_url` are also built per-order, both pointing at one shared
frontend route: `{frontend}/order/{id}/confirmation`. PayHere redirects the browser
here after either a successful payment (`return_url`, with `?status=success`) or a
cancelled/failed one (`cancel_url`, with `?status=cancelled`). One route handles both
outcomes, distinguished only by that query param.

**This `status` query param is a UX hint only, never proof of payment.** A customer
can land on this URL without having actually paid (manually typing it, a slow or
dropped webhook, etc.), so the frontend confirmation page must always call `GET
/api/orders/{id}` and read the authoritative `paymentStatus` field from there. That
field only ever flips to `PAID` inside this endpoint, after the md5 signature below
has been verified, never from the browser redirect alone.

**Request:** PayHere posts `application/x-www-form-urlencoded` with `merchant_id`,
`order_id`, `payhere_amount`, `payhere_currency`, `status_code`, `md5sig`.

**Business logic:**
1. Recompute the expected md5 signature server-side from the merchant secret and
   compare against `md5sig`. **This check is not optional**, without it, anyone
   could POST a fake "payment succeeded" call.
2. Only if the signature is valid **and** `status_code == "2"` (PayHere's
   "successfully completed" code): mark the order `PAID`, and flip its linked
   commission (if any) from `AWAITING_PAYMENT` to `EARNED`.
3. Idempotent, calling this twice on an already-`PAID` order does nothing the
   second time (PayHere's webhook can fire more than once).

**Response:** `200` always (PayHere expects this regardless of whether the signature
check passed internally, a non-200 causes PayHere to retry the webhook).

### `GET /api/orders/{id}`
**Access:** Public (used by the order confirmation page).<br>
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

---

## Athlete Dashboard, Commissions & Admin Oversight

### `GET /api/athletes/validate-code/{code}`
**Access:** Public. Used both by the checkout flow for a live discount
preview and anywhere else a code needs validating.<br>
**Response:** `200` always (never 404, an unrecognized code is a valid "no
discount" answer, not an error, so a live checkout preview doesn't break on a typo):
```json
{ "valid": true, "code": "RISE10", "discountPercentage": 10.00 }
```
Inactive athlete codes also resolve to `"valid": false"`.

### `GET /api/athletes/dashboard`
**Access:** `ATHLETE`. The athlete ID is always taken from the JWT via
`CurrentUser`, **never** from a URL parameter or request body, this is deliberate
and tested explicitly, so one athlete cannot view another athlete's commission data
by guessing or editing an ID.

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
Each order summary carries its own `items` array (an order using an athlete's code
can itself contain multiple products, and the dashboard needs to show all of them,
not just one).
- `salesGenerated` is the sum of every commission-linked order's `subtotalAmount`
  (the full multi-item order value, before discount).
- `commissionEarned` = sum of `EARNED` + `PAID` commissions (every order where
  payment was actually confirmed).
- `paidCommission` = `PAID` only (already paid out by admin).
- `pendingCommission` = `EARNED` only (payment confirmed, payout not yet made).

Commission lifecycle: `AWAITING_PAYMENT → EARNED → PAID`. These are three distinct
statuses, kept separate on purpose, "payment not yet confirmed" and "earned but not
yet paid out" are different states and are never conflated under one ambiguous label.

### `GET /api/admin/orders`
**Access:** `ADMIN`.<br>
**Query params:** `status` (optional, `PENDING` / `PAID` / `CANCELLED`).<br>
**Response:** `200`, array of:
```json
{ "id": 100, "customerName": "...", "customerPhone": "...", "customerAddress": "...",
  "items": [
    { "productId": 1, "productName": "Protocol 001 Tee", "size": "M", "colour": "Black",
      "quantity": 2, "unitPrice": 4500.00, "lineTotal": 9000.00 }
  ],
  "subtotalAmount": 9000.00, "totalAmount": 9000.00, "discountAmount": 0.00,
  "paymentStatus": "PAID", "athleteCode": "RISE10", "orderDate": "..." }
```
This is a customer-enriched view distinct from the public `OrderDetailDto`, it
exposes delivery contact info, which only admins should see.

### `GET /api/admin/athletes`
**Access:** `ADMIN`.<br>
**Response:** `200`, array of `{ "id", "name", "email", "athleteCode", "commissionRate", "active" }`.

### `POST /api/admin/athletes`
**Access:** `ADMIN`.<br>
**Request:** `{ "name", "email", "password", "athleteCode", "commissionRate" }`.<br>
**Validation:** all required; `password` min 6 chars; `commissionRate` between 0–100.<br>
**Business logic:** rejects duplicate email (`409` via `EmailAlreadyInUseException`)
or duplicate athlete code (`400`). Password is BCrypt-hashed before storage. New
athlete is `active: true` by default.<br>
**Response:** `200`, the created `AthleteDto`.

### `PUT /api/admin/athletes/{id}`
**Access:** `ADMIN`.<br>
**Request:** `{ "commissionRate": nullable, "active": nullable }`, both fields
optional; omitting a field leaves it unchanged (this is a partial update, not a full
replace).<br>
**Response:** `200`, updated `AthleteDto`, or `404` if the athlete doesn't exist.
Deactivating an athlete (`active: false`) takes effect immediately, they can no
longer log in on their next attempt (`CustomUserDetailsService` checks `active`
status at login time).

### `GET /api/admin/commissions`
**Access:** `ADMIN`.<br>
**Query params:** `status` (optional, `AWAITING_PAYMENT` / `EARNED` / `PAID`).<br>
**Response:** `200`, array of:
```json
{ "id": 50, "athleteName": "...", "athleteCode": "RISE10", "orderId": 100,
  "commissionAmount": 450.00, "commissionStatus": "EARNED", "paidDate": null }
```

### `PUT /api/admin/commissions/{id}/mark-paid`
**Access:** `ADMIN`.<br>
**Business logic:** only a commission currently in `EARNED` status can be marked
paid, attempting this on `AWAITING_PAYMENT` (payment not even confirmed yet) or an
already-`PAID` commission returns `400` with an explanatory message, rather than
silently succeeding or corrupting state. On success, sets status to `PAID` and
stamps `paidDate` with the current time.<br>
**Response:** `200`, updated `CommissionDto`. `404` if the commission doesn't
exist. `400` if not in `EARNED` status.

*(Product create/update/delete/image management used by the admin product screen
live under `/api/products`; they're admin-only routes but
belong to the Product resource.)*