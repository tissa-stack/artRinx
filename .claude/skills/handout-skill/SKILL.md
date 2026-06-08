# artRINX — Android Developer Handout

**Source:** iOS reference implementation (`RINX-V_2-iOS`, branch `main`)
**Backend base URL:** see `Secrets.{debug,release}.xcconfig` → `API` (e.g. `https://apifargate.rinx.com/api/`)
**WebSocket base URL:** `WS_URL` from same xcconfig
**Auth:** Native HS256 JWT (Bearer token in `Authorization` header). 401 → refresh once via `/auth/native/refresh`; if that fails → sign out.

This document maps every screen to its API calls + UX behaviors. The Chat screen has a dedicated section at the bottom because its state machine is the most intricate.

---

## Changes since 2026-06-08 (pricing & limits revamp — please skim before reading the rest)

Backend shipped a pricing/limits revamp. Anywhere in this doc that references plan pricing, feature copy, or invitation counters has been updated; the highlights:

1. **`remaining_chat_invites` (and `remaining_invites` on chat surfaces) wire field semantic changed.** Field name unchanged for back-compat. Number on the wire used to mean "peer-share invites left" (5/5/25 by tier); now means **"message requests left — new chats you can start this month"** (15/25/25 by tier). Backend confirmed (Q1, Option A): both `/profile.remaining_chat_invites` and the chat-surface endpoints all return the new meaning today. Just update the user-facing label.

2. **`/profile` carries BOTH counters today, under back-compat names:**
    - `total_user_invites` / `remaining_invites` → **peer-share cap** (5/5/25), the codes you give to friends
    - `remaining_chat_invites` → **message-request cap** (15/25/25), starting new conversations

3. **Backend ships additive nested objects this week** so you don't have to remember which old name means what:
   ```json
   "peer_invites":    {"monthly_cap": 5,  "remaining": 3},
   "message_requests": {"monthly_cap": 15, "remaining": 12}
   ```
   Both old and new names will be returned side-by-side; migrate at your own pace.

4. **"Unlimited messages within active chats" is still true.** The 15/25/25 cap is **only on STARTING a new conversation** (when a new chatroom is created). Once a chat is active, messages within it are unlimited on every tier.

5. **Pricing changes:** Artist Pro $9/mo → **$14.99/mo** with a **2-month Apple intro trial** (configured in App Store Connect; Google Play Billing has the equivalent on the Android side — see Subscriptions section). Gallery stays $99/mo Stripe-only with **no trial**.

6. **Tier matrix:**

   |                              | Artworks (active) | Peer invites / mo | New chats / mo | Profile + shop link | Events | City push |
      |---|---|---|---|---|---|---|
   | Basic (Art Curious / Collector / Artist free) | 10 | 5 | 15 | no | no | no |
   | Artist Pro ($14.99 + 2-month intro trial) | 99 | 5 | 25 | yes | no | no |
   | Gallery ($99/mo, web-only Stripe, no trial) | 99 | 25 | 25 | yes | yes | yes |

7. **iOS-side copy renamed** "X invitations left" → "X new chats this month" (and the limit-reached toast clarifies "Unlimited messages within your active chats"). Android should follow the same wording for consistency.

8. **City push** for Gallery event creation is now **exact city match only** (was previously falling back to state + country). FCM payload shape unchanged.

9. **Provider enum:** `subscription.provider` is now confirmed as `"apple" | "stripe" | null` (null = free / no subscription row). Stripe-billed Gallery returns `plan: "gallery"`, `status: "active"`, `provider: "stripe"`.

---

