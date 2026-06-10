# artRINX — iOS implementation handout for Android team

Updated **2026-06-10** for iOS **V1.9**. Use this alongside the Figma. Where the Figma is silent or stale, this doc is authoritative for *behavior* — Figma stays authoritative for *visuals*.

Project context: invite-only art discovery and social platform — real-time messaging, artwork uploads, curations, shop links, events. Bundle ID `com.rinx.artRINXapp`. Backend host: `https://apifargate.rinx.com`. CDN host: `https://devartrinx.b-cdn.net`. Web companion: `https://www.artrinx.com`.

---

## 1. App flow state machine

The root view switches between five states. State transitions are driven by `isInitialized`, `isAuthenticated`, `isProfileComplete`, and `hasFinishedOnboarding` (UserDefaults).

| State | Shown when | Screen |
|---|---|---|
| `launching` | App boot, splash holds for min 1s | SplashView (logo + transition) |
| `onboarding` | First run (no `hasFinishedOnboarding`) | OnboardingView (3-screen onboarding) |
| `invite` | Past onboarding, not authenticated | InviteView (invite code entry) |
| `profileSetup` | Authenticated but profile incomplete | ProfileSetupView (multi-step) |
| `mainApp` | Authenticated + profile complete | TabBarView (5 tabs) |

A `sessionGeneration` counter bumps on every sign-in/out; long-running async work captures the value at start and drops late-arriving results if it has changed — prevents stale data from one user landing on another's session.

---

## 2. Authentication

Native OTP-only auth (no passwords). Backend host: `apifargate.rinx.com/api/auth/native/...`.

### Endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/native/request-otp` | Request OTP via email or phone; body keyed by channel (`email` OR `phone`), `mode: "signin"\|"signup"`, optional `invite_code` / `referral_code` |
| POST | `/api/auth/native/verify-otp` | Verify 6-digit code (field name `code` — NOT `otp`). Carries `consents` object on signup |
| POST | `/api/auth/native/refresh` | Token rotation; returns `RefreshResult { tokens, user }` envelope |
| POST | `/api/auth/native/sign-out` | Revoke session |

### OAuth alternatives (new in V1.9)

`AuthMethodPickerView` — entry screen offering 4 methods:

