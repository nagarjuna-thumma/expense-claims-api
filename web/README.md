# Expense claims web

Angular front end for the API in the parent folder. Employees submit claims and follow their status, managers approve or reject them.

- **Login with Keycloak** using the authorization code flow with PKCE (`angular-oauth2-oidc`). The SPA is a public client with no secret. Tokens refresh before they expire.
- **The token only goes to the API.** `authInterceptor` adds the bearer header to `/api/...` calls and nothing else.
- **Roles come from the token** (`realm_access.roles`) and only decide what the UI shows. The API checks them again on every call. Managers see an Approvals page, and a manager who tries to approve their own claim gets the API's 403 explained in plain words.
- **Types come from the OpenAPI spec.** `src/app/api/schema.ts` is generated from `../docs/openapi.json` with `npm run api:types`. CI regenerates it and fails if it differs, so a backend change that breaks the contract can't slip through.
- Standalone components, signals, the new control flow syntax and typed reactive forms. Tests run on Vitest with `HttpTestingController`.

## Running it

Start Postgres, Keycloak and the API first (see the main README), then:

```bash
npm install
npm start          # http://localhost:4200, proxies /api to the API on 8080
npm test
```

Sign in as `alice` / `alice` to submit claims, and as `bob` / `bob` to approve them.
