# artRINX Backend API Reference

Reference for the Android client. Covers every endpoint the iOS client uses, **excluding** the Apple-only StoreKit endpoints (`/subscriptions/verify-apple`, `/subscriptions/restore-apple`) — those are platform-specific and do not apply to Android.

---

## Table of Contents

1. [Conventions](#1-conventions)
2. [Standard Error Codes](#2-standard-error-codes-cross-cutting)
3. [Auth](#3-auth)
4. [Invite & Waitlist](#4-invite--waitlist)
5. [Profile](#5-profile)
6. [Artworks](#6-artworks)
7. [Curations](#7-curations)
8. [Events](#8-events)
9. [Messages / Chat](#9-messages--chat)
10. [Notifications](#10-notifications)
11. [Search](#11-search)
12. [Locations](#12-locations)
13. [Upload](#13-upload)
14. [Analytics](#14-analytics)
15. [WebSocket (real-time)](#15-websocket-real-time)
16. [Push Notifications (FCM)](#16-push-notifications-fcm)

---

## 1. Conventions

### Base URL
- **Production:** `https://api.artrinx.com/api`
- **Stage/Dev:** `https://apifargate.rinx.com/api`

### Response envelope
Every endpoint returns:
```json
{
  "success": true | false,
  "code": 200,
  "message": "OK" | "<structured_error_code>" | "<human readable>",
  "data": <T | null>
}
```
- `success: false` always pairs with an HTTP status code ≥ 400 in normal cases. **However**, treat the wire as `success: false` → error, regardless of HTTP status — backend occasionally returns HTTP 200 with `success: false` for soft failures.
- On structured errors, `message` is a snake_case identifier (e.g. `invite_required`) you should switch on programmatically. Don't display these strings to the user — render the mapped user-facing copy from the tables below.

### Paginated envelope
Endpoints that paginate wrap data in:
```json
{
  "items": [ ... ],
  "total": 83,
  "page": 1,
  "size": 10
}
```
- 1-indexed page param.
- Default size if omitted is endpoint-specific (usually 10–20).
- `total` is the full result count; use `items.size < size || page * size >= total` to detect last page.

### Auth headers
- All authed endpoints require `Authorization: Bearer <access_token>`.
- Access token lifetime ≈ 15 min.
- Refresh token lifetime ≈ 30 days.
- On `401`, refresh once and retry; on second `401`, sign out + route to auth picker.

### Token-refresh coordination (single-flight)
- Multiple concurrent `401`s must coalesce through a single refresh call. Only one refresh per session at a time.
- After successful refresh, all in-flight retries use the new token.
- After sign-out due to refresh failure, every subsequent request from the same session must be cancelled — don't issue stale-token requests.

### JSON casing
- Wire format is **snake_case** everywhere (`user_id`, `auth_user_id`, `profile_picture_url`).
- Use `Moshi` with `@Json(name = "snake_case_key")` or the Jackson `PropertyNamingStrategies.SNAKE_CASE`.

### Idempotency
- All `GET`s are idempotent and auto-retried by the client on transient failures (timeout, 502/503/504) with exponential backoff + jitter.
- `POST` / `PUT` / `PATCH` / `DELETE` **never** auto-retry. Exception: messages-send uses `client_message_id` for application-level idempotency (see [Messages](#9-messages--chat)).

### Network preflight
- iOS short-circuits with `URLError(.notConnectedToInternet)` before issuing the HTTP request if reachability indicates offline. Android equivalent: check `ConnectivityManager` before the request and surface "No internet connection. Please check your network and try again." without hitting the network.

### URL scheme for deep links
- Custom: `rinxart://...` (e.g. `rinxart://chat/<userId>`)
- Universal links: `https://www.artrinx.com/...`

---

## 2. Standard Error Codes (cross-cutting)

These can appear on most authed endpoints. Map once and reuse everywhere.

### HTTP status → action

| HTTP | Meaning | User-facing copy | Action |
|---|---|---|---|
| `200` + `success: false` | Soft failure | See `message` mapping below | Toast |
| `400` | Bad request / typed business error | See structured-code table | Inline + toast |
| `401` | Token expired or invalid | "Session expired. Please sign in again." | Refresh once → retry → if still 401, sign out + route to auth picker |
| `403` | Forbidden | See structured-code table | Toast (and sometimes redirect) |
| `404` | Resource gone / not found | "This item is no longer available." | Pop the screen / remove from list silently |
| `409` | Conflict / structured state error | See structured-code table | Inline / sheet |
| `422` | Validation error (Pydantic) | "Please check the form." + map to field if shown | Field-level error |
| `429` | Rate limited | Honor `Retry-After`; "Too many attempts. Please wait a moment." | Disable button, show countdown |
| `5xx` | Server error | "The server is having trouble. Please try again." | Retry button |
| Network error | Offline / timeout | "No internet connection." / "Request timed out." | Retry button |
| Cancelled | User left screen | (silent) | Don't surface |

### Structured error codes (in `message` field)

| `message` value | Meaning | User-facing copy | UI behavior |
|---|---|---|---|
| `invite_required` / `invite_or_referral_required` | New user attempted signup/OAuth without an invite code | (none) | Show "Enter invite code" sheet, retry same call with `invite_code` |
| `invite_invalid` | Invite code is bad / used / expired | "That invite code isn't valid." | Inline error on invite field |
| `referral_invalid` | Referral code rejected (gallery web flow) | "That referral code isn't valid." | Inline error |
| `contact_in_use` | Email or phone already has an account | "That email or phone is already in use. Try signing in." | Inline + "Sign in" link |
| `contact_required` | Sent both email + phone (or neither) — backend rule | "Please enter exactly one email or phone number." | Field-level |
| `no_account_found` | Sign-in attempted on unknown email/phone | "No account found with that email or phone." | Inline |
| `invalid_contact` | Malformed email/phone | "That email or phone number doesn't look right." | Inline |
| `otp_invalid` | Wrong OTP code | "Invalid code. Please try again." | Inline + shake animation |
| `otp_expired` | Code timed out | "Code expired. Tap Resend for a new one." | Inline + show Resend button |
| `otp_locked` | Too many wrong attempts (returns `retry_after` seconds in `data`) | "Too many wrong attempts. Try again in N seconds." | Disable input until timer |
| `rate_limited` | Endpoint-level rate limit (server `data.retry_after` or HTTP `Retry-After`) | Server's `message` if present, else "Too many attempts. Please wait." | Disable + countdown |
| `session_expired` / `refresh_invalid` | Refresh failed | "Session expired. Please sign in again." | Force sign-out |
| `refresh_reused` | Token theft detected | "You've been signed out for security. Please sign in again." | Force sign-out |
| `oauth_token_invalid` | Apple/Google `id_token` failed JWKS verification or expired | "Please sign in again." | Restart OAuth from SDK |
| `identity_disabled` | Admin disabled the account (HTTP 403) | "Your account is disabled. Please contact support." | Force sign-out; don't allow retry |
| `not_allowed_on_this_surface` | Role can't sign in on mobile (e.g. agent, admin) | "This account can't sign in on the mobile app. Please use the web portal." | Force sign-out; don't allow retry |
| `account_banned` | Ban gate — server ships a custom message in `data.message` | Render `data.message` verbatim | Force sign-out + show modal with the message |
| `email_required_before_phone_change` | User has no email AND no social identity; phone change would orphan them | "Add an email or sign-in method first so you don't lose access to your account." | Show "Add email first" modal with "Add email" CTA |
| `gallery_on_web_only` | Gallery role can't sign up on mobile | "Gallery accounts are managed on artrinx.com." | Show modal directing to web |
| `phone_notifications_only` | Phone signin requires consenting to SMS notifications | "Phone signin requires SMS notifications." | Modal with consent toggle |

### Error envelope shape (backend variants)

iOS handles two shapes — Android should too:

**Shape A — typed envelope:**
```json
{
  "success": false,
  "code": 400,
  "message": "invite_required",
  "data": null
}
```

**Shape B — FastAPI / Pydantic detail envelope:**
```json
{
  "detail": {
    "code": "otp_invalid",
    "message": "Invalid OTP code"
  }
}
```

Or for validation errors:
```json
{
  "detail": [
    { "loc": ["body", "email"], "msg": "value is not a valid email address", "type": "value_error.email" }
  ]
}
```

Map both shapes to your error type before surfacing.

---

## 3. Auth

### `POST /auth/native/request-otp`

**Used in:** Email sign-in (`EmailOtpSignInView`), Phone sign-in (`LoginWithPhoneView`), Phone signup (`RegisterWithPhoneView`).

**Auth:** No

**Request:**
```json
{
  "email": "user@example.com",
  "phone": "+919999999999",
  "mode": "signin" | "signup",
  "invite_code": "ABC12345" | null,
  "referral_code": null
}
```
- Send **exactly one** of `email` or `phone`. Sending both → `422 contact_required`.
- `invite_code` only on signup (ignored on signin).
- `referral_code` is web-only; **always null on mobile** (agent referral path is intercepted at `/verify-invite`).

**Response:** Empty (`data: null`)

**Error codes:** `contact_in_use` (signup only), `no_account_found` (signin only), `invite_required` (signup only), `invalid_contact`, `rate_limited`.

---

### `POST /auth/native/verify-otp`

**Used in:** Email OTP entry (`EmailOtpVerificationView`, `SignupEmailOtpVerificationView`), Phone OTP entry (`OtpVerificationView`).

**Auth:** No

**Request:**
```json
{
  "email": "user@example.com",
  "phone": "+919999999999",
  "code": "123456",
  "mode": "signin" | "signup",
  "invite_code": "ABC12345" | null,
  "referral_code": null,
  "consents": {
    "accepted_terms": true,
    "sms_2fa_consent": true,
    "account_notification_sms": true,
    "marketing_sms_consent": true
  }
}
```
- Send `email` OR `phone` (the one matching the request-otp call).
- `consents` only on signup (ignored on signin).

**Response (200) — `AuthEnvelope`:**
```json
{
  "access_token": "<jwt>",
  "refresh_token": "<jwt>",
  "access_expires_in": 900,
  "refresh_expires_in": 2592000,
  "user": {
    "id": 127,                              // nullable for mid-signup users
    "auth_user_id": "uuid-string",
    "email": "user@example.com",            // nullable
    "phone": "+919999999999",               // nullable
    "email_verified": true,
    "phone_verified": false,
    "is_admin": false,
    "role": "artist",                       // nullable for mid-signup
    "profile_exists": true,
    "profile_completed": true,
    "consents": { "accepted_terms": true, "sms_2fa_consent": true, "account_notification_sms": true, "marketing_sms_consent": true }  // nullable
  }
}
```
- **MUST decode `id`, `role`, `consents` as nullable** — they're null for brand-new mid-signup users.
- Store `access_token` + `refresh_token` in EncryptedSharedPreferences / Keystore-backed storage.
- Compute absolute access expiry: `now + access_expires_in` seconds.
- Route by `user.profile_completed`: `true` → main app; `false` → profile setup wizard.

**Error codes:** `otp_invalid`, `otp_expired`, `otp_locked` (with `retry_after`), `rate_limited`.

---

### `POST /auth/native/resend-otp`

**Used in:** Same OTP screens as above.

**Auth:** No

**Request:**
```json
{ "email": "user@example.com" }   // or { "phone": "+919999999999" }
```

**Response:** Empty.

**Error codes:** `rate_limited`.

---

### `POST /auth/native/refresh`

**Used in:** Implicit — the network middleware on every 401 retry, and the proactive refresh scheduler (~60s before access-token expiry).

**Auth:** No (refresh token in body)

**Request:**
```json
{ "refresh_token": "<jwt>" }
```

**Response:** Fresh `AuthEnvelope` (both tokens rotated).

**Behavior:**
- Must be single-flight: coalesce concurrent refreshes through one network call; share the result with all waiters.
- On failure → sign-out + route to auth picker.

**Error codes:** `refresh_invalid` → sign-out. `refresh_reused` → sign-out + "signed out for security" toast (theft detection).

---

### `POST /auth/native/logout`

**Used in:** Settings → "Sign out", `AppState.signOut()`.

**Auth:** No (refresh token in body)

**Request:**
```json
{ "refresh_token": "<jwt>" }
```

**Response:** Empty. Best-effort — if the server call fails, still wipe local tokens.

---

### `POST /auth/native/sign-out-all`

**Used in:** Settings → "Sign out everywhere" (`SignOutAllDevicesConfirmationView`).

**Auth:** Yes

**Request:** Empty.

**Response:** Empty. Invalidates every refresh-token row server-side for the user. Then wipe local session like a normal logout.

---

### `POST /auth/native/oauth/apple`

**Used in:** Auth method picker (`AuthMethodPickerView`).

**Auth:** No

**Request:**
```json
{
  "id_token": "<apple identity_token JWT>",
  "invite_code": "ABC12345" | null
}
```

**Response (200):** `AuthEnvelope` (same shape as verify-otp).

**Error codes:**
- `invite_required` → cache `id_token` IN MEMORY (not disk), show invite-code sheet, retry same POST with `invite_code` populated.
- `oauth_token_invalid` → restart Apple sign-in (token ~5min lifetime — expired during invite prompt).
- `gallery_on_web_only` → modal directing to web.

---

### `POST /auth/native/oauth/google`

**Used in:** Same — `AuthMethodPickerView`.

**Auth:** No

**Request:**
```json
{
  "id_token": "<google id_token JWT>",
  "invite_code": "ABC12345" | null
}
```

**Response (200):** `AuthEnvelope`.

**Behavior:** Backend looks up the Google `sub` claim. Known identity → login. Unknown identity → signup (requires `invite_code`). Same `invite_required` retry loop as Apple. Google `id_token` lifetime ~1 hr.

**Error codes:** Same as `/oauth/apple`.

---

### `POST /auth/native/contact/start-add`

**Used in:** Add Email (`AddEmailView`), Add Phone (`AddPhoneView`), Phone permissions (`PhonePermissionsView`).

**Auth:** Yes

**Request:**
```json
{
  "kind": "email" | "phone",
  "value": "newcontact@example.com"   // or "+919999999999"
}
```

**Response:** Empty. Server sends an OTP to `value`.

**Error codes:** `contact_in_use`, `invalid_contact`, `rate_limited`.

---

### `POST /auth/native/contact/confirm-add`

**Used in:** `EmailConfirmationView` (after `start-add`), Phone OTP entry.

**Auth:** Yes

**Request:**
```json
{
  "kind": "email" | "phone",
  "value": "newcontact@example.com",
  "code": "123456"
}
```

**Response:** Fresh `AuthEnvelope` (tokens may rotate).

**Error codes:** `otp_invalid`, `otp_expired`, `otp_locked`.

---

### `POST /auth/native/contact/start-change`

**Used in:** Change Email (`ChangeEmailView`), Change Phone (`ChangePhoneView`).

**Auth:** Yes

**Request:**
```json
{
  "kind": "email" | "phone",
  "new_value": "newemail@example.com"
}
```

**Response:** Empty.

**Error codes:** `contact_in_use`, `invalid_contact`, `email_required_before_phone_change` (phone-change only — show "Add email first" modal).

---

### `POST /auth/native/contact/confirm-change`

**Used in:** Same OTP screens after `start-change`.

**Auth:** Yes

**Request:**
```json
{
  "kind": "email" | "phone",
  "new_value": "newemail@example.com",
  "code": "123456"
}
```

**Response:** Fresh `AuthEnvelope`.

**Error codes:** `otp_invalid`, `otp_expired`, `otp_locked`.

---

## 4. Invite & Waitlist

### `POST /verify-invite`

**Used in:** Invite gate (`InviteView`) — entry-point before sign-in/signup.

**Auth:** No

**Request:** `application/x-www-form-urlencoded`
```
invitation_code=ABC12345
```

**Response:**
```json
{
  "data": {
    "inviter_id": 42,
    "remaining_invites": 5,
    "month": "2026-06",
    "code_type": "peer" | "admin" | "agent"
  }
}
```
- `code_type` drives branching:
  - `peer` / `admin` → cache code in shared prefs as `verifiedInviteCode`, advance to OTP/OAuth signup.
  - `agent` → show "Web-only signup" modal directing to artrinx.com. **Don't proceed in app.**

**Error codes:** `invite_invalid` ("That invite code isn't valid.").

---

### `POST /waitlist`

**Used in:** Waitlist flow (`WaitlistView`).

**Auth:** No

**Custom headers:** `X-Client-Source: ios` (Android: `X-Client-Source: android`), `X-Device-Type: <iphone|android>`.

**Request:**
```json
{
  "email": "user@example.com",
  "phone_number": "+919999999999",
  "profile_type_id": 1,
  "first_name": "John",
  "accepted_terms": true,
  "sms_notifications_opt_in": true,
  "instagram_handle": "johnart" | null,
  "device_type": "iphone",
  "source_tag": "ad_campaign" | null
}
```

**Response:** Empty.

**Error codes:** `contact_in_use`, `invalid_contact`.

---

### `GET /profiles/by-invite-code`

**Used in:** "Invited friends" list (`InvitedUsersView`).

**Auth:** No

**Query:** `?invitation_code=ABC12345&page=1&size=20`

**Response:** `PaginatedResponse<InvitedUser>`:
```json
{
  "data": {
    "items": [
      {
        "id": 123,
        "username": "alice",
        "display_name": "Alice Smith",
        "profile_picture_url": "https://...",
        "profile_type_name": "Artist",
        "follower_count": 12
      }
    ],
    "page": 1,
    "size": 20,
    "total": 5
  }
}
```

---

### `GET /exhibitions/`

**Used in:** Exhibition devices list (Settings → Devices).

**Auth:** Yes

**Response:**
```json
{
  "data": [
    {
      "id": 1,
      "user_id": 42,
      "key": "device-key",
      "name": "Main Gallery Screen",
      "curation_id": 99,
      "artwork_id": null,
      "duration": 30,
      "orientation": "portrait" | "landscape",
      "status": "active"
    }
  ]
}
```

---

### `POST /exhibitions/`

**Used in:** "Pair new device" sheet.

**Auth:** Yes

**Request:**
```json
{
  "key": "device-key",
  "name": "Screen Name",
  "orientation": "portrait" | "landscape",
  "status": "active"
}
```

**Response:** Single `Device` object.

---

### `PUT /exhibitions/{exhibitionId}`

**Used in:** Edit device screen.

**Auth:** Yes

**Request:** (any subset)
```json
{
  "name": "Updated Name",
  "orientation": "portrait",
  "artwork_id": 55,
  "curation_id": 99,
  "duration": 30
}
```

**Response:** Updated `Device`.

---

### `DELETE /exhibitions/{exhibitionId}`

**Used in:** Device list → unpair.

**Auth:** Yes

**Response:** Empty.

---

## 5. Profile

### `GET /profile`

**Used in:** Tab refresh on Profile (`ProfileView.task`), `EditProfileView`, `SubscriptionDetailsView`, `AppState.fetchProfile()` on app launch.

**Auth:** Yes

**Response:** `UserProfile`
```json
{
  "data": {
    "id": 127,
    "auth_user_id": "uuid-string",
    "username": "manoj12",
    "full_name": "Manoj Kumar",
    "full_name_edit_count": 0,
    "display_name": "Manoj Kumar",
    "profile_type_name": "Artist",
    "bio": "Artist bio",                   // nullable
    "age": 32,                              // nullable
    "dob": "1993-08-12",                    // nullable
    "country": "India",                     // nullable
    "state": "Telangana",
    "city": "Karimnagar",
    "profile_link": null,                   // nullable
    "profile_picture_url": "https://...",   // nullable
    "address_line_1": null,
    "address_line_2": null,
    "zipcode": null,
    "no_physical_location": false,
    "invitation_code": "PZLJWB6X",
    "referred_by": null,
    "invited_by": null,
    "accepted_terms": true,
    "sms_2fa_consent": true,
    "account_notification_sms": true,
    "marketing_sms_consent": true,
    "joined_at": "2026-02-06T16:36:10.471848",
    "supabase_user_id": "uuid-string",      // legacy; equals auth_user_id
    "subscription": {
      "plan": "basic" | "artist_pro" | "gallery",
      "status": "active" | "trialing" | "cancelled" | "past_due" | "expired",
      "provider": "apple" | "stripe" | null,
      "expiresAt": "2026-12-31T00:00:00Z" | null,
      "renewsAt": "2026-12-31T00:00:00Z" | null,
      "trialEndsAt": "2026-12-31T00:00:00Z" | null
    },
    "mediums": [
      { "id": 1, "picture": "https://...", "title": "Painting" }
    ],
    "artwork_count": 2,
    "curation_count": 1,
    "follower_count": 8,
    "following_count": 6,
    "total_user_invites": 5,
    "remaining_invites": 5,
    "remaining_chat_invites": 15,
    "max_uploads": 10,
    "remaining_uploads": 8,
    "peer_invites": { "monthly_cap": 5, "remaining": 5 },
    "message_requests": { "monthly_cap": 15, "remaining": 15 }
  }
}
```

**Important — Android dev:** `Medium` Hashable/Equality must only compare `id` (the `picture` URL has cache-bust query strings that differ between `/profile.mediums` and `/mediums/` — equality on URL will cause "selected medium" mismatches).

---

### `PUT /profile/update` (multipart)

**Used in:** Edit profile (`EditProfileView`), Change role (`ChangeRoleView`), Phone permissions toggle, Add/Remove physical address.

**Auth:** Yes

**Request:** `multipart/form-data` — any subset of:
```
display_name=Manoj
bio=My bio
dob=1993-08-12
country=India
state=Telangana
city=Karimnagar
address_line_1=...
address_line_2=...
zipcode=...
no_physical_location=true|false
profile_link=https://...
profile_type_id=1
preferred_medium_ids=1,2,3                   // CSV, NOT JSON array
account_notification_sms=true|false
marketing_sms_consent=true|false
sms_2fa_consent=true|false
profile_picture=<binary>                     // optional file
```

**Response:** Updated `UserProfile`.

**Error codes:** `validation` (field-level), `username_taken`.

---

### `POST /profile` (multipart)

**Used in:** Profile setup wizard (`ProfileSetupView`).

**Auth:** Yes

**Request:** Form data including `profile_type_id`, `full_name`, `username`, `display_name`, `dob`, `country`, `state`, `city`, `preferred_medium_ids`, `profile_picture` (file).

**Response:** `UserProfile`. After this succeeds, `profile_completed` flips to true and user routes to main app.

---

### `GET /profile/username-check?username=foo`

**Used in:** Username field in profile setup (debounced as user types).

**Auth:** No

**Response:**
```json
{ "data": true }     // true = available, false = taken
```

---

### `GET /profile/{userId}/public/info`

**Used in:** Public profile screen (`PublicProfileView`) when tapping another user.

**Auth:** Yes

**Response:** Subset of `UserProfile` — public fields only (no email/phone/address/consents/subscription internals).

**Error codes:** `404` → "Profile not found." (pop screen).

---

### `GET /profile/{userId}/public/artworks?page=1&size=20`

**Used in:** Public profile artworks tab.

**Auth:** Yes

**Response:** `PaginatedResponse<UserArtwork>`.

---

### `GET /profile/{userId}/public/curations?page=1&size=20`

**Used in:** Public profile curations tab.

**Auth:** Yes

**Response:** `PaginatedResponse<UserCuration>`.

---

### `GET /profile/{Id}`

**Used in:** Artist profile mini-card on search results / curation details.

**Auth:** Yes

**Response:**
```json
{
  "data": {
    "id": 42,
    "username": "...",
    "display_name": "...",
    "profile_picture_url": "..." | null,
    "profile_type_name": "Artist",
    "follower_count": 100,
    "is_following": false
  }
}
```

---

### `POST /follow`

**Used in:** Follow button on profile/artist mini-cards.

**Auth:** Yes

**Request:**
```json
{ "followed_id": 42 }
```

**Response:** Empty.

---

### `DELETE /users/{userId}/unfollow`

**Used in:** Unfollow button.

**Auth:** Yes

**Response:** Empty.

---

### `GET /followers?page=1&size=20`

**Used in:** "Followers" list screen.

**Auth:** Yes

**Response:** `PaginatedResponse<FollowerUser>`.

---

### `GET /followed-users?page=1&size=20`

**Used in:** "Following" list screen.

**Auth:** Yes

**Response:** `PaginatedResponse<FollowedUser>`.

---

### `POST /block`

**Used in:** Block user from artist profile / chat menu.

**Auth:** Yes

**Request (block user):**
```json
{ "user_id": 42 }
```
**Request (block artwork):**
```json
{ "artwork_id": 55, "message": "Reason" }
```

**Response:** Empty.

**UI:** Show progress sheet → on success, show toast "Blocked. You won't see this user." and remove from feeds.

---

### `POST /unblock`

**Used in:** Blocked users list → unblock action.

**Auth:** Yes

**Request:**
```json
{ "user_id": 42 }
```

**Response:** Empty.

---

### `GET /blocked/users?page=1&size=20`

**Used in:** Settings → Blocked users.

**Auth:** Yes

**Response:** `PaginatedResponse<BlockedUser>`.

---

### `POST /report-message`

**Used in:** Report user action menu.

**Auth:** Yes

**Request:**
```json
{ "message": "Reason text from user", "reported_user_id": 42 }
```

**Response:** Empty. Toast "Report submitted. We'll review it."

---

### `POST /report-artwork`

**Used in:** Report artwork from artwork detail.

**Auth:** Yes

**Request:**
```json
{ "artwork_id": 55, "message": "Reason text" }
```

**Response:** Empty.

---

### `PATCH /user/delete-me`

**Used in:** Settings → Delete account (with confirmation).

**Auth:** Yes

**Response:**
```json
{
  "data": {
    "deleteOn": "2026-07-18T00:00:00Z",
    "markForDelete": true
  }
}
```

**UI:** Show "Account will be deleted on <date>" confirmation, then force sign-out.

---

## 6. Artworks

### `GET /artworks/shop?page=1&size=20`

**Used in:** Home → Shop feed.

**Auth:** Yes

**Response:** `PaginatedResponse<UserArtwork>`.

---

### `GET /artworks/all?page=1&size=20`

**Used in:** Home → Discover feed (everything).

**Auth:** Yes

**Response:** `PaginatedResponse<UserArtwork>`.

---

### `GET /artworks/?page=1&size=20`

**Used in:** Profile → My artworks tab.

**Auth:** Yes

**Response:** `PaginatedResponse<UserArtwork>` (the signed-in user's own uploads).

---

### `GET /artworks/{id}`

**Used in:** Artwork detail screen (`PublicArtDetailView`).

**Auth:** Yes

**Response:** `UserArtwork`
```json
{
  "data": {
    "id": 362,
    "user_id": 127,
    "title": "Flowers.",
    "image": "artworks/uuid.jpeg",
    "description": "Flowers on red",
    "artist": { "artist_name": "Manoj Kumar", "artist_id": 127 },
    "medium": { "id": 6, "title": "Photography", "picture": "https://..." },
    "shop_link": "https://google.com" | null,
    "price": 12.0 | null,
    "privacy": false,
    "tags": ["Flowers"],
    "rekognition_tags": [],
    "aspect_ratio": 1.333,
    "size": {
      "height_cm": 24.0,
      "width_cm": 34.0,
      "unit": "cm",
      "orientation": "landscape",
      "display": { "cm": "24 × 34 cm", "in": "9.4 × 13.4 in" }
    } | null,
    "city": "Karimnagar",
    "state": "Telangana",
    "country": "India",
    "image_url": "https://...",
    "thumbnail_url": "https://...?width=256&format=webp",
    "webp_url": "https://...?format=webp",
    "display_name": "Manoj Kumar",
    "profile_type_id": 1,
    "profile_type_name": "Artist",
    "profile_picture_url": "https://..." | null,
    "likes_count": 4,
    "is_liked": false
  }
}
```

**Error codes:** `404` → "This artwork is no longer available." Pop screen.

---

### `DELETE /artworks/{id}`

**Used in:** Profile → My artworks → long press / menu → delete.

**Auth:** Yes

**Response:** Empty.

**Error codes:** `404` → silent (already gone, treat as success).

---

### `PUT /artworks/{id}`

**Used in:** Edit artwork screen (`EditArtView`).

**Auth:** Yes

**Request:**
```json
{
  "title": "...",                          // nullable
  "description": "...",                    // nullable
  "artist": { "artist_name": "...", "artist_id": 42 },   // nullable
  "medium_id": 1,                          // nullable
  "privacy": false,                        // nullable
  "shop_link": "https://..." | null,
  "price": 50.0 | null,
  "tags": ["new", "tags"],                 // nullable
  "size": { "height_cm": 24.0, "width_cm": 34.0, "unit": "cm" }  // nullable
}
```
Send only the fields the user actually changed (others omitted, not nulled).

**Response:** Updated `UserArtwork`.

---

### `POST /artworks/like`

**Used in:** Heart button on artwork cards / detail.

**Auth:** Yes

**Request:**
```json
{ "artwork_id": 55 }
```

**Response:** Empty.

**Offline handling:** iOS optimistically updates the heart, then enqueues the mutation to a SwiftData queue if the network is offline. The queue drains on reconnect. Android equivalent: Room-based mutation queue.

---

### `DELETE /artworks/{artworkId}/like`

**Used in:** Heart-off button.

**Auth:** Yes

**Response:** Empty.

---

### `GET /artworks/recommended?page=1&size=20`

**Used in:** Recommendations carousel on home.

**Auth:** Yes

**Response:** `PaginatedResponse<UserArtwork>`.

---

### `GET /artworks/{artworkId}/similar?page=1&size=20`

**Used in:** Artwork detail → "Similar works" section.

**Auth:** Yes

**Response:** `PaginatedResponse<UserArtwork>`.

---

### `GET /artworks/liked?page=1&size=20`

**Used in:** Profile → Liked tab.

**Auth:** Yes

**Response:** `PaginatedResponse<UserArtwork>`.

---

### `GET /artworks/by-artist-id/{artistId}?page=1&size=20`

**Used in:** Public profile → Artworks tab (for guest artists where artist_id is set).

**Auth:** Yes

**Response:** `PaginatedResponse<UserArtwork>`.

---

### `GET /artworks/by-name/{artistName}?page=1&size=20`

**Used in:** Search → tap on an artist name that doesn't have a user profile.

**Auth:** Yes

**Response:** `PaginatedResponse<UserArtwork>`.

---

## 7. Curations

### `GET /curations/{id}`

**Used in:** Curation detail screen.

**Auth:** Yes

**Response:** `UserCuration`
```json
{
  "data": {
    "id": 99,
    "user_id": 42,
    "title": "My Collection",
    "description": "...",
    "privacy": false,
    "artworks": [ { "id": 55, ... }, ... ],
    "author": {
      "id": 42, "username": "...", "display_name": "...",
      "profile_picture": "..." | null
    },
    "is_exhibition": false,
    "is_liked": false,
    "likes_count": 10,
    "no_of_screens": 2,
    "screens": [ { "id": 1, "name": "Screen 1", "key": "..." } ]
  }
}
```

---

### `POST /curations/like`

**Auth:** Yes

**Request:** `{ "curation_id": 99 }` → Empty response.

---

### `DELETE /curations/{curationId}/like`

**Auth:** Yes — Empty response.

---

### `GET /curations/?page=1&size=20`

**Used in:** Profile → My curations tab.

**Auth:** Yes

**Response:** `PaginatedResponse<UserCuration>`.

---

### `DELETE /curations/{id}`

**Auth:** Yes — Empty response.

---

### `POST /curations/`

**Used in:** Create curation wizard (`AddToCurationView`).

**Auth:** Yes

**Request:**
```json
{
  "title": "My Collection",
  "description": "...",
  "privacy": false,
  "artwork_ids": [55, 56, 57]
}
```

**Response:** `UserCuration`.

---

### `PUT /curations/{curationId}` (artwork-list update)

**Used in:** Add/remove artworks from a curation.

**Auth:** Yes

**Request:**
```json
{ "artwork_ids": [55, 56, 57] }
```

**Response:** Updated `UserCuration`.

---

### `PUT /curations/{curationId}` (general update)

**Used in:** Edit curation title/description/privacy.

**Auth:** Yes

**Request:** (any subset)
```json
{
  "title": "...",
  "description": "...",
  "privacy": true,
  "artwork_ids": [55, 56]
}
```

**Response:** Updated `UserCuration`.

---

## 8. Events

### `GET /events/{id}`

**Used in:** Event detail popup, triggered when tapping `event_reminder_*` or `event_created` notifications.

**Auth:** Yes

**Response:**
```json
{
  "data": {
    "id": 1,
    "event_name": "Art Exhibition",
    "image_url": "https://..." | null,
    "venue_name": "Gallery XYZ" | null,
    "street_address": "123 Main St" | null,
    "city": "New York" | null,
    "state": "NY" | null,
    "postal_code": "10001" | null,
    "country": "United States" | null,
    "start_at_utc": "2026-09-03T18:00:00Z",
    "end_at_utc": "2026-09-03T21:00:00Z",
    "event_tz": "America/New_York" | null
  }
}
```

**Error codes:** `404` → "This event is no longer available."

---

## 9. Messages / Chat

### `POST /messages/`

**Used in:** Send button in chat (`UserChatView`).

**Auth:** Yes

**Request:**
```json
{
  "receiver_id": 100,
  "text": "Hello!",
  "image_id": 55 | null,
  "client_message_id": "550e8400-e29b-41d4-a716-446655440000"
}
```
- `client_message_id` — generate a fresh UUIDv4 client-side for every send attempt, lowercased.
- This is **the dedup key**. Backend de-dups on it: same UUID on retry returns the original message, doesn't create a duplicate. **Always generate the UUID before the first send**; if the request fails/times out, retry with the SAME UUID.

**Response:**
```json
{
  "data": {
    "message": {
      "id": "<server-generated msg-uuid>",
      "chatroom_id": "<uuid>",
      "sender_id": 127,
      "receiver_id": 100,
      "text": "Hello!",
      "image_id": null,
      "image_url": null,
      "is_read": false,
      "is_deleted": false,
      "is_edited": false,
      "created_at": "2026-06-18T10:30:00Z",
      "edited_at": null,
      "media_user_id": null,
      "media_user_display_name": null,
      "media_user_profile_picture_url": null,
      "artwork_title": null,
      "sender": null
    },
    "chatroom_id": "<uuid>",
    "invitation_status": true,
    "is_blocked": false,
    "they_blocked_me": false,
    "is_active": true
  }
}
```

**Error codes:**
- `they_blocked_me`, `i_blocked` → "You can't message this user."
- `invite_pending` → "Awaiting your invitation acceptance." (the recipient has to accept first message before further sends).
- `rate_limited` → honor server message.

---

### `GET /my/chatrooms`

**Used in:** Chat list (`ChatListView`), AND also subscribed to at the TabBarView level for unread-badge counting.

**Auth:** Yes

**Response:**
```json
{
  "data": [
    {
      "chatroom_id": "<uuid>",
      "invitation_status": true,
      "user": {
        "id": 216,
        "username": "Rudraisha",
        "display_name": "Rudraish",
        "profile_type_name": "Artist",
        "profile_picture_url": "https://..." | null,
        "i_blocked": false,
        "they_blocked": false
      },
      "last_message": {
        "id": "<uuid>",
        "text": "How are you?",
        "image_id": null,
        "image_url": null,
        "sender_id": 216,
        "receiver_id": 127,
        "created_at": "2026-06-15T20:12:51Z",
        "is_read": true,
        "media_user_id": null,
        "media_user_display_name": null,
        "media_user_profile_picture_url": null,
        "artwork_title": null
      } | null,
      "unread_count": 0,
      "is_active": true,
      "remaining_invites": 15,
      "can_message": true,
      "block_reason": "invite_pending" | "i_blocked" | "they_blocked" | "rate_limited" | null
    }
  ]
}
```

---

### `GET /messages/with/{userId}?before=2026-06-18T10:30:00.123456Z&limit=50`

**Used in:** Chat thread (`UserChatView`).

**Auth:** Yes

**Query:**
- `before` — ISO 8601 with fractional seconds (optional). Returns messages strictly older than this timestamp.
- `limit` — 1–200 (optional, default ~50).

**Response:**
```json
{
  "data": {
    "invitation_status": true,
    "messages": [ { "id": "...", "text": "...", "created_at": "...", "sender_id": ..., "receiver_id": ..., ... } ],
    "i_blocked": false,
    "they_blocked_me": false,
    "is_active": true,
    "next_cursor": "2026-06-17T10:30:00.123456Z" | null,
    "chat_room_id": "<uuid>" | null,
    "can_message": true,
    "block_reason": null
  }
}
```

**Pagination behavior:**
- First load → `before` and `limit` both omitted → returns the most recent page.
- Scroll up → call with `before = next_cursor` from previous response.
- `next_cursor: null` → no more older pages.
- Messages always returned in ascending (oldest-first) order within the page.

---

### `DELETE /chatroom/with/{userId}`

**Used in:** Long-press chat in chat list → delete.

**Auth:** Yes

**Response:** Empty. Removes the chatroom from your side only.

---

### `PATCH /messages/{messageId}`

**Used in:** Long-press message → Edit.

**Auth:** Yes

**Request:**
```json
{ "text": "Edited text" }
```

**Response:** Updated `Message` (with `is_edited: true`, `edited_at` set).

---

### `DELETE /messages/{messageId}`

**Used in:** Long-press message → Delete.

**Auth:** Yes

**Response:** Empty.

---

### `POST /messages/{messageId}/read`

**Used in:** Automatically called when the recipient views a message in chat.

**Auth:** Yes

**Response:** Empty. Backend fans out a `chat_read` WS event to the sender.

---

### `POST /chatroom_id`

**Used in:** First message from any chat-initiation surface (artist profile → "Message" button), to check whether a chatroom already exists and what its state is.

**Auth:** Yes

**Request:**
```json
{ "user_id": 100 }
```

**Response:**
```json
{
  "data": {
    "exists": true,
    "chatroom_id": "<uuid>" | null,
    "invitation_status": true,
    "is_active": true,
    "i_blocked": false,
    "they_blocked_me": false,
    "remaining_invites": 15
  }
}
```

---

## 10. Notifications

### `GET /notifications`

**Used in:** Notifications tab (`NotificationView`), and subscribed to at `TabBarView` level for badge counting.

**Auth:** Yes

**Response:**
```json
{
  "data": {
    "notifications": [
      {
        "id": 3096,
        "type": "follow" | "artwork_like" | "artwork_share" | "curation_like" | "curation_share" | "profile_share" | "event_created" | "event_updated" | "event_reminder_week" | "event_reminder_day" | "event_reminder_3h",
        "message": "anvesh started following you",
        "is_read": true,
        "timestamp": "2026-06-18T04:52:16.366838",
        "actor": {
          "id": 245,
          "name": "anvesh",
          "profileImageUrl": "https://..."
        },
        "action": "followed" | "liked" | "shared" | null,
        "target": {
          "id": 127,
          "type": "profile" | "artwork" | "curation" | "event" | null,
          "name": "Manoj Kumar",                // present for profile/artwork
          "title": "Flowers.",                  // present for artwork/curation/event
          "profileImageUrl": "https://...",     // present for profile
          "thumbnailUrl": "https://..."         // present for artwork
        },
        // Only present for event_* types:
        "event_id": 175,
        "event_image_url": null,
        "organizer_name": "Kapil dev",
        "organizer_handle": "Kapil dev",
        "organizer_profile_url": "https://www.artrinx.com/profile/167"
      }
    ]
  }
}
```

---

### `PATCH /notifications/{id}/read`

**Used in:** Tap on a notification row.

**Auth:** Yes

**Response:** Empty.

---

## 11. Search

### `GET /search/users?query=foo`

**Used in:** Search tab → typing a user query.

**Auth:** Yes

**Response:**
```json
{
  "data": [
    {
      "id": 42, "username": "...", "display_name": "...",
      "profile_picture_url": "..." | null,
      "profile_type_name": "Artist",
      "follower_count": 100, "is_following": false
    }
  ]
}
```

---

### `GET /search/trending-tags`

**Used in:** Search tab empty-state ("Trending" chips).

**Auth:** Yes

**Response:**
```json
{ "data": ["landscape", "abstract", "digital"] }
```

---

### `GET /search` (with complex query params)

**Used in:** Global search (`SearchView`).

**Auth:** Yes

**Query:** Repeating params for arrays.
- `search=<text>`
- `medium_ids=1&medium_ids=2`
- `tags=landscape&tags=nature`
- `country=US`, `state=NY`, `city=New+York`
- `sort_by=popular|recent`
- `shop_art=true|false`

**Response:**
```json
{
  "data": {
    "artworks": [ { "id": 55, ... } ],
    "curations": [ { "id": 99, ... } ],
    "users": [ { "id": 42, ... } ]
  }
}
```

---

### `GET /feed/discover`

**Used in:** Home tab default feed.

**Auth:** Yes

**Response:**
```json
{
  "data": {
    "sponsored": [
      {
        "id": 1, "title": "...", "subtitle": "...",
        "image_url": "https://...",
        "link": "https://..." | null,
        "artist": "...", "views": 829, "clicks": 10, "is_active": true,
        "created_at": "...", "updated_at": "..."
      }
    ],
    "new_art": [ { "id": 55, ... } ],
    "popular_curations": [ { "id": 99, "artworks": [...], "user_id": 42, ... } ]
  }
}
```

---

### `GET /mediums/`

**Used in:** Profile setup (medium picker), Edit Mediums in settings, Upload screen medium picker, Search filter sheet.

**Auth:** Yes (as of 2026-06; previously open — see legacy clients).

**Response:**
```json
{
  "data": [
    { "id": 1, "title": "Painting", "picture": "https://..." },
    { "id": 2, "title": "Sculpture", "picture": "https://..." }
  ]
}
```

---

## 12. Locations

### Track 1 — Scoped (filtered to locations that have content)

These are used for search filter chips ("show me places where art actually exists").

#### `GET /search/locations/countries?sort=count`

**Auth:** Yes

**Response:**
```json
{
  "data": {
    "items": [
      { "name": "United States", "count": 150 },
      { "name": "France", "count": 80 }
    ],
    "total": 195
  }
}
```

#### `GET /search/locations/states?country=United+States&sort=count`

#### `GET /search/locations/cities?country=United+States&state=NY&q=New&limit=200&sort=count`

Same envelope. `q` is a case-insensitive prefix filter. `limit` capped at 500 server-side.

---

### Track 2 — Master catalog (every country/state/city in the world)

Used in profile editor location picker / upload artwork location picker.

#### `GET /locations/master/countries`

**Auth:** Yes

**Response:**
```json
{
  "data": {
    "items": [
      {
        "name": "United States",
        "iso2": "US", "iso3": "USA",
        "phone_code": "1",
        "emoji": "🇺🇸",
        "continent": "NA",
        "currency_code": "USD"
      }
    ],
    "total": 250
  }
}
```
- Cache locally for 24h (response has `Cache-Control: max-age=86400`).

#### `GET /locations/master/states?country_iso2=US`

**Response:**
```json
{
  "data": {
    "items": [ { "name": "New York", "state_code": "NY", "iso_3166_2": "US-NY" } ],
    "total": 50
  }
}
```

#### `GET /locations/master/cities?country_iso2=US&state_code=NY&limit=200&q=New`

**Response:**
```json
{
  "data": {
    "items": [
      { "name": "New York", "latitude": 40.7128, "longitude": -74.0060 }
    ],
    "total": 5
  }
}
```

---

## 13. Upload

Two-step: iOS gets a presigned S3 URL, uploads the image directly to S3, then calls the create endpoint with the S3 path.

### `POST /prepare-upload/`

**Used in:** First step of artwork upload flow (`ArtworkUploadView`).

**Auth:** Yes

**Request:** Empty.

**Response:**
```json
{
  "data": {
    "file_path": "uploads/2026-06/artwork-uuid.jpg",
    "upload_url": "https://s3-presigned-url.example.com/..."
  }
}
```

**Behavior:** Client uses `PUT` to `upload_url` with the image bytes directly. **Do not** send the bytes through the artRINX backend.

---

### `POST /artworks/` (multipart)

**Used in:** Final step of upload, after S3 upload completes.

**Auth:** Yes

**Request:** `multipart/form-data`
```
title=My Artwork
description=Description text
medium_id=1
privacy=false
shop_link=https://...                          (optional)
price=500.00                                    (optional)
tags=landscape,nature                           (CSV)
artist_name=John Doe                            (optional, for guest artists)
artist_id=42                                    (optional, if linking to existing user)
image_file_path=uploads/2026-06/artwork-uuid.jpg
size_height_cm=24.0                             (optional)
size_width_cm=34.0                              (optional)
size_unit=cm                                    (optional, "cm"|"in")
city=Hyderabad                                  (optional)
state=Telangana                                 (optional)
country=India                                   (optional)
```

**Response:**
```json
{
  "data": {
    "id": 55,
    "image_url": "https://cdn.example.com/artwork-55.jpg"
  }
}
```

**Error codes:** `validation` for missing required field (e.g. medium_id, image_file_path).

---

## 14. Analytics

### `POST /admin/click?type=banner&link=https://...`

**Auth:** Yes

**Response:** `{ "data": { "count": 42 } }`

---

### `POST /admin/duration?type=banner&link=https://...&duration=30`

**Auth:** Yes

**Response:** Empty. `duration` in seconds.

---

### `POST /admin/view-history/record`

**Used in:** Artwork detail open, curation detail open.

**Auth:** Yes

**Request:**
```json
{ "target_id": 55, "target_type": "artwork" | "curation" | "profile" }
```

**Response:** Empty.

---

### `POST /banners/track`

**Used in:** Banner carousel impression / tap tracking.

**Auth:** Optional Bearer (works either way).

**Request:**
```json
{ "type": "view" | "click", "banner_id": 1 }
```

**Response:** `{ "data": { "views": 100, "clicks": 25 } }`

---

### `POST /share`

**Used in:** Share-to-user action sheet.

**Auth:** Yes

**Request:** Exactly one of `artwork_id` / `curation_id` / `profile_id` populated.
```json
{
  "to_user_ids": [42, 100],
  "artwork_id": 55,
  "curation_id": null,
  "profile_id": null
}
```

**Response:** Empty.

---

### `POST /me/fcm-token`

**Used in:** App launch, after sign-in.

**Auth:** Yes

**Request:**
```json
{ "fcm_token": "..." }
```

**Response:** `{ "data": { "operation": "updated", "fcm_token": "..." } }`

**Note:** iOS endpoint name is `fcm-token` but iOS sends an APNs-routed-through-FCM token (Firebase Messaging swizzles APNs → FCM). Android sends the raw FCM token. Same endpoint, same shape.

---

### `DELETE /me/fcm-token`

**Used in:** Sign-out (before wiping local tokens).

**Auth:** Yes

**Response:** Empty. Best-effort — don't block sign-out on this.

---

## 15. WebSocket (real-time)

iOS opens a single authenticated WebSocket for the lifetime of the signed-in session. Android should mirror this.

### Connection

```
wss://<host>/ws?token=<access_token>
```

- Reconnect with exponential backoff (capped at 60s) on disconnect.
- Disconnect on path-loss (NetworkMonitor), reconnect with a fresh access token (the previous one may have expired during the offline window).
- Distinguish between user-initiated close (sign-out) and transport drop — only retry on transport drops.

### Event envelope

```json
{ "type": "<event_type>", "data": { ... } }
```

### Event types

| `type` | When fired | `data` shape | Client action |
|---|---|---|---|
| `chat` | New incoming message in any chatroom for this user | `{ "id": "...", "chatroom_id": "...", "sender_id": ..., "receiver_id": ..., "text": "...", "image_url": null, "created_at": "...", "sender": { "id": ..., "username": "...", "display_name": "...", "profile_picture_url": "...", "profile_type_name": "Artist" } | null }` | If user is in the matching chatroom: append to thread, mark as read (POST /messages/{id}/read). Otherwise: increment chatroom unread, insert new row in chat list if no existing row (use inline `sender` block to avoid round-trip). |
| `chat_edit` | The other participant edited a message | `{ "message_id": "...", "chatroom_id": "...", "text": "...", "is_edited": true, "edited_at": "..." }` | Find the message in-memory, update text/edited_at, mark `is_edited`. |
| `chat_delete` | The other participant deleted a message | `{ "message_id": "...", "chatroom_id": "...", "is_deleted": true }` | Remove the message from the local thread. |
| `chat_read` | The other participant read one of our messages | `{ "message_id": "...", "chatroom_id": "...", "reader_id": <Int> }` | Flip `is_read = true` on the message; surface the read-receipt indicator. |
| `typing` | The other participant is/stopped typing | `{ "chatroom_id": "...", "user_id": <Int>, "is_typing": true|false }` | If user is in matching chatroom AND `user_id` matches the other participant: show/hide typing-dots bubble. Auto-clear after 6s of no fresh event. |
| `notification` | New notification (follow, like, share, etc.) | The same shape as items in `GET /notifications.data.notifications` | Prepend to in-memory notifications list, increment bell badge. |

### Outbound (client → server)

| `type` | `data` shape | When |
|---|---|---|
| `typing` | `{ "chatroom_id": "...", "is_typing": true|false }` | When the local user starts/stops typing in a chat thread. Debounce so you send `is_typing: true` at most once per 5 seconds while still typing, and `is_typing: false` after 5s of no keystrokes (or on `text == ""` or on disappear). Server rate-limits to ≤1 event per (sender, chatroom) per 2s. |

### Reconnection

- iOS subscribes to `NetworkMonitor.onlineStateChanges` and uses a `listenToken: UUID` to prevent orphan receive closures from a torn-down task scheduling new receives on a stale handle.
- After reconnect, iOS does NOT re-fetch chatrooms — the chatrooms list endpoint is the canonical recovery; rely on backfill-on-reconnect that merges by message id.

---

## 16. Push Notifications (FCM)

Backend sends FCM push notifications. iOS receives via APNs→FCM swizzle; Android receives natively via FCM.

### Payload shape

```json
{
  "notification": {
    "title": "<display name>",
    "body": "<short preview>"
  },
  "data": {
    "type": "chat" | "notification" | "event_reminder",
    "url": "rinxart://chat/<sender_user_id>"     // or "rinxart://artwork/<id>", etc.
  },
  "apns": {
    "payload": {
      "aps": {
        "badge": 3                  // sum of (unread notifications + unread chat messages)
      }
    }
  },
  "apns-collapse-id": "<chatroom_id>",
  "apns-priority": "10"
}
```

### Deep-link URLs

| URL | Routes to |
|---|---|
| `rinxart://chat/<userId>` | Open chat thread with that user |
| `rinxart://artwork/<id>` | Open artwork detail |
| `rinxart://curation/<id>` | Open curation detail |
| `rinxart://profile/<userId>` | Open profile |
| `rinxart://event/<id>` | Open event detail popup |
| `rinxart://invite?code=ABC` | Verify invite code, start signup flow |

### Universal links (Android App Links)

Same routes also work via `https://www.artrinx.com/<path>`. Add the appropriate `assetlinks.json` entry.

### Tap handling

- App in foreground: suppress the system banner if the user is already viewing the relevant screen (track `activeChatUserId` etc. globally).
- App in background: tap → cold-start with the deep-link URL → routing layer handles the URL like any other.
- Clear the matching notification from the system tray when the user opens its target.

---

## Quick reference: what to display for each error

| Scenario | UI element | Copy |
|---|---|---|
| 401 (after refresh retry also fails) | Force sign-out + toast on auth screen | "Session expired. Please sign in again." |
| 403 `identity_disabled` | Force sign-out + modal | "Your account is disabled. Please contact support." |
| 403 `not_allowed_on_this_surface` | Force sign-out + modal | "This account can't sign in on the mobile app. Please use the web portal." |
| 403 `account_banned` | Force sign-out + modal | Server's `data.message` verbatim |
| 404 (typical) | Pop screen / silent remove | "This item is no longer available." (if it's a tap target) |
| 409 (typical) | Inline error or modal | Endpoint-specific copy (see per-endpoint tables) |
| 422 | Field-level red inline | "Please check the form." + per-field server msg if present |
| 429 | Disable action + countdown | "Too many attempts. Please wait." (use server's `data.retry_after` for the timer) |
| 5xx | Toast with retry button | "The server is having trouble. Please try again." |
| Network offline | Toast | "No internet connection. Please check your network and try again." |
| Network timeout | Toast | "Request timed out. Please try again." |
| Cancelled (user navigated away) | (silent) | — |

---

*Generated from iOS client source on 2026-06-18. Endpoint set verified against `rinx-v2/Network/Endpoints/APIService+*.swift` and `rinx-v2/Features/Authentication/NativeAuthClient.swift`. For wire shapes not covered here, the iOS source is the canonical reference.*

