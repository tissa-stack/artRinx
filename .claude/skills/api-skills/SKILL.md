# RINX Backend API — Android Handout

Complete API contract for the RINX backend, written for the Android client. Use as the source of truth when wiring up integrations.

Every endpoint section follows the same shape: **method · path · auth · request body · response body · notes**. Where useful, a `curl` example is included — copy, replace the token, and paste into Postman/Insomnia to verify.

---

## 0. Quick reference

- **Base URL:** `https://apifargate.rinx.com/api/`
- **Auth header (when required):** `Authorization: Bearer <accessToken>`
- **Required on every request:** `Content-Type: application/json` (unless multipart or form-encoded), `X-App-Version: <semantic-version>`
- **JSON convention:** snake_case on the wire (with a few camelCase fields called out in §14.3)
- **Date format:** ISO 8601 with fractional seconds (`2026-05-30T11:16:10.187Z`)
- **UUIDs:** lowercase strings on the wire
- **Pagination:** `?page=1&size=10` (1-indexed), except chat history which uses cursor pagination
- **Standard response envelope:**
  ```json
  { "success": true, "code": 200, "message": "...", "data": { ... } }
  ```
  `data` is `null` for empty-data responses.

---

## 1. Authentication

All paths under `auth/native/`. No `Authorization` header on the unauthed endpoints.

### 1.1 Request OTP

```
POST /api/auth/native/request-otp
```

| Auth | None |
|---|---|
| Content-Type | `application/json` |
| Response | empty success envelope |

**Request body** (channel-keyed; send **exactly one** of `email` or `phone`):

```json
{
  "phone": "+14155551234",
  "mode": "signup",
  "invite_code": "ABCD1234"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `email` | string | one-of | Email address; omit if sending phone |
| `phone` | string | one-of | E.164 format with leading `+` |
| `mode` | string | yes | `"signin"` or `"signup"` |
| `invite_code` | string | signup only | Peer / admin / agent code (any of three types, see §2.1) |
| `referral_code` | string | optional | Alternate field name for the same code; either name works |

**Response:**

```json
{
  "success": true,
  "code": 200,
  "message": "OTP sent",
  "data": null
}
```

**Possible errors:**

| HTTP | `detail.code` | Cause |
|---|---|---|
| 400 | `invite_invalid` | Code not in any source |
| 400 | `already_registered` | Signing up an email/phone that already has an account |
| 422 | (Pydantic shape) | Both `email` and `phone` sent, or neither |
| 429 | `rate_limited` | Too many requests for this contact |

**curl:**

```bash
curl -X POST https://apifargate.rinx.com/api/auth/native/request-otp \
  -H 'Content-Type: application/json' \
  -H 'X-App-Version: 1.0.0' \
  -d '{"phone":"+14155551234","mode":"signin"}'
