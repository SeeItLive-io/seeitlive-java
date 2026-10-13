# seeitlive-sdk (Java)

Official SeeItLive SDK for Java — an idiomatic port of the reference Node.js
client (`@seeitlive/sdk`). Requires Java 17+.

## Install

Maven coordinate:

```xml
<dependency>
  <groupId>io.seeitlive</groupId>
  <artifactId>seeitlive-sdk</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Quick start

```java
import io.seeitlive.*;

SeeItLive client = new SeeItLive(SeeItLiveConfig.builder()
    .apiKey(System.getenv("SEEITLIVE_API_KEY"))
    .build());

// SMS channel requires recipientPhone + smsConsent (the recipient agreed
// to receive a text; carrier fees may apply)
Session session = client.sessions().create(CreateSessionParams.builder(Channel.SMS)
    .recipientPhone("+15551234567")
    .smsConsent(true)
    .build());
System.out.println(session.inviteUrl() + " " + session.inviteCode());

// email channel requires recipientEmail
client.sessions().create(CreateSessionParams.builder(Channel.EMAIL)
    .recipientEmail("customer@example.com")
    .build());

// link channel needs neither -- just returns a shareable link
Session linkSession = client.sessions().create(CreateSessionParams.builder(Channel.LINK).build());

SessionStatus status = client.sessions().status(session.id());
System.out.println(status.status() + " " + status.producerCount() + " " + status.hasVideo());
```

The client infers `env` (`Env.LIVE` or `Env.TEST`) from the API key's prefix
(`sil_test_...` -> `TEST`, otherwise `LIVE`). Override it with
`SeeItLiveConfig.Builder#env(Env)`, and override the API base URL with
`baseUrl(String)` (default: `https://api.seeitlive.io/v1`).

Provide **exactly one** of `apiKey` or `oauthToken` on the config — passing
both, or neither, throws `IllegalArgumentException`.

### Reading a session

`sessions().get` returns a stored session, including how its invitation was
delivered — useful when re-opening a session an integration created earlier:

```java
SessionDetail s = client.sessions().get(session.id());
System.out.println(s.status() + " " + s.invitation().channel() + " " + s.invitation().recipient());
```

### Guest protection

A session can require the customer to enter a secret before their camera is
shared — a static password, or a rotating code. Read the current one to them:

```java
ProtectionInfo p = client.sessions().getProtection(session.id());
if ("password".equals(p.type())) System.out.println(p.password());
if ("totp".equals(p.type())) System.out.println(p.code() + " " + p.secondsRemaining() + "s left");
```

Change it after creation — including adding one to a session that had none.
Omit the secret and one is generated for you:

```java
ProtectionInfo added = client.sessions().setProtection(session.id(), new SetProtectionParams("password"));
System.out.println(added.password()); // read this to the customer

// Clearing also clears any lockout the customer triggered.
client.sessions().setProtection(session.id(), new SetProtectionParams("none"));
```

Five wrong answers lock the guest out. Clear the lockout — rotation is opt-in,
since the usual cause is a mistyped secret:

```java
client.sessions().resetProtection(session.id(), new ResetProtectionParams());
ProtectionInfo fresh = client.sessions().resetProtection(session.id(), new ResetProtectionParams(true));
```

For `totp`, `getProtection` returns the **current code**, never the seed — the
seed leaves the server only once, in the create response.

### Embedded viewer tokens

`sessions().createAgentToken` mints a fresh agent-view grant so your own UI can
embed the live viewer. Tokens expire after 120 minutes — mint another to
re-open a viewer:

```java
AgentToken viewer = client.sessions().createAgentToken(
        session.id(), new CreateAgentTokenParams("Odoo — crm.lead 42"));
System.out.println(viewer.agentViewUrl());
```

### Snapshots

Frames the agent captured during a session. Both calls require a plan with
snapshot history:

