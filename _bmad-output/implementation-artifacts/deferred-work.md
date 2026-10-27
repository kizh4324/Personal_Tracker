# Deferred Work

- source_spec: `_bmad-output/implementation-artifacts/spec-1-9-dynamic-state-aware-hero-card-integration-with-action-list.md`
  summary: Persist task snoozes with timestamps to avoid in-memory reset across process recreation
  evidence: Hero card snoozing currently uses an in-memory Set in HomeViewModel; snoozed tasks reset to upcoming when the activity process is killed.
- source_spec: `_bmad-output/implementation-artifacts/spec-1-10-core-task-management-delivery-intensity-classification.md`
  summary: Add optional secondary confirmation or undo window for direct task deletions in TaskEditorSheet
  evidence: Task deletion in TaskEditorSheet immediately invokes DeleteTaskUseCase and emits a snackbar notification; adding a 5s bottom undo toast or modal confirmation will offer an additional guard against accidental tap.
- source_spec: `_bmad-output/implementation-artifacts/spec-2-2-deterministic-kotlin-regex-rule-nlp-parser.md`
  summary: Support complex multi-word date phrases (e.g. 'October 26th', '26 Oct') in local regex rules
  evidence: Deterministic NLP currently focuses on ISO-8601, relative days, and weekdays; broad conversational date formats will be expanded during Story 2.3 Gemini Live multimodal parser integration.
- source_spec: `_bmad-output/implementation-artifacts/spec-3-3-emergency-telecom-dynamic-whitelist-bypass.md`
  summary: FocusTimerPolicy persistence across device reboot and process death
  evidence: FocusTimerPolicy is currently maintained via in-memory StateFlow and service companion object; persistent settings baseline is scheduled in Story 6.4 (6-4-app-settings-management-local-privacy-baseline).