```

### 1.2 Verify OTP

```
POST /api/auth/native/verify-otp
```

| Auth | None |
|---|---|
| Content-Type | `application/json` |
| Response | auth envelope (§1.4) |

**Request body:**

```json
{
  "phone": "+14155551234",
  "code": "123456",
  "mode": "signup",
  "invite_code": "ABCD1234",
  "consents": {
    "accepted_terms": true,
    "sms_2fa_consent": true,
    "account_notification_sms": true,
    "marketing_sms_consent": false
  }
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `email` / `phone` | string | one-of | Same channel as the `request-otp` call |
| `code` | string | yes | 6-digit OTP (key is `code`, **not** `otp`) |
| `mode` | string | yes | `"signin"` or `"signup"` |
| `invite_code` | string | signup only | Same as request-otp |
| `referral_code` | string | optional | Alternate code field |
| `consents` | object | signup only | Required for signup; structure above |

**Response:** see §1.4 below.

**Possible errors:**

| HTTP | `detail.code` | Cause |
|---|---|---|
| 400 | `otp_invalid` | Wrong code |
| 400 | `otp_expired` | Code older than ~5 min |
| 400 | `otp_locked` | 5 wrong attempts; honor `retry_after` |
| 400 | `invite_invalid` | Signup gate failed |
| 400 | `already_registered` | Edge race — try sign-in flow |

### 1.3 Resend OTP

```
POST /api/auth/native/resend-otp
```

| Auth | None |
|---|---|
| Body | `{"phone": "+14155551234"}` **or** `{"email": "x@y.com"}` |
| Response | empty success envelope |

Same throttle / locking rules as `request-otp`.

### 1.4 Auth envelope (returned by verify-otp, refresh, confirm-add, confirm-change)

```json
{
  "success": true,
  "code": 200,
  "message": "OK",
  "data": {
    "access_token": "<jwt>",
    "refresh_token": "<opaque-48-byte-string>",
    "access_expires_in": 900,
    "refresh_expires_in": 2592000,
    "user": {
      "id": 127,
      "auth_user_id": "eb9fadd3-7ecb-4ce8-9968-5f4ce5a4c3ce",
      "email": "x@y.com",
      "phone": "+14155551234",
      "email_verified": true,
      "phone_verified": false,
      "is_admin": false,
      "role": "artist",
      "profile_exists": true,
      "profile_completed": true,
      "consents": {
        "accepted_terms": true,
        "sms_2fa_consent": true,
        "account_notification_sms": true,
        "marketing_sms_consent": false
      }
    }
  }
}
```

| Field | Type | Notes |
|---|---|---|
| `access_token` | string (JWT) | Use as Bearer for ~15 minutes |
| `refresh_token` | string | Opaque 48-byte URL-safe; store securely |
| `access_expires_in` | int | Seconds until access expires |
| `refresh_expires_in` | int | Seconds until refresh expires (30 days) |
| `user.id` | int **or null** | Null mid-signup before profile creation |
| `user.auth_user_id` | UUID string | Stable across credential changes |
| `user.email` / `phone` | string **or null** | Either may be null depending on signup method |
| `user.role` | string **or null** | `admin\|agent\|gallery\|artist\|collector\|curious`; null mid-signup |
| `user.consents` | object **or null** | Null mid-signup or for legacy users |

**Kotlin tip:** declare `id`, `role`, `consents` as nullable in your data class. Non-null annotations will fail every fresh signup.

### 1.5 Refresh

```
POST /api/auth/native/refresh
```

| Auth | None (the refresh token IS the authentication) |
|---|---|
| Body | `{"refresh_token": "<opaque>"}` |
| Response | auth envelope (§1.4) |

**Single-flight required** — concurrent callers must coalesce onto one HTTP round-trip, otherwise the backend's reuse-detection invalidates the entire token family and signs the user out.

Pattern in Kotlin (sketch):

```kotlin
private val refreshMutex = Mutex()
private var lastRefreshAt: Long = 0L
private var lastPair: TokenPair? = null

suspend fun refresh(currentRefresh: String): TokenPair = refreshMutex.withLock {
    // 5-second recency cache — sequential callers reuse the last result
    val cached = lastPair
    if (cached != null && System.currentTimeMillis() - lastRefreshAt < 5_000) {
        return@withLock cached
    }
    val fresh = api.refresh(RefreshBody(currentRefresh)).data
    lastRefreshAt = System.currentTimeMillis()
    lastPair = fresh
    fresh
}
```

**Possible errors:**

| HTTP | `detail.code` | Action |
|---|---|---|
| 401 | `refresh_invalid` | Wipe local session, sign out |
| 401 | `refresh_reused` | Wipe local session, sign out with security message |

### 1.6 Logout

```
POST /api/auth/native/logout
```

| Auth | None |
|---|---|
| Body | `{"refresh_token": "<opaque>"}` |
| Response | empty success envelope (always 200, even if token was already revoked) |

Always follow with a local token wipe regardless of network outcome.

### 1.7 Sign out everywhere

```
POST /api/auth/native/sign-out-all
```

| Auth | **Yes** — Bearer |
|---|---|
| Body | none |
| Response | empty success envelope |

Revokes every refresh token belonging to this user across all devices. Access tokens still work until they expire (no server-side blacklist).

### 1.8 Contact add / change (4 endpoints)

For adding a phone to an email-only account, or changing email/phone. All require Bearer.

| Endpoint | Body | Response |
|---|---|---|
| `POST /api/auth/native/contact/start-add` | `{"kind":"email\|phone","value":"<contact>"}` | empty |
| `POST /api/auth/native/contact/confirm-add` | `{"kind":...,"value":...,"code":"123456"}` | fresh auth envelope |
| `POST /api/auth/native/contact/start-change` | `{"kind":"email\|phone","new_value":"<contact>"}` | empty |
| `POST /api/auth/native/contact/confirm-change` | `{"kind":...,"new_value":...,"code":"123456"}` | fresh auth envelope |

**Note:** Confirm-change revokes every refresh token for the user (forced re-auth on other devices). The current device receives a fresh pair in the response.

---

## 2. Invite & onboarding

### 2.1 Verify invite

```
POST /api/verify-invite
```

| Auth | None |
|---|---|
| Content-Type | `application/x-www-form-urlencoded` *(form, **not** JSON)* |
| Body | `invitation_code=ABCD1234` |
| Response | data shape below |

**Three code types accepted in the same field:**

| Type | Source | Format | Example |
|---|---|---|---|
| **peer** | Auto-generated when any gallery user signs up | `[A-Z0-9]{8}` | `ABCD1234` |
| **admin** | Admin-issued bulk codes | `[A-Z0-9]+` (variable) | `LAUNCH2026` |
| **agent** | Issued when an agent is created | `[A-Z0-9]{10}` (fixed 10) | `XK3M9Q2B7N` |

Server normalizes input (strips hyphens/spaces, uppercases). Client filter should be `[A-Z0-9]+` with no length cap. **Do not** length-validate by code type — different sources have different lengths.

**Success response:**

```json
{
  "success": true,
  "code": 200,
  "message": "Invitation code verified successfully",
  "data": {
    "inviter_id": 42,
    "remaining_invites": 5,
    "month": "June 2026",
    "code_type": "peer"
  }
}
```

| Field | Type | Notes |
|---|---|---|
| `inviter_id` | int **or null** | Null for admin and agent codes |
| `remaining_invites` | int **or null** | Null for agent codes; for peer/admin it's the monthly remaining |
| `month` | string | Human-readable current month |
| `code_type` | string | `"peer"` \| `"admin"` \| `"agent"` |

**Error responses:**

```json
{ "success": false, "code": 400, "message": "Invalid invitation code", "data": null }
{ "success": false, "code": 400, "message": "This invitation code has expired.", "data": null }
{ "success": false, "code": 400, "message": "This invitation code has no remaining invites left for this month.", "data": { ... peer code data ... } }
```

**curl:**

```bash
curl -X POST https://apifargate.rinx.com/api/verify-invite \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'X-App-Version: 1.0.0' \
  --data-urlencode 'invitation_code=ABCD1234'
```

### 2.2 Waitlist

```
POST /api/waitlist
```

| Auth | None |
|---|---|
| Content-Type | `application/json` |
| Optional headers | `X-Client-Source: android`, `X-Device-Type: <model>` |

**Body:**

```json
{
  "email": "x@y.com",
  "phone_number": "+14155551234",
  "profile_type_id": 1,
  "first_name": "Jane",
  "accepted_terms": true,
  "sms_notifications_opt_in": true,
  "instagram_handle": "@jane",
  "device_type": "android",
  "source_tag": "instagram_ad"
}
```

### 2.3 People I invited

```
GET /api/profiles/by-invite-code?invitation_code=ABCD1234&page=1&size=10
```

| Auth | None |
|---|---|
| Response | Paginated user list |

---

## 3. Profile

### 3.1 Get my profile

```
GET /api/profile
```

| Auth | Yes |
|---|---|
| Response | Profile object below |

**Response `data`:**

```json
{
  "id": 42,
  "auth_user_id": "eb9fadd3-7ecb-4ce8-9968-5f4ce5a4c3ce",
  "username": "jane",
  "full_name": "Jane Doe",
  "full_name_edit_count": 0,
  "display_name": "Jane",
  "profile_title": "Painter",
  "bio": null,
  "age": null,
  "country": null,
  "state": null,
  "city": null,
  "profile_link": null,
  "profile_picture_url": "https://cdn.example/...jpg",
  "invitation_code": "ABCD1234",
  "referred_by": null,
  "accepted_terms": true,
  "sms_2fa_consent": true,
  "account_notification_sms": true,
  "marketing_sms_consent": false,
  "subscription": { /* §9 */ },
  "joined_at": "2026-02-06T16:36:10Z",
  "mediums": [{"id": 1, "title": "Painting", "picture": "https://..."}],
  "artwork_count": 12,
  "curation_count": 3,
  "follower_count": 120,
  "following_count": 50
}
```

**Critical:** `invitation_code` can be **null** for agent users (PR #76 / migration `ac01`). Declare it nullable.

### 3.2 Create profile (multipart)

```
POST /api/profile
```

| Auth | Yes |
|---|---|
| Content-Type | `multipart/form-data` |

Form fields (strings on the wire): `username`, `display_name`, `profile_title`, `bio`, `age`, `country`, `state`, `city`, `profile_link`, `medium_ids[]`, `accepted_terms`, `sms_2fa_consent`, `account_notification_sms`, `marketing_sms_consent`.

File field: `profile_picture` (image).

### 3.3 Update profile (multipart)

```
PUT /api/profile/update
```

Same multipart shape as 3.2. Send only changed fields.

### 3.4 Username availability

```
GET /api/profile/username-check?username=jane
```

| Auth | None |
|---|---|
| Response | `data: true/false` (true = available) |

### 3.5 Other user's public profile

```
GET /api/profile/{userId}/public/info
```

| Auth | Yes |
|---|---|

**Response `data`:**

```json
{
  "username": "...",
  "display_name": "...",
  "profile_type_name": "Artist",
  "profile_picture_url": "https://...",
  "bio": "...",
  "profile_link": "...",
  "artwork_count": 12,
  "curation_count": 3,
  "following_count": 50,
  "follower_count": 120,
  "is_following": false,
  "chatroomId": "<uuid-or-null>"
}
```

`chatroomId` is **camelCase** on the wire — see §14.3.

### 3.6 Other public-profile endpoints

| Endpoint | Returns |
|---|---|
| `GET /api/profile/{userId}/public/artworks?page=&size=` | Paginated Artwork list (§4.4) |
| `GET /api/profile/{userId}/public/curations?page=&size=` | Paginated Curation list (§5.2) |
| `GET /api/profile/{userId}` | Artist-focused search-result profile |

### 3.7 Follow / unfollow

| Endpoint | Method | Body |
|---|---|---|
| `POST /api/follow` | POST | `{"followed_id": 42}` |
| `DELETE /api/users/{userId}/unfollow` | DELETE | none |
| `GET /api/followers?page=&size=` | GET | none |
| `GET /api/followed-users?page=&size=` | GET | none |

### 3.8 Block / unblock

| Endpoint | Body | Use |
|---|---|---|
| `POST /api/block` | `{"user_id": 42}` | Block a user |
| `POST /api/block` | `{"artwork_id": 99, "message": "spam"}` | Block + report an artwork |
| `DELETE /api/unblock?user_id=42` | none | Unblock a user |
| `GET /api/blocked/users?page=&size=` | none | Paginated blocked list |

### 3.9 Report

| Endpoint | Body |
|---|---|
| `POST /api/report-message` | `{"message":"...","reported_user_id":42}` |
| `POST /api/report-artwork` | `{"artwork_id":99,"message":"..."}` |

### 3.10 Delete account

```
PATCH /api/user/delete-me
```

Returns success ack + deletion-window info. Server schedules a worker job to permanently delete after the grace period.

### 3.11 Feedback

```
POST /api/feedbacks/
{"review": "<message>"}
```

---

## 4. Artworks

All require Bearer auth.

### 4.1 Browse endpoints (all paginated)

| Endpoint | Returns |
|---|---|
| `GET /api/artworks/?page=&size=` | Current user's own artworks |
| `GET /api/artworks/all?page=&size=` | Global public feed |
| `GET /api/artworks/shop?page=&size=` | Artworks with `shop_link` set |
| `GET /api/artworks/liked?page=&size=` | Artworks the current user liked |
| `GET /api/artworks/recommended?page=&size=` | Personalized recommendations |
| `GET /api/artworks/{id}/similar?page=&size=` | Similar artworks |
| `GET /api/artworks/by-artist-id/{artistId}?page=&size=` | By an artist's ID |
| `GET /api/artworks/by-name/{artistName}?page=&size=` | By artist display name (URL-encoded) |

All return paginated lists wrapping the Artwork shape (§4.4).

### 4.2 Single artwork

| Endpoint | Method | Body | Returns |
|---|---|---|---|
| `GET /api/artworks/{id}` | GET | — | Artwork |
| `DELETE /api/artworks/{id}` | DELETE | — | empty |
| `PUT /api/artworks/{id}` | PUT | partial update | Artwork |

**PUT body** (all fields optional; send only what changed):

```json
{
  "title": "...",
  "description": "...",
  "tags": ["abstract"],
  "shop_link": "https://...",
  "price": 120.5,
  "privacy": false,
  "medium_id": 1
}
```

### 4.3 Like / unlike

| Endpoint | Body |
|---|---|
| `POST /api/artworks/like` | `{"artwork_id": 99}` |
| `DELETE /api/artworks/{artworkId}/like` | none |

### 4.4 Artwork shape

```json
{
  "id": 99,
  "userId": 42,
  "title": "...",
  "description": "...",
  "artist": {
    "artist_id": 42,
    "artist_name": "..."
  },
  "medium": {
    "id": 1,
    "title": "Painting",
    "picture": "https://..."
  },
  "privacy": false,
  "shopLink": "https://...",
  "price": 120.5,
  "imageUrl": "https://...",
  "thumbnailUrl": "https://...",
  "webpUrl": "https://...",
  "tags": ["abstract", "blue"],
  "likesCount": 42,
  "isLiked": false,
  "displayName": "...",
  "profileTypeName": "Artist",
  "profilePictureUrl": "https://...",
  "aspectRatio": 0.5625
}
```

Most fields on this shape are **camelCase** on the wire — see §14.3.

---

## 5. Curations

A curation is a curated set of artworks (a mini-exhibition).

### 5.1 Endpoints

| Endpoint | Method | Body | Returns |
|---|---|---|---|
| `GET /api/curations/?page=&size=` | GET | — | Paginated Curation |
| `GET /api/curations/{id}` | GET | — | Curation |
| `POST /api/curations/` | POST | create body below | Curation |
| `PUT /api/curations/{id}` | PUT | `{"artwork_ids":[1,2,3]}` or partial fields | Curation |
| `DELETE /api/curations/{id}` | DELETE | — | empty |
| `POST /api/curations/like` | POST | `{"curation_id": 7}` | empty |
| `DELETE /api/curations/{curationId}/like` | DELETE | — | empty |

**Create body:**

```json
{
  "title": "...",
  "description": "...",
  "privacy": false,
  "artwork_ids": [1, 2, 3]
}
```

### 5.2 Curation shape

```json
{
  "id": 7,
  "userId": 42,
  "title": "...",
  "description": "...",
  "privacy": false,
  "artworks": [ /* Artwork[] */ ],
  "author": { "id": 42, "username": "..." },
  "isExhibition": false,
  "isLiked": false,
  "likesCount": 12,
  "noOfScreens": 0,
  "screens": [{ "id": 1, "name": "Living Room", "key": "AB12CD34" }]
}
```

---

## 6. Search & discover

| Endpoint | Auth | Query | Returns |
|---|---|---|---|
| `GET /api/search` | Yes | `q`, `type`, filters | Search response |
| `GET /api/search/users?query=<text>` | Yes | `query` | Artist list |
| `GET /api/search/trending-tags` | Yes | — | `[String]` |
| `GET /api/feed/discover` | Yes | — | Discover feed |
| `GET /api/mediums/` | **None** | — | `[{id, title, picture}]` |

---

## 7. Messages (REST + WebSocket)

REST endpoints below; WS protocol in §12.

### 7.1 Endpoints overview

| Endpoint | Method | Use |
|---|---|---|
| `POST /api/messages/` | POST | Send a message |
| `GET /api/messages/with/{userId}?before=&limit=` | GET | Fetch thread with cursor pagination |
| `PATCH /api/messages/{messageId}` | PATCH | Edit text |
| `DELETE /api/messages/{messageId}` | DELETE | Delete message |
| `POST /api/messages/{messageId}/read` | POST | Mark as read |
| `GET /api/my/chatrooms` | GET | List chatroom previews |
| `DELETE /api/chatroom/with/{userId}` | DELETE | Delete chat (your side only) |
| `POST /api/chatroom_id` | POST | Resolve `user_id` → `chatroom_id` |

### 7.2 Send message

```
POST /api/messages/
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Body:**

```json
{
  "receiver_id": 42,
  "text": "Hey",
  "image_id": null,
  "client_message_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `receiver_id` | int | yes | Other user's profile ID |
| `text` | string | yes | Message text |
| `image_id` | int | no | Artwork ID for share-an-artwork messages |
| `client_message_id` | UUID string | **yes (recommended)** | **Lowercase**. Server dedups retries by this. Generate once per user-tap, reuse on retry |

**Response `data`:**

```json
{
  "message": { /* §7.4 message shape */ },
  "chatroomId": "<uuid>",
  "invitationStatus": true,
  "iBlocked": false,
  "theyBlocked": false,
  "isActive": true
}
```

Several fields here are **camelCase** on the wire — see §14.3.

**Errors:**

| HTTP | Cause |
|---|---|
| 403 | Mutual block, blocked-user, invite-limit exhausted, waiting-for-accept, not a participant. Do **not** sign out on these. |

### 7.3 Fetch thread (cursor pagination)

```
GET /api/messages/with/{userId}?before=2026-05-30T11:30:00.000Z&limit=50
```

| Query | Type | Notes |
|---|---|---|
| `before` | ISO 8601 string | Returns messages strictly older. Omit on first load. |
| `limit` | int | 1-200. Default returns newest within bound, ASC. |

**Response `data`:**

```json
{
  "invitationStatus": true,
  "messages": [ /* §7.4 message[] */ ],
  "iBlocked": false,
  "theyBlocked": false,
  "isActive": true,
  "nextCursor": "2026-05-29T10:15:00.000Z"
}
```

Use `nextCursor` (or null when reached the start) as the `before` for the next page request.

### 7.4 Message shape

```json
{
  "id": "<uuid>",
  "chatroomId": "<uuid>",
  "senderId": 42,
  "receiverId": 7,
  "text": "Hey",
  "imageId": null,
  "imageUrl": null,
  "isRead": false,
  "isDeleted": false,
  "isEdited": false,
  "createdAt": "2026-05-30T11:16:10.187Z",
  "editedAt": null,
  "mediaUserId": null,
  "mediaUserDisplayName": null,
  "mediaUserProfilePictureUrl": null,
  "artworkTitle": null
}
```

All fields above are **camelCase** on the wire.

### 7.5 Edit message

```
PATCH /api/messages/{messageId}
{"text": "corrected text"}
```

**Response:** the updated message (§7.4). Backend also fans out a `chat_edit` WS event to the other party.

**Edit window:** 15-minute server-side cap. After that, returns 400 with message containing `"Edit window expired"`. Hide the Edit affordance for stale messages; toast a friendly message on cap-hit.

**Other errors:**

| HTTP | Cause |
|---|---|
| 403 | Caller is not the sender |
| 404 | Message already deleted |

### 7.6 Delete message

```
DELETE /api/messages/{messageId}
```

Response: empty. Backend fans out `chat_delete` WS event. Set local `isDeleted = true` and render a "Message deleted" placeholder — do **not** remove the row.

### 7.7 Mark read

```
POST /api/messages/{messageId}/read
```

Response: empty. Backend fans out `chat_read` WS event to the original sender only.

### 7.8 Chatroom list

```
GET /api/my/chatrooms
```

Response: list of preview objects (other user info, last message, unread count).

### 7.9 Resolve chatroom by user_id

```
POST /api/chatroom_id
{"user_id": 42}
```

Response `data`:

```json
{
  "exists": true,
  "chatroom_id": "<uuid-or-null>",
  "invitation_status": true,
  "is_active": true,
  "i_blocked": false,
  "they_blocked": false,
  "remaining_invites": 5
}
```

### 7.10 Delete chat

```
DELETE /api/chatroom/with/{userId}
```

Wipes from your side only. Other party still sees their copy.

---

## 8. Notifications

### 8.1 Endpoints

| Endpoint | Method | Returns |
|---|---|---|
| `GET /api/notifications` | GET | notification list |
| `PATCH /api/notifications/{id}/read` | PATCH | empty |

### 8.2 List response

```json
{
  "success": true,
  "code": 200,
  "data": {
    "notifications": [ /* §8.3 */ ]
  }
}
```

### 8.3 Notification shape

```json
{
  "id": 2616,
  "type": "artwork_like",
  "message": "X liked your artwork 'Y'",
  "is_read": true,
  "timestamp": "2026-05-29T13:39:50.489176",
  "actor": {
    "id": 121,
    "name": "...",
    "profileImageUrl": null
  },
  "action": "liked",
  "target": {
    "id": 329,
    "type": "artwork",
    "title": "...",
    "thumbnailUrl": null
  }
}
```

**Known `type` values:** `artwork_like`, `artwork_comment`, `follow`, `chat_message`, `event_reminder_hour`, `event_reminder_day`, `curation_like`, `subscription_renewed`, `subscription_expired`.

**Forward-compat:** new values may appear at any time. Use an `UNKNOWN` enum fallback (e.g. with `@JsonClass(generateAdapter = true)` on Moshi or `@Serializable` with `@SerialName` + a sealed class default), render `message` text, and disable the tap target for unknowns.

---

## 9. Subscriptions

### 9.1 Google Play Billing

**Status:** the `verify-google-play` / `restore-google-play` endpoint pair does **not exist yet**. Coordinate with backend before wiring Play Billing.

For reference, the Apple counterpart exists:

```
POST /api/subscriptions/verify-apple
{
  "transaction_id": "<storekit-tx-id>",
  "original_transaction_id": "<storekit-orig-tx-id>",
  "product_id": "rinx.artist.pro.monthly",
  "signed_transaction": "<JWS payload>"
}
```

```
POST /api/subscriptions/restore-apple
{"original_transaction_id": "<storekit-orig-tx-id>"}
```

The Google Play equivalent will follow a similar shape but receive `purchase_token`, `subscription_id`, and `package_name`. Don't hard-code the contract yet.

### 9.2 Subscription shape (embedded in profile, returned by verify-apple)

```json
{
  "plan": "basic",
  "status": "active",
  "provider": "apple",
  "expiresAt": "2026-12-31T...",
  "renewsAt": "2026-12-31T...",
  "trialEndsAt": null
}
```

| Field | Type | Values |
|---|---|---|
| `plan` | string | `"basic"` \| `"artist_pro"` |
| `status` | string | `"active"` \| `"trialing"` \| `"cancelled"` \| `"past_due"` \| `"expired"` |
| `provider` | string \| null | `"apple"` \| `"google"` \| `"stripe"` \| null |
| `expiresAt`, `renewsAt`, `trialEndsAt` | ISO 8601 string \| null | camelCase, see §14.3 |

**Premium gating:** treat as premium when `plan == "artist_pro" && status in ["active", "trialing"]`. The backend also re-checks on every premium-only API call — client gating is for UX only, not security.

---

## 10. Upload (artwork creation)

Two-step flow.

### 10.1 Prepare upload

```
POST /api/prepare-upload/
Authorization: Bearer <accessToken>
```

| Body | none |
|---|---|

**Response `data`:**

```json
{
  "file_path": "artworks/abc-123.jpeg",
  "upload_url": "https://signed-bunny-url..."
}
```

### 10.2 Upload bytes (direct to CDN)

```
PUT <upload_url>
Content-Type: image/jpeg
<image bytes as the body>
```

**Important:** this PUT does **not** carry your API `Authorization` header. The signed URL is the credential. Use a plain HTTP client without your usual interceptors, or temporarily disable the auth interceptor for this request.

### 10.3 Finalize artwork

```
POST /api/artworks/
Authorization: Bearer <accessToken>
Content-Type: multipart/form-data
```

Form fields: `file_path` (from step 10.1), `title`, `description`, `tags[]`, `medium_id`, `shop_link`, `price`, `privacy`, `aspect_ratio`.

**Response `data`:** `{"id": 99, "image_url": "https://..."}`.

---

## 11. Analytics & tracking (fire-and-forget)

All require auth except `banners/track`. Errors should be logged, not surfaced to user.

| Endpoint | Method | Body / Query | Use |
|---|---|---|---|
| `POST /api/admin/click?type=&link=` | POST | query string | Track outbound link click |
| `POST /api/admin/duration?type=&link=&duration=` | POST | query string | Track time spent on external link |
| `POST /api/admin/view-history/record` | POST | `{"target_id":99,"target_type":"artwork"}` | Record artwork/curation view |
| `POST /api/banners/track` | POST (no auth) | `{"type":"view\|click","banner_id":51}` | Banner analytics |
| `POST /api/share` | POST | share-request body | Track share action |
| `POST /api/me/fcm-token` | POST | `{"fcm_token":"<fcm-token>"}` | Register/update push token (use FCM for Android) |

---

## 12. WebSocket protocol

### 12.1 Connection

```
wss://apifargate.rinx.com/ws
Authorization: Bearer <accessToken>
X-App-Version: <version>
```

OkHttp WebSocket / Ktor `WebSockets` plugin both work. Default OkHttp WS configuration is fine.

### 12.2 Server specifics

| | |
|---|---|
| Server library | uvicorn 0.34.2 + `websockets` |
| Server protocol ping interval | 20s |
| Server protocol pong timeout | 20s |
| Effective server close on silence | ~40s |
| AWS ALB idle timeout | 300s |
| `permessage-deflate` | advertised by default |
| Max frame size | 16 MB |
| Per-user connection cap | none — multi-device OK |
| Connect-time backlog fan-out | none — history is REST-only via `/api/messages/with/{userId}` |

### 12.3 Event types (server → client)

Every frame is JSON: `{"type": <string>, "data": <payload>}`.

| Type | When | `data` |
|---|---|---|
| `ready` | Server's auth-verified handshake. **Flip UI to "connected" only on this**, not on the URL upgrade completing. | none |
| `ping` | App-level heartbeat every ~30s. One-directional — do **not** send an app-level pong. | none |
| `chat` | New message arrived | full message object (§7.4) |
| `chat_edit` | Other party edited | `{message_id, chatroom_id, text, is_edited, edited_at}` |
| `chat_delete` | Other party deleted | `{message_id, chatroom_id, is_deleted}` |
| `chat_read` | A message you sent was marked read | `{message_id, chatroom_id, reader_id}` |
| `notification` | New notification (matches REST shape, §8.3) | notification object |

**Unknown event types:** silently drop. Don't error.

### 12.4 Close codes

| Code | Strategy |
|---|---|
| `4400` | Give up — ambiguous handshake (client bug) |
| `4401` | Refresh token once, reconnect with fresh bearer. Second 4401 → give up |
| `4403` | Give up — banned or surface-restricted (terminal) |
| `1000` / `1001` / `1006` / `1011` | Reconnect with backoff |
| `1008` (policy violation) | Treat like 4401 |
| `1002` / `1003` / `1007` / `1009` / `1010` | Give up — protocol errors |
| Unknown | Reconnect with backoff (conservative default) |

### 12.5 Heartbeat (client-side, required for production)

OkHttp does **not** send WebSocket protocol-level pings by default. Two layers of defense:

**Active (protocol-level ping):**

```kotlin
val client = OkHttpClient.Builder()
    .pingInterval(25, TimeUnit.SECONDS)   // sends ping frames on the connection
    .build()
```

If the server doesn't pong within ~10s, OkHttp closes with code 1011/1006 → trip the reconnect strategy.

**Passive (inbound-silence watchdog):**

- Track `lastInboundAt` (epoch ms), update on every inbound frame including the app-level `ping`.
- Every 15s, if `now - lastInboundAt > 45_000` → force reconnect.

**Cancel both on app background.** Suspended timers wake up and see the entire background duration as silence, racing with the reconnect path. Cancel on `ON_STOP`; restart after the next `"ready"` event post-foreground reconnect.

### 12.6 Reconnect strategy

- Exponential backoff, capped at 60s, with jitter
- Max 10 attempts before exposing a "Retry" UI affordance
- Bind reconnect to network reachability — pause when offline, dial when path returns with a freshly-fetched access token (the previous one may have expired during the offline window)
- Don't reconnect while app is backgrounded — Android suspends WS tasks anyway. Resume on foreground

### 12.7 Subscriber queue bounds

Whatever stream / channel / flow you fan-out into must be bounded. Recommended: 100 events, drop-oldest policy. Unbounded queues leak memory linearly when a subscriber is paused (suspended UI, background fragment).

Kotlin `Channel(capacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)` does exactly this.

---

## 13. Common patterns

### 13.1 Pagination

- Standard: `?page=1&size=10` (1-indexed). Response wraps `{items, total, page, size}`.
- Cursor (chat only): `?before=<iso8601>&limit=<int>`. Response wraps `{messages, nextCursor, ...}`.

### 13.2 Multipart upload

- `Content-Type: multipart/form-data; boundary=...`
- Used by: `POST /api/profile`, `PUT /api/profile/update`, `POST /api/artworks/`.

OkHttp:

```kotlin
val body = MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("username", "jane")
    .addFormDataPart("profile_picture", "pic.jpg",
        file.asRequestBody("image/jpeg".toMediaType()))
    .build()
```

### 13.3 Snake-case ↔ camelCase exceptions

Most fields convert with a global snake-case strategy. These specific fields are **camelCase on the wire** — annotate explicitly:

- `chatroomId` (in send-message response and public profile)
- `iBlocked`, `theyBlocked`, `invitationStatus`, `isActive` (chat responses)
- `nextCursor` (chat thread pagination)
- All Message fields: `chatroomId`, `senderId`, `receiverId`, `imageId`, `imageUrl`, `isRead`, `isDeleted`, `isEdited`, `createdAt`, `editedAt`, `mediaUserId`, `mediaUserDisplayName`, `mediaUserProfilePictureUrl`, `artworkTitle`
- Artwork fields: `userId`, `shopLink`, `imageUrl`, `thumbnailUrl`, `webpUrl`, `likesCount`, `isLiked`, `displayName`, `profileTypeName`, `profilePictureUrl`, `aspectRatio`
- Curation fields: `userId`, `isExhibition`, `isLiked`, `likesCount`, `noOfScreens`
- Subscription fields: `expiresAt`, `renewsAt`, `trialEndsAt`
- Notification nested fields: `profileImageUrl`, `thumbnailUrl`

Moshi example:

```kotlin
@JsonClass(generateAdapter = true)
data class Message(
    val id: String,
    @Json(name = "chatroomId") val chatroomId: String,
    @Json(name = "senderId") val senderId: Int,
    ...
)
```

### 13.4 Reachability pre-flight

Use `ConnectivityManager.getNetworkCapabilities()` before every request and fail fast offline rather than waiting on the HTTP timeout (typically 30s). Saves user time and avoids burning retry attempts.

### 13.5 Idempotent GET auto-retry

GETs only: auto-retry on timeout / connection-lost / 502 / 503 / 504 with exponential backoff + jitter. POST/PUT/PATCH/DELETE should **not** auto-retry — they're not safe.

### 13.6 Token preflight (mandatory)

Before every authenticated request, check whether the cached access token is within **60s of expiry**. If yes, refresh proactively via the single-flight coordinator (§1.5) **before** sending the request. Without this, cold-launch parallel calls all hit 401 with the same stale token and trigger racing refreshes → false sign-out cascade.

**Plus: don't sign out on data-endpoint 401s.** The only legitimate sign-out signals are:

1. `/refresh` returning `refresh_invalid` or `refresh_reused`
2. 403 with body `"Not authenticated"` or `"Your account has been banned."`

A data-endpoint 401 after a successful refresh is the token-rotation race, not a real invalidation — retry the call with the fresh token.

---

## 14. Error handling

### 14.1 Three error envelope shapes

Try parsing in this order. The first that matches wins.

**Business-rule (typed):**

```json
{ "detail": { "code": "invite_invalid", "message": "...", "retry_after": 60 } }
```

Map `code` to specific UI copy. `retry_after` (seconds) appears on `otp_locked` and `rate_limited`.

**Pydantic validation:**

```json
{
  "detail": [
    { "type": "missing", "loc": ["body", "email"], "msg": "Field required" }
  ]
}
```

Means the client built the body wrong. Surface as a developer-actionable diagnostic, **not** a user-facing toast.

**Bare string:**

```json
{ "detail": "Not authenticated" }
```

Three documented values:
- `"Not authenticated"` → 403, missing/wrong `Authorization` header → sign out
- `"Your account has been banned."` → 403 → sign out + surface ban reason
- `"Upgrade plan to use profile link"` → 400, profile-link plan gate

### 14.2 Known business-rule error codes

| Code | When | Recovery |
|---|---|---|
| `otp_invalid` | Wrong OTP entered | Toast, let user retry |
| `otp_expired` | OTP > 5 min old | Resend OTP |
| `otp_locked` | 5 wrong attempts | Cooldown UI with `retry_after` |
| `rate_limited` | Too many requests | Cooldown with `retry_after` |
| `refresh_invalid` | Refresh token rejected | Wipe + sign out |
| `refresh_reused` | Already-rotated refresh token presented | Wipe + sign out with security message |
| `session_expired` | Generic 401 | Wipe + sign out |
| `invalid_contact` | Phone/email format invalid | Inline validation error |
| `contact_in_use` | Email/phone already on another account | Inline error |
| `invite_invalid` | Code didn't match any source | Inline error on invite screen |

### 14.3 HTTP status conventions

| Code | Treatment |
|---|---|
| 200, 201 | Success |
| 204 | Success, no body |
| 400 | Client error — surface message |
| 401 | Auth — only sign out on the refresh path (§13.6) |
| 403 | `"Not authenticated"` → sign out; otherwise surface error, stay signed in |
| 404 | Resource not found |
| 409 | Conflict (rare) |
| 422 | Validation error — see Pydantic envelope |
| 429 | Rate limited |
| 500-504 | Server error — auto-retry idempotent GETs; surface to user otherwise |

---

## 15. Implementation checklist

For each feature:

1. **Identify the endpoint(s)** from the relevant section above
2. **Match the JSON shape** — snake_case on the wire unless flagged in §13.3
3. **Wrap responses** in the standard envelope `{success, code, message, data}`
4. **For auth-required calls**, attach `Authorization: Bearer <accessToken>` AFTER the preflight expiry check (§13.6)
5. **Handle the three error envelope shapes** (§14.1) — don't blanket-sign-out on data 401s
6. **For paginated endpoints**, use 1-indexed `page` + `size` (chat uses cursor)
7. **For real-time features**, subscribe to the relevant WS event stream (§12.3) with a bounded buffer (§12.7)
8. **Drop telemetry breadcrumbs** at lifecycle points (auth events, WS connect/disconnect, key user actions) so cross-platform crash reports read consistently with iOS

---

## Appendix A — Recent backend changes worth knowing

These shipped recently and are reflected in the contracts above; flagging in case you're cross-referencing older docs.

- **Agent referral codes now accepted by the invite-code screen** (PR #85, 2026-06-01). `/api/verify-invite` returns a new optional `code_type` field. The OTP signup endpoints accept agent codes through the existing `invite_code` body field with no contract change — only behavior expanded. See §2.1.
- **`UserProfile.invitation_code` is nullable** for agent users (migration `ac01`, PR #80). Decoder must accept `null`. See §3.1.
- **Refresh-token storage is HMAC-SHA256** keyed by `NATIVE_REFRESH_HMAC_SECRET` (PR #76). One-time effect on stage cutover: all pre-cutover refresh tokens became invalid. New tokens issued after the cutover are stable.
- **WebSocket close codes 4401 / 4403** are now emitted on an established socket (after `accept()`) rather than pre-handshake, so URLSession-equivalents (and OkHttp) see them as real close frames. See §12.4.
- **Chat 401 → 403 conversion** (PR #66, PR #68). Action-denied paths (mutual block, invite limit, blocked-user, not-a-participant, wrong sender on edit/delete) now return 403 instead of 401. Do not sign out on these.
- **Message edit window:** 15 minutes server-side cap (PR #66). Error message contains `"Edit window expired"`.
- **`POST /api/invites/` is removed.** The old "create me an invite link" endpoint. If any legacy code still calls it, it returns 404 now.

---

**Last reviewed:** 2026-06-01. If anything here drifts from real backend behavior, ping the backend team — happy to update the doc rather than have you guess.
