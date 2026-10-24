# Deferred Work

- source_spec: `_bmad-output/implementation-artifacts/spec-1-9-dynamic-state-aware-hero-card-integration-with-action-list.md`
  summary: Persist task snoozes with timestamps to avoid in-memory reset across process recreation
  evidence: Hero card snoozing currently uses an in-memory Set in HomeViewModel; snoozed tasks reset to upcoming when the activity process is killed.
