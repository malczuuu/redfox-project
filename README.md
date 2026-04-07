# RedFox Project

> **Proof of Concept** - demonstrates OAuth2 PKCE authentication across a three-tier Spring Boot + Angular architecture.

## Applications

| Application           | Port | Description                                                                                                                                                                                                                                                                                     |
|-----------------------|------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **redfox-webapp**     | 8482 | Angular SPA. Entry point for the user. Initiates the OAuth2 PKCE flow, handles the authorization callback, and calls the resource API. Manages Projects, Things, and Users.                                                                                                                     |
| **redfox-app**        | 8481 | Spring Boot resource server. Exposes the REST API and acts as a backend-for-frontend (BFF) for token operations - it holds the client secret and proxies token exchange and refresh requests to the authserver, keeping credentials off the browser. Validates JWTs on every protected request. |
| **redfox-authserver** | 8483 | Spring Authorization Server (OAuth2/OIDC). Authenticates users via a login form, issues short-lived access tokens (2 min) and long-lived refresh tokens (30 days). Sessions and authorizations are persisted in PostgreSQL.                                                                     |

## Authentication Flow

The webapp uses the OAuth2 Authorization Code flow with PKCE. The browser never sees the `client_secret` - all token 
requests are proxied through `redfox-app`.

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant webapp as redfox-webapp<br/>(:8482)
    participant app as redfox-app<br/>(:8481)
    participant auth as redfox-authserver<br/>(:8483)

    User->>webapp: Click "Login"
    webapp->>Browser: Generate code_verifier + code_challenge (PKCE/S256)<br/>Store code_verifier in sessionStorage
    Browser->>auth: GET /oauth2/authorize?client_id=webapp-client<br/>&response_type=code&scope=openid profile<br/>&redirect_uri=…/oauth2/callback<br/>&code_challenge=…&code_challenge_method=S256
    auth->>Browser: Redirect to /login (form)
    User->>auth: Submit credentials
    auth->>Browser: Redirect to /oauth2/callback?code=<auth_code>
    Browser->>webapp: Load /oauth2/callback?code=<auth_code>
    webapp->>app: POST /api/oauth2/token<br/>{ code, redirectUri, codeVerifier }
    app->>auth: POST /oauth2/token<br/>grant_type=authorization_code<br/>code=… code_verifier=…<br/>client_id=webapp-client client_secret=***
    auth-->>app: { access_token (JWT), refresh_token, expires_in }
    app-->>Browser: Set HttpOnly cookies:<br/>redfox_access_token (max-age = expires_in)<br/>redfox_refresh_token (max-age = 8h)<br/>204 No Content
    webapp->>Browser: Navigate to /projects
```

## Token Refresh Flow

Access tokens expire after 2 minutes. The Angular `authInterceptor` transparently refreshes them on a 401 response and
retries the failed request. The refresh token is rotated on every use (`reuse-refresh-tokens: false`).

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant webapp as redfox-webapp<br/>(:8482)
    participant app as redfox-app<br/>(:8481)
    participant auth as redfox-authserver<br/>(:8483)

    User->>webapp: Navigate to /projects
    webapp->>app: GET /api/v1/projects<br/>(cookie: redfox_access_token - expired)
    app-->>webapp: 401 Unauthorized
    webapp->>app: POST /api/oauth2/refresh<br/>(cookie: redfox_refresh_token)
    app->>auth: POST /oauth2/token<br/>grant_type=refresh_token<br/>refresh_token=… client_id=… client_secret=***
    auth-->>app: { access_token (new JWT), refresh_token (new), expires_in }
    app-->>Browser: Set updated HttpOnly cookies<br/>204 No Content

    alt Refresh succeeded
        webapp->>app: GET /api/v1/projects (retry)<br/>(cookie: redfox_access_token - new)
        app-->>webapp: 200 OK
        webapp-->>User: Projects list
    else Refresh failed
        webapp->>Browser: Clear session, redirect to /
    end
```

## Basic Auth

HTTP Basic authentication is enabled in `redfox-app` for testing convenience - it allows direct API calls without 
running through the full OAuth2 flow. Configured via `redfox.auth.basic.enabled=true|false`.
