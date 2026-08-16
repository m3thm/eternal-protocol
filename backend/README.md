# Backend

Spring Boot REST API for the Eternal Protocol e-commerce site.

## Stack

- Java 17, Spring Boot 3.3.4
- Spring Web, Spring Data JPA, Spring Security
- JWT auth (`jjwt` 0.12.6), one login endpoint shared across Customer/Athlete/Admin roles
- H2 in-memory DB by default (zero setup), MySQL profile available for persistent data
- Cloudinary, product image hosting
- PayHere, hosted checkout (sandbox)
- Lombok, `springboot3-dotenv` (loads `.env` automatically)

## Project layout

```
src/main/java/com/eternalprotocol/api/
├── entity/       # JPA entities: Product, Customer, Athlete, Admin, Order, Commission, CartItem...
├── repository/   # Spring Data JPA repositories
├── dto/          # Request/response DTOs, never expose entities directly over the API
├── security/     # JWT filter, unified UserDetailsService, CurrentUser helper
├── service/      # Business logic, one service per module
├── controller/   # REST endpoints, one controller per module
├── config/       # SecurityConfig, CorsConfig, CloudinaryConfig, AdminBootstrap
└── exception/    # Custom exceptions + GlobalExceptionHandler

src/main/resources/
├── application.yml        # default config, H2, JWT, CORS, Cloudinary, PayHere, admin bootstrap
└── application-mysql.yml  # overrides datasource/JPA settings when the "mysql" profile is active

src/test/java/com/eternalprotocol/api/
├── security/     # unit test for JwtUtil
├── service/      # unit tests (Mockito, no DB/Spring context), one per service
└── integration/  # full Spring context + MockMvc + real H2 DB, one per controller/module
```

## Running the backend

### 1. Quick start: H2 in-memory DB (zero setup)

```bash
cd backend
cp .env.example .env    # fill in real values, see "Credentials" below
mvn spring-boot:run
```

The app runs on `http://localhost:8080`. Data lives in memory and resets every restart.

**First admin login:** before your *first* run, set `ADMIN_BOOTSTRAP_EMAIL` and
`ADMIN_BOOTSTRAP_PASSWORD` in `.env`. On startup, if no admin account exists yet, one
is created from those values. It's safe to leave them in `.env` afterwards, the
bootstrap only ever fires while the `admins` table is empty. No sample products are
seeded; add real ones from the admin dashboard once you're logged in.

**H2 console** (inspect the in-memory DB in a browser, default profile only):
`http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:mem:eternalprotocol`, user
`sa`, blank password.

### 2. Persistent method: MySQL + Workbench

H2 wipes on every restart, which is fine for quick iteration but not for a demo you
want to survive a reboot. To switch to a real, persistent MySQL database:

**a) Install MySQL** (skip if already installed):
- **Windows/Mac:** download MySQL Community Server from
  https://dev.mysql.com/downloads/mysql/ and run the installer. Set a root password
  you'll remember, you need it in step (c).
- **Mac (Homebrew):** `brew install mysql && brew services start mysql`
- **Linux:** `sudo apt install mysql-server && sudo systemctl start mysql`

**b) Install MySQL Workbench**, a GUI for browsing/querying your database (the
persistent-DB equivalent of the H2 console above). Bundled with the Windows/Mac
installer, or download separately: https://dev.mysql.com/downloads/workbench/.
Once installed, add a new connection to `localhost:3306` with the root user/password
from step (a), this lets you browse tables, run ad-hoc SQL, and inspect data visually
instead of only through the app or `mysql` CLI.

**c) Set your MySQL password**, put it in `.env` as `MYSQL_PASSWORD`. You don't need
to create the `eternal_protocol` database yourself: `createDatabaseIfNotExist=true`
in the connection URL (`application-mysql.yml`) creates it automatically on first run.

**d) Run with the `mysql` profile active:**

- **Terminal:**
  ```bash
  cd backend
  mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
  ```
- **IntelliJ:** Run → Edit Configurations → your `EternalProtocolApplication` config
  → "Active profiles" field → type `mysql` → Apply → run normally.

The same one-time admin bootstrap applies (see above), except now the data survives
restarts. Confirm it worked by opening MySQL Workbench, connecting to `localhost:3306`,
and checking the `eternal_protocol` schema for tables like `products`, `customers`,
`orders`, `admins`, `commissions`.

To go back to the zero-setup H2 mode, just run without `-Dspring-boot.run.profiles=mysql`.

**Resetting demo data:** `db-manager.sql` in this folder truncates products, product
images, cart items, orders, order items, and commissions (handy before a demo/viva to
start from a clean catalog). Run it against your MySQL database, e.g. in Workbench,
open the file and execute it against the `eternal_protocol` schema, or via CLI:
`mysql -u root -p eternal_protocol < db-manager.sql`. It leaves customers, athletes,
and admins intact.

## Credentials needed and where to get them

All of these are read from a `.env` file in this folder (see `.env.example`), never
commit real credentials or hardcode them in `application.yml`.

