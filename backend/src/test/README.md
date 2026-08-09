# Eternal Protocol — Backend Test Suite

## What's in this zip

```
src/test/java/com/eternalprotocol/api/
├── security/
│   └── JwtUtilTest.java                 (unit — token generation/validation)
├── service/                             (unit tests, Mockito — no DB, no Spring context)
│   ├── ProductServiceTest.java
│   ├── AuthServiceTest.java
│   ├── CartServiceTest.java
│   ├── CustomerServiceTest.java
│   ├── OrderServiceTest.java            (discount calc, stock, payment confirmation)
│   ├── AthleteServiceTest.java          (dashboard math, admin athlete/commission mgmt)
│   └── PaymentServiceTest.java          (PayHere hash + signature verification)
└── integration/                         (full Spring context + MockMvc + real H2 DB)
    ├── AuthTestHelper.java              (shared helper, not a test itself)
    ├── AuthIntegrationTest.java
    ├── ProductIntegrationTest.java
    ├── OrderIntegrationTest.java
    ├── AthleteIntegrationTest.java
    ├── AdminIntegrationTest.java
    ├── CustomerIntegrationTest.java
    └── SecurityConfigTest.java          (401 vs 403 behaviour specifically)

src/main/java/com/eternalprotocol/api/config/
└── SecurityConfig.java                  (MODIFIED — see below, required for tests to pass)
```

**86 test methods across 16 files**, covering every service and every controller.

## ⚠️ One main-source file is included and required

`SecurityConfig.java` is not just a test file — it's a small but necessary fix
to your actual application code, included here because several tests depend
on it.

**The problem it fixes:** by default, Spring Security's fallback behaviour
for "no token provided" vs. "valid token, wrong role" is inconsistent and
version-dependent — in a lot of configurations it returns `403 Forbidden`
for *both* cases, when a REST API should return `401 Unauthorized` for a
missing/invalid token and reserve `403` for "you're logged in, but not
allowed here." I added an explicit `authenticationEntryPoint` (401) and
`accessDeniedHandler` (403) to `SecurityConfig` so this is guaranteed and
testable, instead of left to chance.

**Extracting this zip over your project will overwrite your existing
`SecurityConfig.java`.** If you've made your own changes to that file since
the version I last gave you, diff it first rather than blindly overwriting.

## How to install

1. Extract this zip directly into your `backend/` folder (the one containing
   `pom.xml` and `src/`) — it's already laid out with the matching folder
   structure, so files land in the right place automatically.
2. When prompted to overwrite `SecurityConfig.java`, say yes (see warning above).
3. Run the tests:
   ```
   cd backend
   mvn test
   ```

No new dependencies are needed — `spring-boot-starter-test` and
`spring-security-test` were already in your `pom.xml` from the start, and
they include JUnit 5, Mockito, and AssertJ.

## What each layer actually tests

**Unit tests (`service/`, `security/`)** — fast, no database, no Spring
context. Each service's dependencies (repositories, encoders, etc.) are
mocked with Mockito, so these test business logic in isolation: does the
discount get calculated correctly? Does an invalid athlete code fail
silently instead of blocking checkout? Does stock actually decrement?

**Integration tests (`integration/`)** — slower, but exercise the real
thing: an actual HTTP request goes through Spring MVC, your real
`SecurityConfig`, a real JWT, a real H2 database, and back out — the same
path a real frontend request takes. These specifically check the security
properties called out throughout your code comments, e.g.:
- one athlete can't see another athlete's dashboard data
- one customer can't delete another customer's cart item by guessing an ID
- a customer token can't hit an admin-only route, and vice versa
- a guest checkout never shows up in any logged-in customer's order history

## Running a single test class

```
mvn test -Dtest=OrderServiceTest
mvn test -Dtest=AthleteIntegrationTest
```

## A note on test isolation

The integration tests all use `@Transactional`, so anything a test writes
to the database (a new product, a new customer, an order) is automatically
rolled back after that test method finishes. This means tests can run in
any order without polluting each other, and you don't need to reset your
database between runs — including the one seeded by `DataSeeder`
(`admin@eternalprotocol.com` / `admin123`), which several tests log in with
directly.