```java
List<SnapshotSummary> snapshots = client.snapshots().list(session.id()); // newest first
SnapshotImage image = client.snapshots().get(snapshots.get(0).id());

// data() is base64 PNG — the endpoint's raw-bytes mode is not used by the SDK.
Files.write(Path.of("frame.png"), Base64.getDecoder().decode(image.data()));
```

## OAuth usage

For OAuth-based (multi-tenant / marketplace) integrations, exchange an
authorization code for an access token using the standalone
`SeeItLive.exchangeCode(...)` static method (this call is unauthenticated —
it uses your client credentials in the request body, not a bearer token):

```java
TokenResponse token = SeeItLive.exchangeCode(ExchangeCodeParams.builder()
    .code(authorizationCodeFromRedirect)
    .clientId(System.getenv("SEEITLIVE_CLIENT_ID"))
    .clientSecret(System.getenv("SEEITLIVE_CLIENT_SECRET"))
    .redirectUri("https://your-app.example.com/oauth/callback")
    .build());

SeeItLive client = new SeeItLive(SeeItLiveConfig.builder()
    .oauthToken(token.accessToken())
    .build());

TokenInfo info = client.oauth().verify();
System.out.println(info.clientId() + " " + info.teamId() + " " + info.scopes() + " " + info.plan());
```

## Request signing

Set `signingSecret(...)` on the config builder to have every outgoing request
HMAC-signed automatically. When set (a non-empty string), each request carries:

- `X-SeeItLive-Signature: sha256=<hex>` —
  `HMAC_SHA256(signingSecret, "<unix_ts>.<METHOD>.<path>.<sha256hex(raw_body)>")`,
  where `path` is the request path only (e.g. `/sessions`, no base URL and no
  `/api/v1` prefix) and `raw_body` is the exact bytes sent (hashed as the empty
  string when there is no body).
- `X-SeeItLive-Timestamp: <unix_ts>` — unix seconds.

`path` is signed **including any query string** the SDK sends (e.g.
`/snapshots/<id>?format=base64`) — the server verifies path *and* query,
so the two must match exactly.

```java
import io.seeitlive.*;

SeeItLive client = new SeeItLive(SeeItLiveConfig.builder()
    .apiKey(System.getenv("SEEITLIVE_API_KEY"))
    .signingSecret(System.getenv("SEEITLIVE_SIGNING_SECRET"))
    .build());
```

A present-but-empty `signingSecret` is treated the same as absent — requests
are unsigned by default, so enabling signing is fully backwards-compatible.
`SeeItLive.exchangeCode(...)` is never signed.

## Errors

All SDK errors extend `SeeItLiveException` (`getStatusCode()`, `getMessage()`,
`getCode()`):

| Class | HTTP status |
| --- | --- |
| `ValidationException` (also has `getField()`) | 400 |
| `AuthenticationException` | 401 |
| `PermissionException` | 403 |
| `NotFoundException` | 404 |
| `RateLimitException` (also has `getRetryAfter()`, seconds — nullable `Integer`, and a `0` is a real "retry now", not "absent") | 429 |
| `SeeItLiveException` | any other non-2xx status |

```java
try {
    client.sessions().status("missing");
} catch (RateLimitException e) {
    System.out.println("retry after " + e.getRetryAfter() + " seconds");
} catch (SeeItLiveException e) {
    System.err.println(e.getStatusCode() + " " + e.getMessage() + " " + e.getCode());
}
```

## Security

- **Never log API keys, OAuth access tokens, or refresh tokens.**
- **Never ship `clientSecret` to the browser / a mobile app / any
  untrusted client.** `SeeItLive.exchangeCode(...)` must only be called
  from a trusted server-side context — it is a confidential-client
  operation. Client-side code should redirect the user through your own
  backend, which performs the code exchange and hands the browser a
  short-lived session, never the raw OAuth token or client secret.

## Development

```bash
cd packages/client-java
mvn test
```

Tests run entirely offline against a local `com.sun.net.httpserver.HttpServer`
stub — no live network calls.