| Variable | What it's for | Where to get it |
|---|---|---|
| `JWT_SECRET` | Signs/verifies login tokens | Any random 256-bit+ string, e.g. `openssl rand -base64 32`. Required, the app won't start without it. |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Hosts product images uploaded from the admin dashboard | Free sign-up (no card needed) at https://cloudinary.com/users/register/free, all three values are on your Dashboard homepage after signing up. Product image upload fails until these are set. |
| `PAYHERE_MERCHANT_ID`, `PAYHERE_MERCHANT_SECRET` | Sandbox checkout, generates the hosted payment page and verifies payment notifications | Free sandbox account at https://www.payhere.lk/support/sandbox. Checkout fails until these are set, `checkout-url` in `application.yml` points at PayHere's sandbox endpoint (`sandbox.payhere.lk`), so no real money is ever involved during development. |
| `ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_PASSWORD` | Creates your first admin account | Pick any values, see "First admin login" above. |
| `MYSQL_PASSWORD` | Only used when the `mysql` profile is active | Your MySQL root password, see MySQL setup above. |

Two more things worth knowing that aren't `.env` values:
- **CORS**, `app.cors.allowed-origins` in `application.yml` defaults to
  `http://localhost:5173,http://localhost:3000`. Add your deployed frontend URL here
  once you have one, or the browser will block requests to the API.
- **PayHere is sandbox-only as configured**, before any real launch you'd swap
  `checkout-url` to PayHere's live endpoint and use production merchant credentials.

## Running the tests

**86 test methods across 16 files**, covering every service and every controller.

```bash
cd backend
mvn test                          # run everything
mvn test -Dtest=OrderServiceTest  # run a single test class
mvn test -Dtest=AthleteIntegrationTest
```

No extra setup needed, `spring-boot-starter-test` and `spring-security-test` are
already in `pom.xml` and pull in JUnit 5, Mockito, and AssertJ.

**Two layers of tests:**

- **Unit tests** (`src/test/java/.../service/`, `.../security/`), fast, no database,
  no Spring context. Each service's dependencies (repositories, encoders, etc.) are
  mocked with Mockito, so these test business logic in isolation: is the commission
  amount calculated correctly? Does an invalid athlete code fail gracefully instead of
  blocking checkout? Does stock actually decrement on order creation?
- **Integration tests** (`src/test/java/.../integration/`), slower, but exercise the
  real thing: an actual HTTP request goes through Spring MVC, the real
  `SecurityConfig`, a real JWT, and a real H2 database. These specifically verify
  security properties like: one athlete can't see another athlete's dashboard data,
  one customer can't delete another customer's cart item by guessing an ID, a
  customer token can't hit an admin-only route (and vice versa), and a guest
  checkout never appears in any logged-in customer's order history.

**Test isolation:** integration tests are `@Transactional`, so anything a test writes
to the database is rolled back automatically after that test method finishes, tests
can run in any order without polluting each other or needing a DB reset between runs.
Tests that need an admin login create their own admin directly via `AdminRepository`
(see `AuthTestHelper.createAdminAndGetToken`) rather than relying on a seeded account.

`SecurityConfig.java` includes an explicit `authenticationEntryPoint` (401) and
`accessDeniedHandler` (403), so a missing/invalid token consistently returns 401 and
"logged in but wrong role" consistently returns 403, several integration tests
depend on this distinction (`SecurityConfigTest.java` tests it directly).

## API reference

Full endpoint-by-endpoint contract, request/response shapes, validation rules, and
business logic notes, lives in [`BACKEND_API_SPEC.md`](./BACKEND_API_SPEC.md).
Quick summary of the surface area:

- `POST /api/auth/login`, `POST /api/auth/register`
- `GET/POST/PUT/DELETE /api/products[/{id}]`, `GET /api/products/subcategories`
- `POST /api/products/upload-image`, product image gallery endpoints under `/api/products/style/{styleCode}/images`
- `POST /api/orders`, `GET /api/orders/{id}`, `GET /api/orders/{id}/checkout-redirect`, `POST /api/orders/{id}/confirm-payment`
- `GET/PUT /api/customers/me`, `GET /api/customers/me/orders`, cart endpoints under `/api/customers/me/cart`
- `GET /api/athletes/validate-code/{code}`, `GET /api/athletes/dashboard`
- `GET/POST/PUT /api/admin/athletes[/{id}]`, `GET /api/admin/orders`, `GET /api/admin/commissions`, `PUT /api/admin/commissions/{id}/mark-paid`

## Known limitations

- **No refresh-token flow**, JWTs just expire after 24h and the user logs in again.
  Which is fine for this project but a real production app would add refresh tokens.
- **PayHere sandbox only**, see "Credentials" above.
- **`ddl-auto: update`** auto-manages the schema from your entities, which is
  convenient for just a demo but isn't how we'd manage migrations in production
  (we'd use Flyway/Liquibase instead)
