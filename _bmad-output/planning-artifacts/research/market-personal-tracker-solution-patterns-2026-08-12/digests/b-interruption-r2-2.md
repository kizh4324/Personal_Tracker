# Digest — B (interruption) — r2 (platform enforcement, mid-2026 reality)

Accessed: 2026-08-12.

## CLAIMS
1. **iOS 26 App Limits now accept 0 minutes = a true full block** (previously min was 1 min). Confirmed by three independent post-release sources; Apple's own support doc page could not be fetched directly (page slug redirects to guide TOC).
   - source: techlockdown.com/articles/ios-26-screen-time-changes · publisher: Tech Lockdown · pub_date: 2026-04-20 · accessed: 2026-08-12 · confidence: **high** · class: platform
   - source: timingapp.com/blog/screen-time-on-iphone-and-ipad/ · publisher: Timing App · pub_date: 2026-04-13 · accessed: 2026-08-12 · confidence: **high** · class: platform
   - source: thecap.beehiiv.com/p/make-one-more-minute-non-negotiable · publisher: The Cap (Cat & Nat) · pub_date: 2026-01-14 · accessed: 2026-08-12 · confidence: medium (newsletter register) · class: platform
2. **"One More Minute" still exists in current iOS and cannot be disabled in Settings** — it was NOT removed or made fully non-negotiable. An Apple Community moderator thread answers the "can I disable it?" question with "this option cannot be disabled."
   - source: discussions.apple.com/thread/250882762 · publisher: Apple Support Communities · pub_date: n.d. (post-iOS16, still current) · accessed: 2026-08-12 · confidence: **high** · class: platform
3. **iOS 26's "One Minute" extension is still present and currently buggy** — granting it during Downtime spills the minute across all app categories, locking the device; unfixed through 26.0.1 per user reports.
   - source: macobserver.com/tips/how-to/ios-screen-time-issue-one-minute-setting-affects-all-apps-on-iphone/ · publisher: The Mac Observer · pub_date: 2025-11-20 · accessed: 2026-08-12 · confidence: high · class: platform
   - source: discussions.apple.com/thread/256137598 · publisher: Apple Community · pub_date: 2025 (iOS 26 era) · accessed: 2026-08-12 · confidence: high (multiple users) · class: platform
4. **Screen Time limits are IGNORE-ABLE by default** — Apple's own doc states this; "Block at End of Limit" only appears once a Screen Time passcode is set, and even then the user can tap Ignore Limit (add 15 min or skip rest of day) unless the passcode is not theirs.
   - source: support.apple.com/guide/iphone/set-schedules-with-screen-time-iphb0c7313c9/ios · publisher: Apple (primary) · pub_date: current (iOS 26 guide) · accessed: 2026-08-12 · confidence: **high** · class: platform
   - source: techlockdown.com/articles/fix-ignore-limit-screen-time · publisher: Tech Lockdown · pub_date: 2026-06-04 · accessed: 2026-08-12 · confidence: high · class: platform
5. **iOS 26.4 now requires the Screen Time PIN (not the iPhone passcode/Face ID) to revoke a third-party app's Screen Time permission** — hardens app-based blockers on the adult's own device; the strongest single-user iOS improvement found.
   - source: techlockdown.com/articles/ios-26-screen-time-changes · publisher: Tech Lockdown · pub_date: 2026-04-20 · accessed: 2026-08-12 · confidence: **medium** (single secondary) · class: platform
6. **Google Pause Point announced 2026-05-12**: 10-second "Why am I here?" pause, breathing/timer/photo/substitute-app suggestions, opt-out requires a phone restart.
   - source: blog.google/products-and-platforms/platforms/android/pause-point/ · publisher: Google (primary) · pub_date: 2026-05-12 · accessed: 2026-08-12 · confidence: **high** · class: platform
7. **Pause Point has NOT shipped as of mid-Aug 2026.** Android 17 stable dropped June 2026 and the June Pixel Feature Drop listed many features but not Pause Point; both sources state it's expected "later this year" via a future update/Feature Drop.
   - source: androidauthority.com/how-to-replicate-android-17-pause-point-right-now-3687961/ · publisher: Android Authority · pub_date: 2026-07-19 · accessed: 2026-08-12 · confidence: **high** · class: platform
   - source: techcabal.com/2026/08/07/how-pause-point-on-android-17-works-and-why-it-matters/ · publisher: TechCabal · pub_date: 2026-08-07 · accessed: 2026-08-12 · confidence: high · class: platform
   - (corroborating absence: androidheadlines.com/2026/06/16/... June Feature Drop list omits it) · publisher: Android Headlines · 2026-06-16 · accessed: 2026-08-12
8. **kSafe-style timer lockboxes are genuinely override-proof by design** — no reset, no code, no backdoor; only escape is brute force that destroys the box. No cheating option exists.
   - source: lockboxtimer.com/ksafe-lock-box-review/ · publisher: Lockbox Timer (independent 4-week test) · pub_date: 2025-08-03 · accessed: 2026-08-12 · confidence: **high** · class: product