## Table of Contents
1. [Auth & Session](#auth--session)
2. [Onboarding / Invite / Signup](#onboarding--invite--signup)
3. [Profile Setup Wizard](#profile-setup-wizard)
4. [Home / Discover](#home--discover)
5. [Search](#search)
6. [Upload](#upload)
7. [Notifications](#notifications)
8. [Profile (Self + Public)](#profile-self--public)
9. [Settings](#settings)
10. [Subscriptions / Plans](#subscriptions--plans)
11. [Push Notifications (FCM)](#push-notifications-fcm)
12. [WebSocket](#websocket)
13. [**Chat — full state machine**](#chat--full-state-machine) ⭐

---

## Auth & Session

| Screen | Endpoint | Method | Purpose |
|---|---|---|---|
| InviteView | `verify-invite` (form-data `invitation_code`) | POST | Validate invite/referral code BEFORE allowing signup. Returns `code_type` (`peer`/`admin`/`agent`). |
| RegisterWithPhone / RegisterWithEmail | `auth/native/request-otp` | POST | Send OTP. Body: `phone` OR `email`, `mode: "signup"`, `invite_code` (required for signup), consents nested. **Do NOT send `referral_code`.** |
| LoginWithPhone / EmailOtpSignIn | `auth/native/request-otp` | POST | Same as above but `mode: "signin"`, no invite_code. |
| OtpVerification / SignupEmailOtpVerification | `auth/native/verify-otp` | POST | Verify OTP. Body: `phone` OR `email`, `code`, `mode`, `invite_code` (signup), `consents` (signup). Returns `{access_token, refresh_token, user}`. |
| Background (every screen) | `auth/native/refresh` | POST | Body: `{refresh_token}`. Proactively fired `accessTokenExpiresAt - 60s` and on app foreground; reactively on 401. Single-flight: 20 concurrent expired calls = 1 refresh round-trip. |
| Settings → Logout | `auth/native/logout` | POST | Revokes refresh token server-side. |
| Settings → Sign out everywhere | `auth/native/sign-out-everywhere` | POST | Revokes ALL refresh tokens for the user. |

### Error code mapping (native auth)
Backend returns `{detail: {code: "...", message: "...", retry_after?: int}}`. Important codes:
- `invalid_otp`, `otp_expired`, `otp_locked` (with `retry_after`)
- `rate_limited` (with `retry_after`, `message`)
- `invite_or_referral_required`, `invite_invalid`, `referral_invalid`
- `already_registered`, `no_account_found`
- `refresh_invalid`, `refresh_reused` (security: wipe tokens + sign out)
- `session_expired`, `identity_disabled`, `not_allowed_on_this_surface`

---

## Onboarding / Invite / Signup

### OnboardingView
- First-launch only. No API calls. Carousel of value props.
- After: navigates to **InviteView**.

### InviteView ⭐ critical
**Purpose:** single text field for any invite/referral code. Backend disambiguates type.

```
User types code → tap Continue
  → POST /verify-invite (form-data invitation_code=<code>)
  → Response: {data: {code_type: "peer" | "admin" | "agent", inviter_id?, remaining_invites?, month}}
  → BRANCH on code_type:
      "peer"   → write @AppStorage("verifiedInviteCode") = code; navigate to Signup
      "admin"  → same as peer
      "agent"  → show "Complete on web" modal: "Gallery signups are
                 completed on the web. Visit artrinx.com/gallery"
                 Buttons: [Open artrinx.com/gallery] [Cancel]
                 DO NOT navigate to OTP. DO NOT call request-otp.
  → 400 invalid → show toast with response.message
```

**Why agent codes get redirected:** Gallery accounts are web-purchase-only via Stripe. Mobile's request-otp schema rejects Gallery profile creation with `gallery_on_web_only` 400.

**Deep links pre-fill this field:**
- `rinxart://invite/<CODE>` (custom scheme)
- `https://www.artrinx.com/invite/<CODE>` (universal link)

**No deep link for Gallery referrals.** Users get those via email/SMS as a code to type in.

### WaitlistView
- Shown when user doesn't have an invite code yet.
- API: `POST /waitlist` with `{email, phone_number, profile_type_id, first_name, accepted_terms, sms_notifications_opt_in, instagram_handle?, device_type, source_tag?}`

---

## Profile Setup Wizard

5-step linear wizard shown immediately after signup OTP succeeds. State held in one ViewModel; backend POST fires from the **mediums step**, not the final plan step.

| Step | Screen | API | Notes |
|---|---|---|---|
| 1 | ProfileTitleView | — | Pick role: **Artist / Collector / Art Curious only**. Gallery is NOT shown (web-only acquisition). |
| 2 | ProfileDetailsView | `GET profile/username-check?username=...` | Debounced 500ms, on every keystroke ≥5 chars. |
| 3 | PersonalInfoView | — | Country/state/city pickers fed from local LocationService bundle. |
| 4 | PreferredMediumsView | `GET mediums/` | Fetch + display medium catalogue. User picks exactly 3. |
| 4 → Continue | (fires here, not step 5) | `POST profile` (multipart/form-data) | Body: `username, full_name, display_name, profile_type_id, age, country, state, city, accepted_terms, sms_2fa_consent, account_notification_sms, marketing_sms_consent, medium_ids` (comma-joined string), optional `bio`, optional `invitation_code`, optional `profile_picture` file. **DO NOT send `referral_code`** — backend 422s on mobile. |
| 5 | ProfileTitleAndPlanStepView | — | **Informational only.** No API calls. Shows the plans available for the picked role. Exit button = "Explore artRINX" which triggers `AuthRepository.refreshSession()` to pull the updated user with `profile_completed: true`. |

**Cold-launch recovery:** if user force-quits between step 4 success and step 5 exit, on next launch the wizard re-routes them to step 4. Tapping Continue re-fires POST profile — backend returns 409 "already exists". iOS treats 409 as success and advances. Android must do the same.

---

## Home / Discover

### HomeView (Home tab root)
- **`GET feed/discover?page=N&size=10`** — paginated discover feed (artworks + banners + curations interleaved).
- **`GET search/trending-tags`** — for the top tag carousel.
- **`GET admin/view-history/record`** (POST? check) — fires when art appears on screen long enough (impressions analytics).

### PublicArtDetailView
- **`GET artworks/{id}`** — artwork detail.
- **`GET artworks/{id}/similar`** — similar art carousel.
- **`POST artworks/like` / `DELETE artworks/{id}/like`** — like/unlike. Optimistic UI; on failure → enqueue in `LiveMutationQueue` (SwiftData), drain on network resume.
- **`POST follow` / `POST users/{userId}/unfollow`** — follow the artist.
- **`POST share`** — share analytics.

### PublicCurationDetailView
- **`GET curations/{id}`**
- **`POST curations/like` / `DELETE curations/{id}/like`**
- Same follow/share endpoints.

### PublicProfileView
- **`GET profile/{userId}/public/info`** — header info. **Returns:** `username, display_name, profile_type_name, profile_picture_url, bio, profile_link, mediums, artwork_count, curation_count, follower_count, following_count, is_following, chatroom_id, invitation_status, i_blocked, they_blocked, can_message, block_reason`
- **`GET profile/{userId}/public/artworks?page=N&size=10`**
- **`GET profile/{userId}/public/curations?page=N&size=10`**
- **`POST block` / `POST unblock`** — block toggle.
- **`POST report-artwork` / `POST report-message`** — reporting.

---

## Search

### SearchView
- **`GET search?query=...&type=...`** — combined search.
- **`GET search/users?query=...`** — user-only.
- **`GET search/trending-tags`** — empty state.

---

## Upload

### UploadHomeView (Upload tab root)
- No API calls on tab open. Two cards: "Upload Art" + "New Collection". Plus quota card + progress card.

### Upload tap handler — branch by role × plan ⭐
```
isPaid = profile.subscription.is_paid
role   = profile.profile_type_name

if isPaid:
    if artwork_count >= max_uploads:
        → "Upload limit reached" alert
    else:
        → open photo picker
elif role == "Gallery":
    → "Gallery plan required" alert (artrinx.com)
    Body: "Uploading art requires an active Gallery plan, which is
    managed on artrinx.com. Visit our website to view or change
    your plan."
    Buttons: [Open Website] [Not Now]
    DO NOT trigger any in-app purchase / Restore flow.
else:
    → "Upgrade required" alert (in-app Artist Pro purchase)
```

### UploadView (after image selected)
- **`POST prepare-upload/`** (multipart with image bytes) — triggers AWS Rekognition tagging + returns `{filepath, upload_url, rekognition_tags, aspect_ratio}`.
- **PUT to `upload_url`** — direct upload of image bytes to S3/CDN (signed URL).
- **`POST artworks/`** (multipart) — create artwork record with `title, description, artist_id, medium_id, tags, shop_link (premium only), price (only when shop_link present), privacy, filepath, aspect_ratio`.

### Field gating
- **Shop link** — visible/locked based on `ShopLinkVisibility` enum:
    - Artist Free → locked with IAP paywall prompt
    - Artist Pro → unlocked
    - Gallery active sub → unlocked (no Apple IAP)
    - Gallery inactive → hidden
    - Other roles → hidden
- **Price** — only shown when `shop_link` is non-empty.

---

## Notifications

### NotificationView (Notifications tab → Notifications segment)
- **`GET notifications`** — list. Returns `[{id, type, message, is_read, timestamp, actor, action?, target}]`.
- **`PATCH notifications/{id}/read`** — mark single notification as read (fires on row tap).
- Server-driven enum fallback: unknown `type` / `action` values render as plain text with no tap target.

### MessagesView (Notifications tab → Messages segment)
- **`GET my/chatrooms`** — chat list. Returns `[{chatroom_id, invitation_status, user, last_message?, unread_count, is_active, remaining_invites, can_message, block_reason}]`.
- Pull-to-refresh re-fires.
- Row tap → navigate to **UserChatView** (see Chat section).

---

## Profile (Self + Public)

### ProfileView (Profile tab root — your own profile)
- **`GET profile`** — your full profile (cached + refreshed on appear).
- **`GET artworks/by-artist-id/{artistId}?page=N&size=10`** — your art grid.
- **`GET curations/?owner_id={id}&page=N&size=10`** — your curations.
- **`GET artworks/liked`** — your liked feed.

### Edit Profile
- **`PATCH profile/update`** (multipart) — update fields. Optional `profile_picture` file.

---

## Settings

### SettingsView
- **`GET followers?page=N`** / **`GET followed-users?page=N`** — followers/following lists.
- **`GET blocked/users`** — blocked users management.
- **`POST users/{userId}/unfollow`**, **`POST block`**, **`DELETE block`** — relationship actions.
- **`POST feedbacks/`** — submit feedback.
- **`DELETE user/delete-me`** — delete account.

### Change Role (Settings → Change Role)
- **`PATCH profile/update`** (form-data `profile_type_id=<id>`) — save new role.
- **Visibility rule:** the row is HIDDEN in Settings when:
    - User's role is Gallery (Gallery is web-managed), OR
    - User has any active paid subscription (`subscription.is_paid == true`) — paid plans are tied to a role server-side.
- **Picker rule:** shows all 3 (Artist, Collector, Art Curious). User's current role is pre-selected; Save stays disabled until a different role is picked.

### Change Email / Add Email / Change Phone / Add Phone
- **`POST auth/native/start-add-contact` / `POST auth/native/start-change-contact`** — initiate. Sends OTP to new contact.
- **`POST auth/native/confirm-add-contact` / `POST auth/native/confirm-change-contact`** — verify OTP, apply change.
- Phone change precondition: backend may return `email_required_before_phone_change` 400 → show modal "Please add an email before changing your phone".

---

## Subscriptions / Plans

### SelectPlanView (Settings → Select plan)
- **No API calls on appear** — reads `profile.subscription` from app state.
- IAP purchase flow for Artist Pro:
    - iOS: StoreKit `Product.purchase()` → on verified transaction → **`POST subscriptions/verify-apple`** with `{transaction_id, original_transaction_id, jws_representation}` (the JWS payload from `Transaction.jsonRepresentation`).
    - Android: Google Play Billing equivalent. Expected backend endpoint: `subscriptions/verify-google` (confirm with backend; not currently present in iOS code). Send the purchase token + product ID + signature.
- Restore Purchases tap:
    - iOS: StoreKit `AppStore.sync()` → if entitlement found, **`POST subscriptions/restore-apple`**.
    - Android: Play Billing `queryPurchasesAsync()` → call the Android equivalent.

### Plan card content (2026-06 pricing revamp)

iOS keeps all plan copy in a single source-of-truth enum (`PlanType.swift`). Android should mirror. Current strings on each card:

**Basic** — Free — "For everyone"
- Browse and like art
- Follow and share profiles
- Upload 10 artworks
- Invite 5 friends per month
- Start up to 15 new chats per month
- Unlimited messages within active chats

**Artist Pro** — $14.99/mo — "For artists"
- *(+ a small gift-icon "N months free trial" line is rendered dynamically below the subtitle when the StoreKit product reports an introductory offer with payment mode = free trial. iOS reads `product.subscription?.introductoryOffer?.period` and formats. Android does the equivalent via Play Billing's `SubscriptionOfferDetails.pricingPhases`.)*
- Browse and like art
- Follow and share profiles
- Upload 99 artworks
- Profile link
- Shop art link
- Invite 5 friends per month
- Start up to 25 new chats per month
- Unlimited messages within active chats

**Gallery** — $99/mo — "For exhibition spaces & curators"
- Browse and like art
- Follow and share profiles
- Upload 99 artworks
- Profile link
- Shop art link
- Invite 25 friends per month
- Start up to 25 new chats per month
- Unlimited messages within active chats
- Create events  (web only)
- City-targeted push notifications for events  (web only)

### Plan card CTA behavior ⭐
| Plan | Card CTA |
|---|---|
| Basic (when user is on Basic) | "Current Plan" pill — no button |
| Artist Pro (when user is Artist Free) | Colored "Upgrade" button → IAP purchase. Auto-renewable subscription disclosure (price + "auto-renews monthly until cancelled" + Terms/Privacy links) renders below the button — required by App Store 3.1.3 / Play equivalent. |
| Artist Pro (when user is Artist Pro) | "Current Plan" pill |
| Gallery (any state) | **"Managed on artrinx.com"** text-only footer — NO button, NO purchase CTA. Apple anti-steering (3.1.1) / Play equivalent. |

### Paywall footer (Restore Purchases + Manage Subscription) — visibility matrix
| Role + state | Footer renders? |
|---|---|
| Artist Free | Yes — Restore only |
| Artist Pro (Apple-subscribed) | Yes — Manage + Restore |
| Collector / Art Curious | **No** — no IAP path |
| Gallery (any state, even subscribed) | **No** — Stripe billing, no IAP to manage |

### Dynamic intro-trial line (Artist Pro only)

Source of truth is **App Store Connect** (or Google Play Console on Android). Don't hardcode "2 months free" in code — read the offer dynamically and format. iOS implementation in `PlanCardView.introTrialLine`:
- Returns `nil` when no offer is configured, when the product hasn't finished loading, or when offer mode isn't free trial.
- When present, formats as `"N months free trial"` (or days/weeks/years per offer unit, singularizing for value == 1).
- Renders a small gift-icon line beneath the subtitle.

Backend Q3 rationale: ASC/Play is the canonical place for offer config. Product may A/B-test trial length later; dynamic copy adapts without an app update.

### Per-tier message-request limit (NEW)

The chat compose banner and the post-send success copy both read `profile.remaining_chat_invites` (server-driven). iOS rendered strings:

| Surface | String |
|---|---|
| Chat compose banner (when chatroom doesn't exist yet) | "You have N new chats this month" |
| Post-send success view | "Your message is on its way to NAME — you have N new chats left this month." |
| Quota-exhausted toast | "You've reached your monthly limit for starting new chats. Unlimited messages within your active chats." |

These replaced the old "X invitations left" / "Your invitation is in NAME's inbox" / "chat invite limit" copy. Android please use the same phrasing.

---

## Push Notifications (FCM)

### Token registration
- `Firebase.Messaging` delegate → on token refresh → **`POST me/fcm-token`** with `{fcm_token, platform: "ios" | "android", device_id}`.
- Re-fires on user change (sign in/out) to bind the token to the current user.

### Payload shape (engagement notifications: likes, follows, shares)
```json
{
  "notification": {"title": "...", "body": "..."},
  "data": {
    "type": "curation_like",
    "actor_id": "208",
    "resource_type": "curation",
    "resource_id": "297",
    "route": "/curations/297",
    "url": "https://app.artrinx.com/curations/297",
    "collapse_id": "curation_297",
    "idempotency_key": "curation_like:208:127:curation:297",
    "notification_id": "2714",
    "aggregate_count": "1"
  },
  "android": {"collapse_key": "curation_297"},
  "apns": {
    "headers": {"apns-priority": "10", "apns-collapse-id": "curation_297"},
    "payload": {"aps": {"sound": "default", "thread-id": "curation_297"}}
  }
}
```

### Tap handler behavior
1. Open `data.url` as a deep link if present.
2. If `data.notification_id` is present: **`PATCH notifications/{id}/read`** (fire-and-forget) so badge clears instantly.
3. Special case: `data.kind == "gallery_enterprise_notice"` — open app to home only, no URL navigation, no banner, no CTA (App Store anti-steering).

### Server-side behavior (no client work, just FYI)
- Toggle-spam dedup: like→unlike→like in 30 min = 1 push for likes, 30 min shares, 24 hr follows.
- Presence-skip: if user has a live WebSocket connection, push is skipped (they get realtime update in-app).
- `aggregate_count` may exceed "1" in future when aggregation ships. **Don't hardcode `=== 1`.**

### Quiet hours / DND
- Not implemented client-side. Relies on iOS Focus modes. Android can rely on Do Not Disturb similarly.

---

## WebSocket

**Endpoint:** `${WS_URL}` with `?token=<accessToken>` query param. Token-authenticated; on `close code 1008` (policyViolation) → refresh token + reconnect.

**Reconnection:** exponential backoff capped at 60s. Subscribes to network reachability; auto-disconnects on path loss, auto-reconnects on path restore with a freshly-fetched token.

**Event envelope:** `{type: string, data: object}` — `type` switches handling.

### Event types
| `type` | Payload | What iOS does |
|---|---|---|
| `ready` | — | Connection established. |
| `notification` | NotificationModel | Insert at top of notifications feed; update unread badge. |
| `chat` | MessageModel | Append to active chat (if `chatRoomId` matches) or update chat list row. |
| `chat_edit` | MessageModel | In-place update of message (text + edited_at + is_edited). Monotonic guard: reject if local `edited_at >= event.edited_at`. |
| `chat_delete` | `{id, chatroom_id}` | Mark message `is_deleted = true` (soft delete; bubble renders "Message deleted"). |
| `chat_read` | `{message_id, chatroom_id}` | Mark sender's message as read on the sender's side (read receipt). |

On reconnect after offline window: call **`GET /messages/with/{userId}?limit=50`** as backfill (server doesn't replay events).

---

# Chat — full state machine ⭐

The most complex screen. Read this section carefully.

## Three entry points to UserChatView

1. **MessagesView tap on chatroom row** → `UserChatView(otherUserId: userId)`
2. **PublicProfileView tap on "Message" button** → same constructor
3. **Push notification tap** with URL `/chat/<userId>` → router pushes UserChatView

## On view appear

```
1. GET /messages/with/{otherUserId}?limit=50
   Response data (UserChat shape):
   {
     "messages": [MessageModel, ...],
     "invitation_status": bool,
     "i_blocked": bool,
     "they_blocked": bool,
     "is_active": bool,
     "next_cursor": ISO8601 | null,
     "chatroom_id": string | null,
     "can_message": bool,
     "block_reason": "i_blocked" | "they_blocked" | "invite_pending" | "rate_limited" | null,
     "remaining_invites": int
   }

2. WebSocket subscription is already active for the user globally;
   filter `chat` / `chat_edit` / `chat_delete` / `chat_read` events
   by chatroom_id match.

3. As messages render and ENTER VIEWPORT:
   POST /messages/{messageId}/read  (only for received unread messages)
```

## Pagination (scroll to top)
- Top of LazyList triggers loadEarlier:
    - **`GET /messages/with/{userId}?before={oldestVisibleCreatedAt}&limit=50`**
    - Prepend results, dedup by id.
    - When `next_cursor` is null in response, stop showing the "load earlier" spinner.

## Reconnect backfill
- On WS `ready` event after reconnect, fire **`GET /messages/with/{userId}?limit=50`**.
- Merge-by-id: for each server message:
    - Not in local store → insert by `created_at`
    - Already in local store → update mutable fields (text, is_edited, is_deleted, edited_at, is_read)

## Send message
```
POST /messages/
Body: {
  receiver_id: int,
  text: string,
  image_id?: int,        // for image messages
  client_message_id: UUID  // CALLER-OWNED for backend dedup
}
Response data (SendMessageResponse):
{
  message: MessageModel,
  chat_room_id: string,
  invitation_status: bool,
  is_blocked: bool,
  they_blocked_me: bool,
  is_active: bool
}

→ Append message to local store
→ Update local state from response
→ Clear text input
```

**`client_message_id` retry contract:** if you ever wire a retry path (timeout, network drop), pass the SAME UUID. Backend dedups → no duplicate message. Generating a fresh UUID per retry creates duplicates.

## Edit message
- Allowed only on YOUR OWN non-deleted text messages, **within 15 minutes of `created_at`**.
- After 15min, backend rejects with 400 "Edit window expired". iOS hides the Edit menu item past this; show a toast if user somehow taps stale Edit.
- **`PATCH /messages/{messageId}`** with `{text: newText}`.
- Backend fans out a `chat_edit` WS event to all participants; recipient's UI updates in place.

## Delete message
- Allowed on YOUR OWN messages.
- **`DELETE /messages/{messageId}`** — soft delete server-side.
- Local bubble shows "Message deleted" placeholder (italic, dimmed).
- Backend fans out `chat_delete` WS event.

## Delete entire chat
- Menu → Delete Chat → **`DELETE /chatroom/with/{otherUserId}`**
- Pops UserChatView, removes row from MessagesView.

## Block / Unblock from chat
- Menu → Block Profile → **`POST block`** with `{blocked_user_id: int}` → local `is_blocked = true`.
- Menu → Unblock → **`DELETE block`** with `{blocked_user_id: int}` → local `is_blocked = false`.

---

## Chat compose field — gating ⭐⭐⭐

This is the single most important behavior to get right. The compose field at the bottom of UserChatView has 4 possible disabled states + 1 active state. Each renders different placeholder text AND different informational text above it.

### State variables (from UserChat response)
- `i_blocked` — current user has blocked the other person
- `they_blocked` — the other person has blocked the current user
- `invitation_status` — true = invitation accepted; false = pending/unsent
- `is_active` — true = both users have sent at least one message
- `messages` — list of messages so far
- `chatroom_id` — null if no chatroom exists yet

### Decision tree

```
1. i_blocked == true
   → INFO TEXT: "You blocked this user. Unblock to send messages."
   → FIELD: disabled. Placeholder: "Messaging disabled"
   → SEND BUTTON: hidden / disabled

2. else if they_blocked == true
   → INFO TEXT: "You can't send messages to this user."
   → FIELD: disabled. Placeholder: "Messaging disabled"
   → SEND BUTTON: hidden / disabled

3. else if messages.isEmpty AND chatroom_id == null/empty
   (= no prior chat; this will be the first message — the "invite")
   → INFO TEXT (above field, in a card):
        Icon: invite symbol
        Title: "Invite <userName> to chat"
        Body: "This message will act as your invitation to chat for
               the first time with this profile. You can only send
               one message in this invite until they accept."
        Footnote (right-aligned): "You have <N> new chats this month"
        (N comes from profile.remaining_chat_invites — new semantic
        per 2026-06 revamp; see top of doc)
   → FIELD: enabled. Placeholder: "Write Your Message"
   → SEND BUTTON: enabled when text is non-empty

4. else if invitation_status == true AND is_active == false AND messages.isNotEmpty
   (= recipient sent an invitation; current user hasn't replied yet)
   → INFO TEXT:
        Icon: envelope
        Title: "This is an invitation"
        Body: "Reply to accept the invitation"
   → FIELD: enabled. Placeholder: "Write Your Message"
   → SEND BUTTON: enabled

5. else if invitation_status == false AND is_active == false AND messages.isNotEmpty
   (= current user sent the invitation; recipient hasn't responded)
   → INFO TEXT:
        Icon: envelope
        Title: "Invitation sent"
        Body: "You can send another message when <userName> responds."
   → FIELD: disabled. Placeholder: "Messaging disabled"
   → SEND BUTTON: disabled

6. else (normal active chat: invitation_status == true AND is_active == true)
   → INFO TEXT: none (no banner above field)
   → FIELD: enabled. Placeholder: "Write Your Message"
   → SEND BUTTON: enabled when text is non-empty
```

### Forward-compatible: `can_message` + `block_reason`
Backend now also ships `can_message: bool` and `block_reason: string?` on the same response. **For now**, the legacy fields above (`i_blocked` / `they_blocked` / `invitation_status` / `is_active`) drive the UI on iOS. Decode `can_message` + `block_reason` as optional fields so future server changes don't break parsing.

**`block_reason` values** (priority order, fixed server-side):
1. `"i_blocked"` (highest — current user took action; show Unblock CTA)
2. `"they_blocked"`
3. `"invite_pending"`
4. `"rate_limited"`
5. `null` (sending allowed)

When you eventually migrate the UI to read `can_message` directly, the decision tree collapses to: `if !can_message → render banner based on block_reason → disable field`. But for now, keep the legacy state machine for parity with iOS.

### Edit mode (overrides normal compose)
When `editingMessageId != null`:
- An X cancel button appears to the LEFT of the field.
- Field placeholder: "Edit your message"
- Field pre-fills with the message text.
- Send button becomes a checkmark (✓).
- Tap ✓ → fires PATCH /messages/{id} with the new text.
- Tap X → discards edit, clears field.

### Link-stripping (sanitization on input)
The field's `onChange` strips http://, https://, and www. URLs from user input in real time. Other text + whitespace pass through. Backend would reject these, but stripping client-side prevents the user from typing a link and being confused when it disappears server-side.

---

## Chat list row (MessagesView) ⭐

Each row in MessagesView is built from `ChatRoomModel`:
```
{
  chatroom_id: string,
  invitation_status: bool,
  user: { id, username, display_name, profile_type_name, profile_picture_url?, i_blocked, they_blocked },
  last_message?: {
    id, text, image_id?, image_url?, sender_id, receiver_id, created_at,
    is_read, media_user_id?, media_user_display_name?,
    media_user_profile_picture_url?, artwork_title?
  },
  unread_count: int,
  is_active: bool,
  remaining_invites: int,
  can_message: bool,
  block_reason: string?
}
```

### Row display rules
- **Avatar:** `user.profile_picture_url` if non-null. Falls back to colored initials (e.g. "RA" for "Raghava") with a hash-derived background color. **Important:** if image fails to load OR decode (e.g. corrupt CDN bytes, 404), fall back to initials too — not a gray broken-image placeholder.
- **Name:** `user.display_name`.
- **Timestamp:** `last_message.created_at` formatted as `"9:34 PM"` (today), `"Yesterday"`, `"May 30"`, etc.
- **Preview text:** `last_message.text` if present (truncate to 2 lines).
- **Unread badge:** small colored dot (no number) when `unread_count > 0`.
- **"invitation pending" pill** (right side, italic): rendered when `!invitation_status && last_message != null`.

### unread_count semantics
Server counts only messages where:
- `sender_id != currentUserId`
- `is_read == false`
- `is_deleted == false`
- `deleted_for_receiver == false`

Don't double-count anything client-side. The server number is canonical.

---

## Public profile "Message" button gating

When viewing someone else's `PublicProfileView`, the "Message" button is enabled/disabled based on the same fields, also returned on `GET /profile/{userId}/public/info`:

| State | Button behavior |
|---|---|
| `i_blocked == true` | Hide button entirely (or show "Unblock" CTA that opens an unblock confirmation) |
| `they_blocked == true` | Disabled (greyed out), tooltip "You can't message this user" |
| `can_message == true` | Enabled; tap → navigate to UserChatView |
| `can_message == false, block_reason == "invite_pending"` | Show "Invitation Sent" pill instead of Message button |
| `can_message == false, block_reason == "rate_limited"` | Disabled with retry-after message |

---

## Subscription-aware UX summary

Quick reference for behaviors gated on `profile.subscription`:

| Surface | Behavior |
|---|---|
| Upload tab | See "Upload tap handler" above. Gallery + Basic → web alert; other Basic → in-app upgrade. |
| Shop link field on UploadView | Visibility from `ShopLinkVisibility.resolve(profile, isArtistProActive)` matrix. |
| Profile link field on UploadView | Visible for all roles (no gating). |
| Settings → Change Role row | Hidden for Gallery role; hidden for any paid plan. |
| Settings → Select Plan | Always visible. Carousel + footer content varies by role/sub state (see Plans section). |
| Subscription badges on public profile | "Pro" badge for Artist Pro, "Gallery" badge for Gallery. Driven by `profile.subscription.plan`. |

---

## Critical contract reminders

1. **Mobile has ONE invite-code field.** Never send `referral_code` to `request-otp` / `verify-otp` / `createUserProfile`. Backend 422s. The single field's value is disambiguated by `/verify-invite` returning `code_type`.

2. **Gallery is web-only end-to-end.**
    - No `profile_type_id = 4 (Gallery)` ever in POST /profile from mobile (backend 400s).
    - No StoreKit IAP for Gallery (backend's verify-apple expects Artist Pro product id).
    - No in-app purchase CTA buttons for Gallery anywhere.
    - No deep-link path for Gallery referrals.

3. **Apple anti-steering compliance (3.1.1).** Wherever you'd otherwise show a "buy on web" CTA: rephrase as account-management language ("Managed on artrinx.com", "Open Website" with no purchase verbs). No "Subscribe", "Buy", "Upgrade", "Join" verbs on any external-link button.

4. **`subscription.is_paid` is the source of truth for premium gating** in UX. `StoreKitManager.isArtistProActive` is advisory only (used to drive purchase-flow UX like paywall auto-dismiss). Backend MUST also re-check on every premium-only API call; client gating is for UX, not security.

5. **Receipt verification:** send Apple's JWS-signed `Transaction.jsonRepresentation` to `verify-apple` / `restore-apple`, not just transaction IDs. Server verifies the JWS signature against Apple's public key.

---

## Reference files in iOS source (for cross-checking)

| Concern | iOS file |
|---|---|
| Endpoints catalog | `rinx-v2/Network/Endpoints/APIService+*.swift` |
| Chat ViewModel state machine | `rinx-v2/Screens/MainApp/Notification+MessagingTab/Messages/ViewModel/ChatViewViewModel.swift` |
| Chat compose field gating | `rinx-v2/Screens/MainApp/Notification+MessagingTab/Messages/View/UserChatView.swift` (search `UserChatStatusInfo`, `UserChatTextFieldView`) |
| Chat list row | `rinx-v2/Screens/MainApp/Notification+MessagingTab/Messages/View/ChatListRowView.swift` |
| Avatar fallback | `rinx-v2/Screens/Components/ProfileAvatarView.swift` |
| Upload gate branching | `rinx-v2/Screens/MainApp/Upload/Views/UploadHomeView.swift` (search `handleUploadTap`) |
| Plan card | `rinx-v2/Screens/Components/PlanCardView.swift` |
| Plan visibility matrix | `rinx-v2/Models/PlanType.swift` (search `availablePlans(for:)`) |
| Shop-link visibility | `rinx-v2/Models/ShopLinkVisibility.swift` |
| WebSocket | `rinx-v2/Network/WebsocketManager.swift` |
| Deep-link parser | `rinx-v2/Router/DeepLinkParser.swift` |
| Push handler | `rinx-v2/App/AppDelegate.swift` (UNUserNotificationCenterDelegate) |
| Native auth client | `rinx-v2/Features/Authentication/NativeAuthClient.swift` |

---

*Last updated: iOS main HEAD at 2026-06-09. Backend contracts: stage rev 112+ (post chat-access surface, post engagement-notifications spec, post 2026-06 pricing & limits revamp).*