1. Sign in with Email (→ `EmailOtpSignInView` → `EmailOtpVerificationView`)
2. Sign in with Phone (→ `LoginWithPhoneView` → `OtpVerificationView`)
3. Sign in with Apple (`AppleSignInService` — uses `ASAuthorizationAppleIDProvider`)
4. Sign in with Google (`GoogleSignInService` — uses Google's iOS SDK)

Apple + Google flow: provider gives an ID token, backend exchanges it via `/api/auth/native/oauth` for the standard session envelope. Same downstream — populates `currentUser` + token pair in keychain.

### Consents collected at signup time (V1.9)

The `consents` object sent on `verify-otp` (signup mode only):

```
{
  "accepted_terms": true,         // required
  "sms_2fa_consent": true,        // required for phone signup
  "account_notification_sms": bool,
  "marketing_sms_consent": bool   // optional opt-in
}
```

Backend stamps these onto the user record atomically with account creation.

### Session storage

Token pair (access + refresh) stored in iOS Keychain via `TokenStore`. Refresh handled by `RefreshCoordinator` — single-flight (concurrent refresh calls share one network round-trip), with a 30s recency cache (same `RefreshResult` returned to callers in the window). Proactive refresh scheduled by `ProactiveRefreshScheduler` ~60s before access token expiry.

### Onboarding screens

| Screen | Purpose | Next |
|---|---|---|
| `OnboardingView` | 3-page carousel intro + "Get Started" button. Sets `hasFinishedOnboarding=true` on completion. | InviteView |
| `InviteView` | Invite code text field + Continue. POST `/api/verify-invite` with code. Response `code_type: "peer"\|"admin"\|"agent"`. peer/admin → push to AuthMethodPicker. agent (Gallery referral) → routes to `artrinx.com/gallery` web (no in-app continuation). | AuthMethodPicker / Gallery web |
| `WaitlistView` | Reached from InviteView "Join Waitlist". Collects name + email + country/state/city + accepted terms + marketing consent. POST `/waitlist`. | ConfirmWaitlistView |
| `ConfirmWaitlistView` | "You're on the waitlist" confirmation. | None (dead end) |
| `AuthMethodPickerView` | 4-option picker (Email/Phone/Apple/Google). | Per method |
| `LoginWithPhoneView` / `RegisterWithPhoneView` | PhoneNumberKit-powered phone input. Calls `request-otp` with phone channel. | OtpVerificationView |
| `EmailOtpSignInView` / `RegisterWithEmailView` | Email input. Calls `request-otp` with email channel. | EmailOtpVerificationView / SignupEmailOtpVerificationView |
| `OtpVerificationView` / `EmailOtpVerificationView` / `SignupEmailOtpVerificationView` | 6-digit OTP entry (auto-paste from SMS/email). Calls `verify-otp` with `code`. Carries `consents` on signup variant. | Mainapp or ProfileSetup |

### Profile setup (after signup, before main app)

`ProfileSetupView` is a multi-step wizard:

1. `ProfileDetailsView` — username (live availability check via `/api/profile/username-check?username=`), full name, bio, age, country/state/city
2. `ProfileImagePickerView` — profile picture upload (multipart)
3. `ProfileTitleView` — role picker: Artist / Collector / Art Curious. (Gallery role is web-only — not in iOS picker per Plan/Role policy.)
4. `PreferredMediumsView` — multi-select from GET `/api/mediums/` (e.g. Painting / Sculpture / Drawing / Photography / Digital / Mixed Media / Print)
5. Submit → POST `/api/profile` (multipart with image file + form fields)

`StepIndicatorView` shows progress dots at the top.

---

## 3. Main app — tab bar

`TabBarView` hosts a SwiftUI `TabView` (iOS 18+ `Tab(...)` syntax with the new floating Liquid Glass style on iOS 26). 5 tabs:

| Index | Tab | Tab icon (SF Symbol) | Root screen |
|---|---|---|---|
| 0 | Home | `house` | `HomeView` |
| 1 | Search | `magnifyingglass` | `SearchView` |
| 2 | Upload | `plus.circle` | `UploadHomeView` |
| 3 | Notifications | `bell` (+ red badge) | `NotificationMessagingView` |
| 4 | Profile | `person` | `ProfileView` |

`GlobalCoordinator` owns one router per tab (all are `Router<HomeFlow>` typealiased — `HomeFlowRouter`, `SearchFlowRouter`, `NotificationFlowRouter`, `ProfileFlowRouter`). Push nav within each tab. Tab switches preserve their stacks.

---

## 4. Home tab (`HomeView`)

3 subtabs in a paged layout (uses a `UICollectionViewController` under the hood for vertical scroll perf; SwiftUI views inside each card via `HostingCollectionViewCell`).

### Subtabs

| Subtab | Endpoint | What it shows |
|---|---|---|
| Discover | `GET /api/feed/discover` | Mixed feed: sponsored banners, "New Art For You" (10 cards), "Popular Curations" (10), "More Art" (10). Has its own ResponseCache for instant boot. |
| Shop | `GET /api/artworks/shop?page=&size=` | Paginated artworks with `shop_link` populated. Tap "Go to NAME's shop" → branded alert → SFSafariViewController. |
| For You | `GET /api/artworks/recommended?page=&size=` | Personalized recommendations. |

### Home navigation destinations (`HomeFlow` enum)

These are the destinations reachable from any tab via `router.navigate(to: .XXX)`:

```
case publicArtDetails(artworkId: Int)
case publicCurationDetails(curationId: Int)
case publicProfile(userId: Int)
case userChat(userId: Int, userName: String? = nil, userTitle: String? = nil)
case newChatSearch
case settings
case editProfile
case changeEmail / addEmail / addPhone / changePhone
case changeRole
case selectPlan
case devices
case inviteFriends
case blockedAccounts
case phonePermissions
case artDetailPage(artworkId: Int)            // own artwork detail (editable)
case editartwork(artwork: UserArtwork)
case previewArtwork(artwork: UserArtwork, request: EditArtworkRequest)
case curationDetailPage(curationId: Int)
case editcuration(curation: UserCuration)
case followersFollowing(initialTab: FollowersFollowingTab = .followers)
case artistWithProfile(artistId: Int)         // tap artist who has a profile
case artistWithoutProfile(artistName: String) // tap free-text attributed artist
case bannerWebView(url: URL, title: String)
case otpverification(...)
```

### `PublicArtDetailView` (artwork detail)

Reached via `publicArtDetails(artworkId:)`. Loads `GET /api/artworks/{id}`. Shows:

- Full-size webp image (zoomable via pinch-to-zoom)
- Title, description, like/share/+ buttons (heart, paperplane, add-to-curation)
- Likes count (animated count transitions on tap)
- Artist row → taps to `artistWithProfile(artistId)` or `artistWithoutProfile(artistName)`
- Medium row
- Shop link (if present) → branded alert → SFSafariViewController
- Uploader profile chip → `publicProfile(userId)`
- Below: "Similar Artworks" carousel via `GET /api/artworks/{id}/similar?page=&size=`
- Records view via `POST /api/admin/view-history/record` with `{target_id, target_type: "artwork"}`

### `PublicCurationDetailView` (curation detail)

Reached via `publicCurationDetails(curationId:)`. Loads `GET /api/curations/{id}`. Shows curation title, description, owner chip, and a grid of artwork tiles. Tap tile → `publicArtDetails(artworkId:)`. Tap "Add to Curation" if applicable.

### `PublicProfileView` (someone else's profile)

Reached via `publicProfile(userId:)`. Loads in parallel:

- `GET /api/profile/{userId}/public/info` → name, bio, profile link, mediums, counts, follow status, blocked status, can_message flag, remaining message-requests
- `GET /api/profile/{userId}/public/artworks?page=&size=` → their artworks
- `GET /api/profile/{userId}/public/curations?page=&size=` → their curations

Two tabs: "Art" / "Collections". Follow / Message buttons at top. "Following" pill expands to popover (Unfollow / Block / Report). Tap profile link → branded "Go to portfolio" alert → Safari sheet.

Block / Unblock flows present `customOverlayAlert` confirmations. Report opens `ReportProfileView` sheet.

### `ArtFeedCardView`

Reusable card used in feeds. Shows uploader avatar + name + role, image, title, like / share / heart actions. Tap image → `publicArtDetails`. Tap profile chip → `publicProfile`. Tap artist name → `artistWithProfile` or `artistWithoutProfile` based on `artist.artist_id` nullity.

---

## 5. Search tab (`SearchView`)

Three modes:

| Mode | Endpoint | Result rows |
|---|---|---|
| Initial (empty search) | `GET /api/search/trending-tags` | `TrendingTagsView` — top tags clickable to populate search |
| Type-ahead users | `GET /api/search/users?query=` | `SearchUserResultsView` |
| Submit / full search | `GET /api/search?q=&type=` | `SearchArtworkResultView` / `SearchCurationsResultView` / `SearchUserResultsView` |

Filter chips at top: All / Artworks / Curations / Users (drives `type` parameter). Results push to detail views via the standard flow destinations.

---

## 6. Upload tab (`UploadHomeView` → `UploadView`)

Quota-gated. `UploadHomeView` shows:

- `UploadQuotaCard` — "X of Y artworks used" (server-driven `maxUploads` + `artworkCount`)
- `UploadActionCard` — primary CTA. State depends on quota:
    - Has quota → "Upload Artwork" → opens `UploadView`
    - Out of quota → `LimitReachedAlertView` ("Upload limit reached / Delete an existing artwork to upload a new one")
    - Free Artist tier hitting limit → `UploadUpgradeRequiredAlertView` → `SelectPlanView` sheet
    - Free Collector/ArtCurious (non-upgradable) → same LimitReached alert (no upgrade button — they don't have a purchasable plan)
    - Gallery user → `UploadGalleryWebOnlyAlertView` → opens `artrinx.com` (Gallery is web-purchase-only)
- `PlusButtonView` — bottom floating + button

### Upload flow (`UploadView`)

`UploadManager.shared` (singleton `@Observable`) holds form state. Validation gates the Upload button (`canUpload` requires `isFormValid`):

| Field | Required | Validation |
|---|---|---|
| Title | Yes | non-empty, ≤40 chars |
| Description | Yes (today) | non-empty, ≤255 chars |
| Artist (name) | Yes | non-empty |
| Artist (ID, optional) | No | set when picked from `SearchArtistViewUpload` results |
| Medium | Yes | selected from `GET /api/mediums/` |
| Tags | No | array, validated for max count |
| Shop link | No | URL when present; if set, price also required |
| Price | Conditional | required when shop link set |
| Privacy | No | bool toggle |
| Image | Yes | photo picker, multipart upload |

### Upload sequence

1. `POST /api/prepare-upload/` → returns `filepath` (signed S3/B2 upload URL prep)
2. `PUT` raw image bytes to `filepath` (multipart with file)
3. `POST /api/artworks/` (form-encoded) with:
   ```
   title, image_url (the filepath), description, artist_name, artist_id (optional),
   medium_id, privacy (bool), aspect_ratio, shop_link (optional), price (optional),
   tags (comma-separated string, optional)
   ```
4. Response includes the created `UserArtwork`. ResponseCache invalidated for relevant feeds.

`UploadProgressCard` shows live progress while the upload runs in background. Background upload via `URLSession` background config so the upload survives if the user backgrounds the app.

`ArtUploadPreview` is a final review screen before submit.

`SearchArtistViewUpload` — when user types artist name, debounced search via `GET /api/search/users?query=`. Results show in `SearchArtistCardViewUpload` rows. Picking one sets both `artist` (display name) and `artistId` (numeric). Otherwise free-text artist with no `artistId`.

`SelfUserCardView` — quick-pick "self as artist" card at top of search results.

`CreateCurationView` — separate flow under Upload tab. Multi-select existing artworks → name the curation → `POST /api/curations/`.

---

## 7. Notifications + Messages tab

`NotificationMessagingView` is a segmented control switching between two views:

### Notifications subtab (`NotificationView`)

Loads `GET /api/notifications` → list of `Notification` rows. Each row dispatched by `type` to a dedicated view (see table). All rows auto-mark-read on appear via `PATCH /api/notifications/{id}/read`.

| `type` (wire) | Row view | Tap behavior |
|---|---|---|
| `artwork_like` | `ArtworkLikeNotificationView` | Thumbnail → `publicArtDetails(target.id)`. Actor name link → `publicProfile(actor.id)`. |
| `artwork_share` | `ArtworkShareNotificationView` | Same as above. |
| `curation_like` | `CurationLikeNotificationView` | Thumbnail → `publicCurationDetails(target.id)`. Actor name → profile. |
| `curation_share` | `CurationShareNotificationView` | Same. |
| `follow` | `FollowNotificationView` | Thumbnail (actor avatar) → `publicProfile(actor.id)`. Actor name → profile. |
| `profile_share` | `ProfileShareNotificationView` | Same. |
| `event_created` / `event_reminder_week` / `event_reminder_day` / `event_reminder_3h` | `EventNotificationRowView` (**NEW V1.9**) | **Whole row tap** → fetches event + shows popup. Only `@handle` text (colorTheme blue) navigates to organizer profile. |
| Other / unknown | `GenericNotificationView` | No tap target. |

### `Notification` wire shape

```json
{
  "id": 2603,
  "type": "event_reminder_3h",
  "message": "is hosting an event near you in 3 hours.",
  "is_read": false,
  "timestamp": "2026-05-20T00:16:30Z",
  "actor": { "id": 167, "name": "Kapil dev", "profileImageUrl": "https://..." },
  "action": "liked" | "followed" | "shared" | null,
  "target": { "type": "artwork"|"curation"|"profile"|"event"|null, "id": int|null,
              "name": str|null, "profileImageUrl": str|null, "title": str|null, "thumbnailUrl": str|null },

  // Event-only fields (null on all other types):
  "organizer_name": "Untitled Gallery",
  "organizer_handle": "untitledgal",
  "organizer_profile_url": "https://www.artrinx.com/profile/167",
  "event_id": 175,
  "event_image_url": "https://cdn.artrinx.com/events/175.jpg" | null
}
```

`type` and `target.type` are tolerant enums — unknown server values decode to `.unknown` and the row falls back to `GenericNotificationView` instead of failing the whole array decode.

### Event notification row layout (V1.9)

`EventNotificationRowView` matches Figma node `3600-9781`:

- Left: event banner image (`event_image_url`) as 55×55 rounded square (corner radius 8). Falls back to actor avatar if `event_image_url` is null.
- Right: composed `AttributedString`: `{organizer_name}` (Poppins-SemiBold, primary color) + ` ` + `@{organizer_handle}` (Poppins-SemiBold, **colorTheme blue**, tappable link to profile) + ` ` + `{message}` (Poppins-Regular, primary color)
- Footer right: timestamp (relative — "1h ago")

**Tap behavior is specific:**
- Tap `@handle` (the colored part only) → `router.navigate(to: .publicProfile(userId: organizer_id))`
- Tap **anywhere else** on the row (thumbnail, name, verb-phrase, whitespace) → fetches event + presents popup

This is a deliberate divergence from the other notification types (which only have actor-name + thumbnail tap targets, with whitespace being non-tappable).

### Event detail popup (`EventDetailPopupView`)

Reached when an event row is tapped. Loads `GET /api/events/{event_id}`. Renders as a centered card matching Figma node `3611-9993`:

- Top: megaphone icon (SF Symbol `megaphone.fill`, 56×56, colorTheme blue)
- Title: `event_name` (Poppins-SemiBold 18)
- 3 rows with fixed-width label column (76pt) for vertical alignment:
    - **Date**: `"September 3rd, 2026"` — month name, ordinal day (`st`/`nd`/`rd`/`th`), comma, year. Rendered in `event_tz`.
    - **Time**: `"6:00PM – 9:00PM"` — start–end range, en-dash separator, **no space between number and AM/PM**, rendered in `event_tz`.
    - **Location**: 3 lines: `venue_name` / `street_address` / `"City, ST PostalCode"` (US) or `"City, Country"` (single-tier country)
- Bottom: "Learn more" CTA button (currently no-op — destination TBD by backend)
- "X" close button top-right

Presented via `customOverlayAlert` (UIWindow at `.alert + 1` — covers iOS 26 floating tab bar and toolbar).

### Event detail wire shape (`GET /api/events/{id}` → V1.9 contract)

```json
{
  "id": 175,
  "event_name": "shiva artworks publish event",
  "image_url": null | "https://...",
  "venue_name": "Maharashtra",
  "street_address": "5, 2nd Floor, Bharmal House, ...",
  "city": "Mumbai",
  "state": "maharashtra",
  "postal_code": "400003",
  "country": "india",
  "start_at_utc": "2026-05-20T01:16:00Z",   // ISO 8601 UTC
  "end_at_utc":   "2026-05-20T10:16:00Z",
  "event_tz":     "Asia/Kolkata",            // IANA timezone name
  // Legacy aliases also present for back-compat — ignore:
  "date", "start_time", "end_time", "address_line_1", "address_line_2",
  "zipcode", "name_of_place", "event_time", "location_name", "event_url",
  "timezone", "user_id", "created_at", "updated_at"
}
```

Construct UTC instant from `start_at_utc` (ISO 8601) and render in `event_tz` (IANA), not the device's local zone. A NYC event always reads `6:00PM – 9:00PM` regardless of where the viewer is. Fall back to UTC if `event_tz` is missing or unparseable.

### Push notifications for events

Backend ships FCM payloads for 4 moments: immediate-on-create, 7 days before, 1 day before, 3 hours before. Audience restricted to users whose profile city+state+country matches the event.

```
notification.title = "artRINX"
notification.body  = "{organizer_name} @{organizer_handle} {verb}"
  // verb varies by type:
  //   event_created       → "is hosting an event near you."
  //   event_reminder_week → "is hosting an event near you in 1 week."
  //   event_reminder_day  → "is hosting an event near you tomorrow."
  //   event_reminder_3h   → "is having an event near you in 3 hours."
```

FCM `data` dict (all values strings, null-valued keys dropped):

```
event_id, type, event_name, event_image_url,
organizer_name, organizer_handle, organizer_profile_url,
start_at_utc, end_at_utc, event_tz,
venue_name, street_address, city, state, postal_code, country
```

iOS handles push tap via `AppDelegate.didReceive`. Reads `data["url"]` if present; otherwise constructs `rinxart://event/{event_id}` from `data["event_id"]` and routes through `DeepLinkParser` → `GlobalCoordinator.pendingEventId` → `NotificationView` consumes the id and opens the popup.

### Messages subtab (`ChatListView`)

Loads `GET /api/my/chatrooms` → list of `ChatRoomModel`. Each row shows: other user's avatar + name + last message + unread count. Tap → `userChat(userId, userName, userTitle)`.

Search bar at top filters by username / display-name / last-message text (client-side filter on the loaded list).

`NewChatUserSearchView` — "+" button. Search users by query → pick → starts new chat (gated by `remaining_message_requests` quota). Calling `POST /api/messages/` with the first message creates the chatroom.

Quota: free Basic = 15 new chats/month, Artist Pro = 25, Gallery = 25. After cap: `"You've reached your monthly limit for new chats. Unlimited messages within your active chats."`

### `UserChatView` (1-1 chat)

Loads `GET /api/messages/with/{userId}?page=&size=` — paginated chat history. Bubble layout (own messages right, other left). WebSocket `WebSocketManager` (singleton, `@MainActor`) pushes new messages live via `chat` event type. Read receipts via `PATCH /api/messages/{id}/read`.

Compose bar at bottom: text field + send. `POST /api/messages/` with `recipient_id` + `text`. Edit/delete own messages via long-press: `PATCH /api/messages/{id}` (newText) and `DELETE /api/messages/{id}`.

Block/unblock confirmations are `customOverlayAlert`s. Report user → sheet.

---

## 8. Profile tab (`ProfileView`)

Own profile. Loads `GET /api/profile`. Layout:

- `ProfilePageHeaderView` — profile picture, display name, role, bio, profile link (tap → external Safari with branded alert), counts (Art / Collections / Followers / Following)
- 3 segmented tabs: "Art" / "Collections" / "Liked"
- Tab content via `ArtListView` / `CurationListView` / `LikedListView` — paginated:
    - Art: `GET /api/artworks/?page=&size=` (own)
    - Curations: `GET /api/curations/?page=&size=` (own)
    - Liked: `GET /api/artworks/liked?page=&size=`
- Settings gear (top right) → `settings`
- Followers/Following counts tap → `followersFollowing(initialTab:)` → 2-tab view with `FollowersView` (`GET /followers`) + `FollowingView` (`GET /followed-users`)

### Own artwork detail (`UserArtDetailView`)

Reached via `artDetailPage(artworkId)`. Same data as `PublicArtDetailView` but with edit/delete affordances. Tap edit → `editartwork(artwork)` → `EditArtView`. Tap delete → `customOverlayAlert` confirmation → `DELETE /api/artworks/{id}`.

### Edit artwork (`EditArtView`)

Form pre-filled from existing artwork. PUT `/api/artworks/{id}` with `EditArtworkRequest` body (multipart for image change, JSON for fields-only). Reuses `ArtworkPreviewView` for the final-review pattern.

### Own curation detail (`UserCurationDetailView`)

Same as public curation but with reorder / remove / delete affordances. `editcuration(curation)` → `EditCurationView` (add/remove artworks via multi-select, `PUT /api/curations/{id}`).

---

## 9. Settings stack

`SettingsView` is the landing screen. Sections + their destinations:

### Account
- **Edit profile** → `EditProfileView` — full name (max 2 edits enforced via `full_name_edit_count`), bio, age, country/state/city, profile link, profile picture. `PUT /api/profile/update` (multipart).
- **Change email** → `ChangeEmailView` (existing email user) or `AddEmailView` (no email yet). Triggers email-change OTP via `AuthRepository.startChangeEmail`.
- **Change phone** → `ChangePhoneView` or `AddPhoneView`. Same OTP flow.
- **Select plan** → `SelectPlanView` — paywall with 3 plan cards (Basic / Artist Pro / Gallery). StoreKit-driven for Artist Pro; Gallery shows "Managed on artrinx.com" web-only footer.
- **Invite friends** → `InviteFriendsView` — shows your invite code, share sheet integration.
- **Delete Account** → `DeleteAccountConfirmationView` modal → `DELETE /api/user/delete-me` → sign-out + return to onboarding.

### Privacy
- **Blocked Accounts** → `BlockedAccountsView` (`GET /api/blocked/users`) — unblock via per-row action.
- **Phone Permissions** → `PhonePermissionsView` — two toggles:
    - Marketing SMS Communications: reads `profile.marketing_sms_consent`, writes via `PUT /api/profile/update` form field `marketing_sms_consent`. Shows `EnableMarketingPopup` / `DisableMarketingPopup` modal confirmation before toggling.
    - Push Notifications: reads `UNUserNotificationCenter.notificationSettings()`. First grant requests authorization. Denied state opens iOS Settings.

### Devices (Gallery role only — exhibition screens)
- **Devices** → `DeviceView` (`GET /api/exhibitions/`) — list of registered screens.
- **Edit Device** → `EditDeviceView` — assign artwork or curation to display, set orientation (portrait/landscape) + rotation duration.

### Resources
- Tutorial / About Us / Terms and Conditions / Community Guidelines / Privacy Policy — all open `https://www.artrinx.com/{slug}` in `bannerWebView`.

### Bottom
- **Logout** → `LogoutConfirmationView` → calls `AuthRepository.signOut()` + `coordinator.resetOnLogout()` → returns to Invite screen.
- **Sign out of all devices** → `SignOutAllDevicesConfirmationView` → backend-side session revocation across all devices.
- **App Version V1.9** — static text at bottom.

### URL constants

```
baseWebURL            = "https://www.artrinx.com"
termsOfUse            = baseWebURL + "/terms-of-use"
privacyPolicy         = baseWebURL + "/privacy-policy"
aboutUs               = baseWebURL + "/about-us"
communityGuidelines   = baseWebURL + "/community-guidelines"
artworkURL(id)        = baseWebURL + "/artwork/{id}"
curationURL(id)       = baseWebURL + "/curation/{id}"
profileURL(id)        = baseWebURL + "/profile/{id}"
inviteURL(code)       = baseWebURL + "/invite/{code}"
```

---

## 10. Subscription / paywall

### Plans (V1.9 — server-driven via `profile.subscription`)

| Plan | Wire value | Price | Source | Features (matrix) |
|---|---|---|---|---|
| Basic | `basic` | Free | — | 10 artworks, 5 invites/month, 15 new chats/month |
| Artist Pro | `artist_pro` | $14.99/mo (USD) | Apple IAP | 99 artworks, profile link, shop link, 5 invites, 25 new chats |
| Gallery | `gallery` | $99/mo | Stripe via `artrinx.com` (web only) | 99 artworks, profile link, shop link, 25 invites, 25 new chats, create events, city-targeted push notifications |

`profile.subscription` shape:

```json
{
  "plan": "basic"|"artist_pro"|"gallery",
  "status": "active"|"inactive"|"expired"|"in_grace_period",
  "provider": "apple"|"stripe"|null,    // null when free
  "expiresAt": "ISO 8601" | null,
  "renewsAt":  "ISO 8601" | null,
  "trialEndsAt": "ISO 8601" | null
}
```

**Source of truth:** `profile.subscription.isPaid` (server-blessed) gates premium UI features. `StoreKitManager.isArtistProActive` is advisory only — used for paywall auto-dismiss UX, never to gate features. Backend re-checks subscription on every premium-only API call.

### Artist Pro StoreKit flow

`StoreKitManager.shared` exposes `artistProProduct` (`com.rinx.artRINXapp.artistpro_monthly`) and observes transactions. Purchase → `Transaction.jsonRepresentation` (JWS-signed) sent to backend via `POST /api/subscriptions/verify-apple`. Restore → `POST /api/subscriptions/restore-apple` with `{ originalTransactionId }`. Backend matches `productID` loosely (any ID containing "artistpro") so SKU rename doesn't break verification.

Intro offer: **2 months free trial** on Artist Pro. Rendered dynamically from `product.subscription?.introductoryOffer` (period.value + period.unit). Falls back to silence if not parseable.

### Gallery role policy

Gallery is **web-purchase-only via Stripe**. iOS shows the plan card on `SelectPlanView` but with no in-app CTA — bottom shows "Available on artrinx.com" sub-label. Per Apple anti-steering rule (App Store 3.1.1), no in-app link to the external purchase. Users hit `artrinx.com/gallery` independently.

### Plan changes for paid Gallery users

A Gallery user (paid via Stripe) cannot change role via `ChangeRoleView` (role lock in place — role + paid is a contract). UI hides the role-change row for them.

### Upload gate by role

`canUpgradeInApp` predicate = `(!isPaid && role == .artist)`. Controls the upgrade-required vs limit-reached alert split — Collector/ArtCurious users see "Upload limit reached / Delete to make room" instead of the upgrade alert (no purchasable plan for their role).

---

## 11. Push notifications + deep links

### Firebase Cloud Messaging

`AppDelegate` registers for remote notifications post-permission-grant (not at launch — to avoid the iOS denial without permission UI). `FirebaseMessaging` SDK auto-handles APNs→FCM token forwarding. Token sent to backend via `POST /api/me/fcm-token` whenever it rotates.

### Push payload dispatch

`AppDelegate.didReceive(_:withCompletionHandler:)`:

1. Reads `userInfo["notification_id"]` if present → marks as read via `PATCH /api/notifications/{id}/read`
2. Gallery enterprise notice gate: `userInfo["kind"] == "gallery_enterprise_notice"` → no-op (no in-app webview, no banner — informational only, per App Store policy)
3. Deep link extraction:
    - If `userInfo["url"]` present → route via `PushNotificationRouter.shared.route(url)`
    - Else if `userInfo["type"]` starts with `"event_"` AND `userInfo["event_id"]` present → constructs `rinxart://event/{event_id}` locally and routes (fallback in absence of backend-provided `url`)

### Deep link URL schemes (`DeepLinkParser`)

Accepts both `rinxart://` (custom scheme) and `https://www.artrinx.com/` (universal links). Either form decodes to the same `DeepLinkContentAction`:

```
rinxart://invite/{code}                  → .invite(code: String)
rinxart://artwork/{id}    OR /artworks/  → .artwork(id: Int)
rinxart://curation/{id}   OR /curations/ → .curation(id: Int)
rinxart://profile/{id}    OR /profiles/ OR /user/ OR /users/ → .profile(userId: Int)
rinxart://event/{id}      OR /events/    → .event(id: Int)
```

`GlobalCoordinator.handleDeepLink(url)` parses and applies:
- `.invite` → stashes pending invite code (consumed on next InviteView mount)
- `.artwork` / `.curation` / `.profile` → switches to `.home` tab and pushes the corresponding detail screen
- `.event` → switches to `.notifications` tab and parks `pendingEventId` for `NotificationView` to consume on appear (which fetches event + presents popup)

Associated domain: `applinks:www.artrinx.com` (entitlement file). Recommendation to backend: **emit universal-link form** (`https://www.artrinx.com/event/{id}`) in push payloads, not custom-scheme — works as link in email/SMS/web fallback when app isn't installed.

---

## 12. Modal / alert presentation

iOS uses a **custom UIWindow-based alert** for all branded confirmations (matches Figma alert styles like "Go to portfolio", "Logout", "Block user", etc.). Implementation in `CustomAlertView.swift`:

- Hosted in a dedicated `UIWindow` at `windowLevel = .alert + 1`
- Sits above EVERY system chrome layer including the iOS 26 floating Liquid Glass tab bar and navigation chrome (which `.overlay` and `.fullScreenCover` cannot cover)
- Animations driven by SwiftUI `@State` flip inside the hosted view — content fades + slides via `.transition(.blurReplace.combined(with: .push(from: .bottom)))`
- On host-view `.onDisappear`, the window tears down so logout/root-state changes clear the alert automatically
- Environment values (`AppState`, `GlobalCoordinator`) forwarded from the host into the UIHostingController's rootView

### Custom alert callsites

| Callsite | Alert content |
|---|---|
| `PublicProfileView` | Block / Unblock confirmations |
| `PublicProfileHeaderView` → `PublicProfileView` (via lifted state) | Profile link "Go to portfolio" |
| `PublicArtDetailView` / `UserArtDetailView` / `ArtworkPreviewView` / `ShopView` | "Go to shop" link |
| `SettingsView` | Logout / Sign out all devices / Delete account confirmations |
| `PhonePermissionsView` | Enable / Disable marketing popups |
| `BlockedAccountsView` | Unblock confirmation |
| `EditDeviceView` / `UserArtDetailView` / `UserCurationDetailView` | Delete confirmations |
| `ProfileView` | Feedback (Send feedback sheet) |
| `TabBarView` | Upload error / Upload limit reached / Upgrade required / Gallery web-only |
| `NotificationView` (V1.9) | Event detail popup |
| `UserChatView` | Block / Unblock confirmations in chat |

For Android: a single component analogous to the UIWindow-overlay should be implemented so alerts render above the tab bar AND nav bar consistently. Drag-to-dismiss not required (we don't have it on iOS either — explicit Cancel/Confirm buttons or X close).

### Toast pattern

`.toast(isPresented:, message:, type: .success|.error|.info)` modifier. Auto-dismiss 3s. Used for transient feedback (action success, network errors that don't block flow). Renders at the bottom-center.

---

## 13. WebSocket

`WebSocketManager` is a `@MainActor` `@Observable` singleton. Token-authenticated handshake. Used for two live event channels:

- **`notification`** events — new notification rows arrive live, pre-pended to the notifications list and bump the tab badge
- **`chat`** events — new messages arrive live in active `UserChatView`s

Exponential backoff reconnect (cap 60s). Auto-disconnects on network path loss; auto-reconnects on path restore with a freshly-fetched session token (previous one may have expired during the offline window). Uses a `listenToken: UUID` to prevent orphaned receive closures from a torn-down task from scheduling new receives on a stale handle.

WebSocket URL: derived from `WS_URL` env (`Secrets.xcconfig`). Path: `wss://...notifications`. Auth: bearer token in the connection request headers.

---

## 14. Offline mutation queue

Some user actions (specifically: artwork likes) get queued offline via `LiveMutationQueue` (SwiftData-backed `@Model PendingMutation`). Behavior:

- `ArtworkService.toggleLike(id:)` enqueues a `PendingMutation` on network failure instead of reverting the optimistic UI update
- Queue drains automatically when `NetworkMonitor.onlineStateChanges` emits an online event
- `PendingMutationKind` raw-value stability is unit-tested (don't rename enum cases)

Android equivalent: implement similar local-queue behavior for offline likes so UI doesn't bounce when network is flaky. Other mutations (sending messages, creating artworks) currently fail fast on network errors and the user retries.

---

## 15. Image pipeline

CDN: `https://devartrinx.b-cdn.net/{folder}/{uuid}.{ext}` (BunnyCDN). URL extensions:

```
?width=256                  → scaled width (px)
?aspect_ratio=16:9          → cropped/resized to ratio
?format=webp                → WebP encoding
?v=1770050675               → cache-bust version (set on profile picture updates)
```

iOS uses `URL.appendingWidth(_:quality:)` helper to add the width param scaled to display pixel density. `RemoteImage` view backed by `ImageCache.shared` (file-backed cache + in-memory bitmap cache). Synchronous cache hits on first frame, async fetch on cache miss, decoded bitmap released on `.onDisappear` to keep memory flat across long scrolls.

`ImagePrefetcher` warms upcoming images while the user is scrolling slowly.

Kingfisher is also in the dependencies but most rendering uses `RemoteImage` + `ImageCache` (the custom pipeline). `KFImage` is used for a few specific cases (image viewer, avatar).

---

## 16. Quotas + counters

`UserProfile` exposes both legacy flat counters and new nested override-aware counters (V1.9):

```
// Legacy (deprecated, still populated for back-compat):
total_user_invites        → peer-share invites cap (plan default)
remaining_invites         → peer-share invites remaining
remaining_chat_invites    → message-requests remaining (semantic shifted: was peer invites,
                            now means new-chats remaining per backend's 2026-06 revamp)
max_uploads               → upload cap by plan (10/99/99)

// New (preferred — override-aware):
peer_invites:    { monthly_cap: int, remaining: int }    // 5/5/25 by plan
message_requests:{ monthly_cap: int, remaining: int }    // 15/25/25 by plan
```

For new UI work, read from the nested `peer_invites` / `message_requests` — they reflect any admin-set override on the user's plan default. Legacy fields are plan-default only.

### Quota-driven UI

- Upload limit: `UploadQuotaCard` shows `artworkCount / maxUploads`. Server rejects over-cap requests; client gating exists for UX only.
- Message requests (new chats): "X new chats left this month" shown in chat compose; at 0, alert "You've reached your monthly limit for new chats. Unlimited messages within your active chats."
- Peer invites: "Invite N friends per month" surfaced on plan cards.

---

## 17. Roles

Four roles: `artist`, `collector`, `curious` (Art Curious), `gallery`. Stored as `profile.role` (string). Affects:

| Surface | Effect |
|---|---|
| Plan picker | Artist → [Basic, Artist Pro]. Collector/Curious → [Basic]. Gallery → [Basic, Gallery]. |
| Upload limit reached | Artist (free) → "Upgrade required" alert. Collector/Curious → "Delete to make room" alert (no purchasable plan). Gallery → web-only alert. |
| Change role | Available to Basic users. Paid Artist (Artist Pro) and Paid Gallery → role-change row hidden (role + paid is a contract). |
| Public profile rendering | Role shown as italic subtitle under display name. "Art Curious" is hidden (it's the "lurker" role; not displayed publicly). |

---

## 18. Block / report

### Block

- `POST /api/block` with `user_id` (general block) or `artwork_id` + `message`
- `POST /api/unblock` with `user_id`
- `GET /api/blocked/users?page=&size=` for the list
- Blocked-user state cached in `ProfileService.shared.isUserBlocked(_:)` (in-memory) — updated optimistically on block/unblock

When a profile is blocked, `PublicProfileView` shows `BlockedUserView` instead of the normal Art/Collections tabs. The "Message" button is disabled. Custom back-nav from `PublicProfileView` pops back to home (not the previous page) when a block just occurred.

### Report

- Profile: `POST /api/report-message` with `message` + `reported_user_id` (modal sheet for reason text)
- Artwork: `POST /api/report-artwork` with `artwork_id` + `message`

---

## 19. Required env / config

Both Debug and Release builds need a Secrets xcconfig (not committed):

```
SUPABASE_URL=                    # legacy, not used post-native-auth migration
SUPABASE_ANON_KEY=               # same
API=https://apifargate.rinx.com
WS_URL=wss://...
```

Read at runtime through `Info.plist` via the `Secrets` enum (`rinx-v2/Constants/Secrets.swift`).

Firebase config: `GoogleService-Info.plist` in the project root. Must match the Firebase project that owns Crashlytics + Remote Config + Messaging + Analytics + In-App Messaging.

---

## 20. iOS-only behaviors Android may need to replicate or skip

- **Screenshot prevention** — `ScreenShotPreventerMask` applies a secure `UITextField` trick globally to suppress screenshots of sensitive views. Android equivalent: `FLAG_SECURE` on activities where appropriate.
- **App version header** — every API request carries `X-App-Version: 1.9` (set by `AppVersionHeader`). Backend uses for legacy-traffic gating and crash report tagging.
- **`AppLogger`** — typed `os.Logger` categories: `network`, `websocket`, `auth`, `cache`, `analytics`, `push`, `storekit`, `upload`, `lifecycle`, `ui`. Backed by Crashlytics breadcrumbs for the action-level events.
- **In-process navigation deep link scheme** — within the app, AttributedString links use `profile://{user_id}` (NOT `rinxart://`) so `OpenURLAction` can route in-process without going through the full deep-link pipeline. External-facing links (push, share sheets) use `rinxart://` or universal-link `https://www.artrinx.com/...`.
- **Crash reporting** — Firebase Crashlytics. Run Script build phase uploads dSYMs on every build. Android: integrate Crashlytics same project.
- **Remote Config** — Firebase Remote Config with seeded defaults in `RemoteConfigDefaults`. Used for feature gates (small list today). Android should consume the same flag names if/when used.

---

## 21. Recent changes (V1.9) — heads-up for Android

Bundled here because some of these are visual/wire changes that won't be obvious from the Figma alone.

### Event notifications (NEW)

The 4-moment event flow described in §7 is brand new. Backend ships:
- 4 notification types (`event_created`, `event_reminder_week`, `event_reminder_day`, `event_reminder_3h`)
- Organizer triplet (`organizer_name`, `organizer_handle`, `organizer_profile_url`) on event-type notification rows
- `event_image_url` on event-type notification rows
- New endpoint `GET /api/events/{id}` returning the popup-shape (V1.9 contract — see §7 for full shape)
- FCM push payloads for each moment, audience-scoped by matching city+state+country

Android needs to implement: event notification row (Figma node `3600-9781`) + event detail popup (Figma node `3611-9993`) + push handler that constructs the event deep link from `event_id` when no `url` field is present.

### Marketing consent toggle fix

`PhonePermissionsView`: marketing toggle now reads `profile.marketing_sms_consent` (same source as where it writes via `PUT /api/profile/update`). Earlier bug had it reading `currentUser.consents.marketingSmsConsent` (signup-time snapshot, didn't reflect later mutations). Android equivalent: ensure read and write source for the toggle are the same field.

### Custom alert presentation

All branded modals now use UIWindow-at-`.alert+1`. Required because iOS 26's floating Liquid Glass tab bar and navigation chrome render above `.overlay` and `.fullScreenCover`. Android equivalent: ensure your modal dialogs render above any system tab/nav chrome that may persist on top of normal modal scrims.

### V1.10 — DO NOT use this version label

An earlier handoff incorrectly named the target build "V1.10". The actual ship is V1.9. Any reference to "V1.10" in earlier docs / commit messages should be treated as V1.9.

### Field nullability tolerance

Several wire fields can come back null even though the type implies presence:
- `Notification.target.id` / `target.type` — null on event-type rows (event_id is in a separate field)
- `Notification.action` — null on event-type rows
- `UserArtwork.artist.artist_name` and `UserArtwork.medium` and `UserArtwork.description` — null on partially-filled artworks (data leak from non-iOS upload paths)
- `Subscription.provider` — null on free plans
- `UserProfile.subscription` fields can be partially populated

Android should decode these as optional/nullable types from the start. iOS uses tolerant decoding (unknown enum → `.unknown`) for `NotificationType` and `TargetType` so a new server-rolled-out type doesn't blank the whole notifications list.

---

## 22. Open contract items / known gaps as of 2026-06-10

- **`event_tz: "UTC"` for legacy events**: Backend's IANA timezone derivation kicks in only when `events.timezone` is NULL. Existing events with a hardcoded "UTC" value bypass it. Mumbai event 175 displays in UTC instead of `Asia/Kolkata`. Backend may backfill or leave; iOS displays whatever it gets.
- **Legacy event rows returning 404**: Older `event_*` notification rows reference `event_id`s that no longer exist (events 174/175/176/177 in test data). Backend returns `Event not found` for those. iOS surfaces an error toast. Not blocking for new events going forward.
- **`organizer_profile_url` scheme**: Currently `rinxart://profile/{id}` in our test data. iOS team requested switch to universal-link form (`https://www.artrinx.com/profile/{id}`). iOS handles both; backend may switch independently.
- **`Learn more` button on event popup**: Currently a no-op. Destination TBD by product/backend.

---

## 23. Quick reference — every API endpoint, alphabetical

```
POST   /api/admin/click                                    (analytics — outbound link)
POST   /api/admin/duration                                 (analytics — Safari sheet dwell)
POST   /api/admin/view-history/record                      (artwork/curation impression)
POST   /api/artworks/                                      (create — multipart)
GET    /api/artworks/?page=&size=                          (own)
GET    /api/artworks/{id}                                  (detail)
PUT    /api/artworks/{id}                                  (edit)
DELETE /api/artworks/{id}                                  (delete)
GET    /api/artworks/{id}/similar?page=&size=
DELETE /api/artworks/{id}/like                             (unlike)
POST   /api/artworks/like                                  (like, body: { artwork_id })
GET    /api/artworks/all?page=&size=
GET    /api/artworks/by-artist-id/{artistId}?page=&size=
GET    /api/artworks/by-name/{artistName}?page=&size=
GET    /api/artworks/liked?page=&size=
GET    /api/artworks/recommended?page=&size=
GET    /api/artworks/shop?page=&size=
POST   /api/auth/native/oauth                              (apple/google id-token exchange)
POST   /api/auth/native/refresh
POST   /api/auth/native/request-otp
POST   /api/auth/native/sign-out
POST   /api/auth/native/verify-otp
POST   /api/banners/track                                  (impression/click on sponsored banner)
POST   /api/block                                          (block user/artwork)
GET    /api/blocked/users?page=&size=
GET    /api/chatroom_id?user_id=                           (resolve chatroom id for compose)
POST   /api/chatroom_id                                    (same; body: { user_id })
DELETE /api/chatroom/with/{userId}                         (delete chat)
POST   /api/curations/                                     (create)
GET    /api/curations/?page=&size=                         (own)
GET    /api/curations/{id}                                 (detail)
PUT    /api/curations/{id}                                 (edit — artwork list or general fields)
DELETE /api/curations/{id}
DELETE /api/curations/{id}/like
POST   /api/curations/like                                 (body: { curation_id })
GET    /api/events/{id}                                    (V1.9 — popup shape)
GET    /api/exhibitions/                                   (Gallery devices)
POST   /api/exhibitions/                                   (create device)
PUT    /api/exhibitions/{id}                               (update device assignment)
DELETE /api/exhibitions/{id}
POST   /api/feedbacks/                                     (send feedback message)
GET    /api/followed-users?page=&size=
GET    /api/followers?page=&size=
POST   /api/follow                                         (body: { followed_id })
GET    /api/feed/discover                                  (Home tab Discover subtab — mixed feed)
GET    /api/mediums/                                       (medium picker)
POST   /api/messages/                                      (send)
PATCH  /api/messages/{id}                                  (edit)
DELETE /api/messages/{id}
PATCH  /api/messages/{id}/read                             (mark message read)
GET    /api/messages/with/{userId}?page=&size=
POST   /api/me/fcm-token                                   (body: { token })
GET    /api/my/chatrooms                                   (chat list)
GET    /api/notifications
PATCH  /api/notifications/{id}/read
POST   /api/prepare-upload/                                (returns signed filepath for image upload)
GET    /api/profile                                        (own)
POST   /api/profile                                        (create — multipart, used during signup)
GET    /api/profile/{userId}/public/artworks?page=&size=
GET    /api/profile/{userId}/public/curations?page=&size=
GET    /api/profile/{userId}/public/info
GET    /api/profile/{id}                                   (artist info by user id)
GET    /api/profile/username-check?username=
PUT    /api/profile/update                                 (multipart — edit own profile)
GET    /api/profiles/by-invite-code?invite_code=&page=&size=
POST   /api/report-artwork
POST   /api/report-message
GET    /api/search?q=&type=
GET    /api/search/trending-tags
GET    /api/search/users?query=
POST   /api/share                                          (share request to backend for tracking)
POST   /api/subscriptions/restore-apple                    (body: { originalTransactionId })
POST   /api/subscriptions/verify-apple                     (body: signed JWS transaction payload)
POST   /api/unblock
POST   /api/users/{userId}/unfollow
DELETE /api/user/delete-me
POST   /api/verify-invite                                  (body: { code })
POST   /api/waitlist                                       (body: name, email, country/state/city, consents)
```

Response envelope is always:

```json
{ "success": bool, "code": int, "message": str, "data": <typed> }
```

Paginated responses wrap data:

```json
{ "items": [...], "total": int, "page": int, "size": int }
```

Date fields in `data` are ISO 8601 (timestamps with fractional seconds — `2026-04-18T15:59:26.794217`). JSON keys are snake_case (`event_name`, `profile_picture_url`); iOS uses `JSONDecoder.keyDecodingStrategy = .convertFromSnakeCase` to match Swift camelCase property names.

---

## 24. Where to ask if blocked

- **Visual / layout questions** → Figma file (RINX-Prototype-V2)
- **Wire shape / endpoint behavior** → Backend team
- **Tap target / navigation / state behavior** → This doc + iOS source
- **Anything that's still ambiguous** → ping iOS team

End of handout.