9. **Independent press evidence on lockboxes is experience-based, not experimental** — Wirecutter (tested 3, "short bursts helped my productivity") but cautions lockboxes are a last resort that fail "unless you have done the intention-setting... work first"; Business Insider's 2-year user reports it "does a good job of limiting my mindless phone use."
   - source: nytimes.com/wirecutter/reviews/break-up-with-your-phone/ · publisher: NYT Wirecutter · pub_date: 2024-02-06 · accessed: 2026-08-12 · confidence: high (review, not a study) · class: evidence
   - source: businessinsider.com/guides/home/ksafe-review · publisher: Business Insider · pub_date: 2023-12-27 · accessed: 2026-08-12 · confidence: medium · class: evidence
10. **NFC hardware blockers (Blok/Brick/Bloom/Unpluq) are NOT override-proof** — every one ships a built-in emergency exit (Brick 5/mo, Bloom 3, Blok 3 free then $4.99 each), and Android-side blocking inconsistency is commonly reported. They add friction, not an unbreakable lock.
    - source: accountableai.xyz/blog/best-nfc-phone-blockers-2026 · publisher: Accountable AI (independent comparison) · pub_date: 2026-01-19 · accessed: 2026-08-12 · confidence: **medium** · class: product
    - (contrast/vendor register: blog.blok.so/p/how-is-blok-different... claims "harder to bypass" + "+83% screen time" with no data — flag as marketing)

## VERDICTS
- **Q1 — Apple 0-minute App Limit / "One More Minute" in iOS 26** → **proven / mixed** → A 0-minute per-app full block is confirmed (3 independent 2026 sources, incl. hands-on). "One More Minute" was NOT removed and cannot be disabled; it still exists, is currently buggy in iOS 26, and the real single-user win is that third-party blockers now require the Screen Time PIN (iOS 26.4) to be disabled.
- **Q2 — Google Pause Point shipped?** → **not shipped** → Announced 2026-05-12 (Google blog, primary) but confirmed absent from the Android 17 initial rollout (June 2026) and the June Pixel Feature Drop; two independent sources (Android Authority 07/19, TechCabal 08/07) both say "later this year." No user-reaction data exists because it isn't live.
- **Q3 — Do hardware locks materially beat software bypasses?** → **mixed** → Timer lockboxes (kSafe) are the only self-imposed option that is truly unbypassable within a session, and independent reviewers confirm short-term productivity wins — but evidence is anecdotal/experience-based, no controlled studies, and NFC cards (Blok/Brick/etc.) do NOT beat software (all have built-in paid/limited emergency exits). Hardware wins on override-resistance, not on proven outcomes.
- **Q4 — Best-in-class pattern for a single-user self-enforced limit** → **proven** (synthesis) → Accept that pure software enforced on the user's own device is always overridable (Apple's own doc: limits are ignore-able by default). Realistic best-in-class: (a) external commitment — passcode/allow-list held by a second party (accountability partner / Family Sharing organizer), the only software path that survives the user's own override attempts; (b) escalation warning→full block at limit, plus deliberate opt-out friction (cool-down, expensive/unblockable confirm) to buy reflection time — the Pause Point restart-required design is the OS-level validation of this; (c) optional hardware pairing (timer lockbox for hard sessions) as the override-proof add-on; (d) default to override-with-friction as the honest design contract, and exploit iOS 26.4's Screen Time-PIN protection so third-party blockers can't be silently revoked.

## LEADS
- Re-check Pause Point via Pixel Feature Drop / Digital Wellbeing in Sept–Oct 2026 — Google said "later this year"; TechCabal (08/07) is the freshest tracker.
- Verify the iOS 26.4 "Screen Time PIN required to revoke permissions" claim against Apple release notes (iOS 26.4) — currently one secondary source (Tech Lockdown).
- Apple's "What's new in iOS 26" page (support.apple.com/guide/iphone/iphfed2c4091/ios) is the right primary for a direct 0-minute-limit citation; the specific App Limits page slug kept redirecting to the guide TOC this run.
- Pull Apple's own "Set schedules with Screen Time" page (iphb0c7313c9) for the exact current wording on "limits can be ignored once reached."
- MacObserver's "One Minute" bug coverage is a live thread to watch — it's the same feature, new bug, and will change the answer again.

## NOT FOUND
- Apple primary support-doc text confirming the 0-minute App Limit (direct page not retrievable; relied on 3 secondary sources).
- Any peer-reviewed/controlled evidence that hardware lockboxes improve self-imposed screen-time outcomes (only experience reviews: Wirecutter, Business Insider, Lockbox Timer, YouTube).
- Any independent corroboration of The Cap's specific "one-more-minute is less negotiable" mechanism (only the newsletter's own hands-on claim; Tech Lockdown's PIN change partially corroborates).
- Any user-reaction data for Pause Point (feature unshipped).
