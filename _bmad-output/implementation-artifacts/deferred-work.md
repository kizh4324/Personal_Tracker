# Deferred Work

- source_spec: `_bmad-output/implementation-artifacts/spec-1-9-dynamic-state-aware-hero-card-integration-with-action-list.md`
  summary: Persist task snoozes with timestamps to avoid in-memory reset across process recreation
  evidence: Hero card snoozing currently uses an in-memory Set in HomeViewModel; snoozed tasks reset to upcoming when the activity process is killed.
- source_spec: `_bmad-output/implementation-artifacts/spec-1-10-core-task-management-delivery-intensity-classification.md`
  summary: Add optional secondary confirmation or undo window for direct task deletions in TaskEditorSheet
  evidence: Task deletion in TaskEditorSheet immediately invokes DeleteTaskUseCase and emits a snackbar notification; adding a 5s bottom undo toast or modal confirmation will offer an additional guard against accidental tap.
