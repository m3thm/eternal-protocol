# Eternal Protocol — Backend

Spring Boot REST API for the Eternal Protocol e-commerce site (SE2012 group project).

## Stack
- Java 17, Spring Boot 3.3.4
- Spring Web, Spring Data JPA, Spring Security
- JWT auth (jjwt 0.12.6) — one login endpoint shared across Customer/Athlete/Admin
- H2 in-memory DB by default (zero setup) — MySQL config included in `application.yml`, commented out
- PayHere hosted checkout integration (sandbox)

## Quick start

```bash
cd backend
cp .env.example .env    # fill in your own values — see "Before it fully works" below
mvn spring-boot:run
```

App runs on `http://localhost:8080`.

**First admin login:** set `ADMIN_BOOTSTRAP_EMAIL` and `ADMIN_BOOTSTRAP_PASSWORD`
in `.env` before the first run. On startup, if no admin account exists yet,
one is created from those two values — after that it's safe to leave them
in `.env` (or remove them); they're only used while the `admins` table is
empty. No sample product is seeded any more — add real products from the
admin dashboard once you're logged in.

H2 console (to inspect data, default/dev profile only): `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:eternalprotocol`, user `sa`, blank password)

## Before it fully works

All of the below are read from a `.env` file in this folder (see
`.env.example`) — never committed, never hardcoded in `application.yml`.

1. **PayHere sandbox credentials** — sign up free at https://www.payhere.lk/support/sandbox,
   then set `PAYHERE_MERCHANT_ID` and `PAYHERE_MERCHANT_SECRET` in `.env`.
   Checkout will fail until you do.
2. **Cloudinary credentials** — sign up free (no credit card) at
   https://cloudinary.com/users/register/free, then set
   `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, and `CLOUDINARY_API_SECRET`
   in `.env` — find all three on your Cloudinary Dashboard homepage after
   signing up. Product image upload will fail until you do.
3. **JWT secret** — set `JWT_SECRET` in `.env` to a real random value
   (`openssl rand -base64 32`) before any real deployment.
4. **Admin bootstrap** — set `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD`
   in `.env` (see Quick start above).
5. **CORS origins** — `app.cors.allowed-origins` in `application.yml` defaults to
   `http://localhost:5173` (Vite's default port). Add your deployed frontend
   URL here once you have one.
6. **Switch to MySQL** (optional, for data that survives restarts) — see below.

## Switching to a persistent MySQL database

By default this runs on H2 (in-memory, wiped every restart) so you can try
it out with zero setup. To make data persist, switch to MySQL:

**1. Install MySQL** (skip if already installed):
- **Windows/Mac**: download MySQL Community Server from
  https://dev.mysql.com/downloads/mysql/ and run the installer. When it
  asks, set a root password you'll remember — you'll need it in step 3.
- **Mac (Homebrew)**: `brew install mysql && brew services start mysql`
- **Linux**: `sudo apt install mysql-server && sudo systemctl start mysql`

**2. (Optional) Install MySQL Workbench** — a GUI for browsing your database,
similar to what the H2 console gives you. Included with the Windows/Mac
installer above, or download separately at
https://dev.mysql.com/downloads/workbench/.

**3. Set your MySQL password** — set `MYSQL_PASSWORD` in `.env` to the root
password you set in step 1. (You don't need to create the
`eternal_protocol` database yourself — `createDatabaseIfNotExist=true` in
the connection URL in `application-mysql.yml` does that automatically on
first run.)

**4. Run with the MySQL profile active:**

- **Terminal:**
  ```
  cd backend
  mvn spring-boot:run -Dspring-boot.run.profiles=mysql
  ```
- **IntelliJ:** Run → Edit Configurations → select your
  `EternalProtocolApplication` run config → in the "Active profiles" field,
  type `mysql` → Apply → run as normal (green ▶ button).

If `ADMIN_BOOTSTRAP_EMAIL`/`ADMIN_BOOTSTRAP_PASSWORD` are set in `.env`,
you'll get the same one-time admin creation as on H2, but now the data
lives in actual MySQL and survives restarts. Confirm in MySQL Workbench (or
`mysql` CLI) by connecting to `localhost:3306` and checking the
`eternal_protocol` database — you should see `products`, `customers`,
`orders`, `admins`, etc.

To go back to the quick H2 setup, just run without `-Dspring-boot.run.profiles=mysql`.


## Project layout

```
src/main/java/com/eternalprotocol/api/
├── entity/       # JPA entities: Product, Customer, Athlete, Admin, Order, Commission, CartItem
├── repository/   # Spring Data JPA repositories
├── dto/          # Request/response DTOs (records) — never expose entities directly
├── security/     # JWT filter, unified UserDetailsService, CurrentUser helper
├── service/      # Business logic, one per module
├── controller/   # REST endpoints, one per module
├── config/       # SecurityConfig, CorsConfig, AdminBootstrap
└── exception/    # Custom exceptions + GlobalExceptionHandler
```


## Module → code mapping (for splitting work across the group)

| Module | Owns |
|---|---|
| 1 — Storefront/Catalog | `Product` entity, `ProductRepository`, `ProductService`, `ProductController` |
| 2 — Cart/Checkout + Accounts | `Order`, `CartItem`, `Customer` entities; `OrderService`, `CartService`, `CustomerService`, `PaymentService`; `OrderController`, `CustomerController` |
| 3 — Athlete System | `Athlete`, `Commission` entities (dashboard half); `AthleteService.getDashboard/validateCode`; `AthleteController` |
| 4 — Admin Dashboard | `AthleteService` admin methods, `OrderService.getAllOrdersForAdmin`; `AdminController` |

Cross-cutting (shared, not owned by one person): `AuthController`/`AuthService`,
`SecurityConfig`, `CorsConfig`, `GlobalExceptionHandler`, `JwtUtil`/`JwtAuthFilter`.

## API endpoints implemented

All endpoints match `BACKEND_API_SPEC.md` (the full, current spec — read
that file for complete request/response shapes, validation rules, and
business logic notes). Quick reference:
- `POST /api/auth/login`, `POST /api/auth/register`
- `GET/POST/PUT/DELETE /api/products[/{id}]`
- `POST /api/products/upload-image` — product image upload (Cloudinary)
- `POST /api/orders`, `GET /api/orders/{id}`, `GET /api/orders/{id}/checkout-redirect`, `POST /api/orders/{id}/confirm-payment`
- `GET /api/customers/me[/orders|/cart]`, `POST /api/customers/me/cart`, `DELETE /api/customers/me/cart/{id}`
- `GET /api/athletes/validate-code/{code}`, `GET /api/athletes/dashboard`
- `GET/POST/PUT /api/admin/athletes[/{id}]`, `GET /api/admin/orders`, `GET /api/admin/commissions`, `PUT /api/admin/commissions/{id}/mark-paid`

## Known limitations / things worth flagging in your report

- **JWT in this stateless setup carries no refresh-token flow** — tokens
  just expire after 24h and the user has to log in again. Fine for a
  student project; a production app would add refresh tokens.
- **PayHere sandbox only** — swap `checkout-url` to the live PayHere URL and
  use production merchant credentials before any real launch.
- **`ddl-auto: update`** auto-manages your schema, which is convenient for
  coursework but not how you'd manage migrations in production (you'd use
  Flyway/Liquibase instead) — worth a one-line mention under "best practices"
  in the viva if asked about it.
