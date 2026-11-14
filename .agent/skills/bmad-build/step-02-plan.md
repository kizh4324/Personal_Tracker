# Step 2: Plan

## RULES

- **Language** — Speak in `{{.communication_language}}`. Write any file output in `{{.document_output_language}}`.
- No intermediate approvals.
- **Elephant-Goldfish Model (EGM) Integration:**
  - **Role 1: The Elephant (Context-Rich Spec Designer):** Holds full project context (`docs/okf/index.md`, `ARCHITECTURE-SPINE.md` AD-1 to AD-8, `.harness2/EXPERIENCE.md`, and `HANDOFF.md`) to draft story specifications under a strict "No Code During Design" rule (`/egm:egm-brainstorm` & `/egm:egm-draft`). Never write, modify, or patch production Kotlin code during design.
  - **Role 2: The Goldfish Swarm (Stateless Zero-Context Reviewers):** Before implementation begins, dispatch fresh, zero-history subagents (`/egm:egm-review` / `/egm:egm-review-loop`) that receive ONLY `{spec_file}` and referenced Kotlin files (zero parent conversation history) to execute the 3-stage pre-implementation EGM gate:
    a) *Comprehension Check (`egm-comprehension`)*: Stateless Goldfish summarizes the spec's architecture and MVI data flow back to the Elephant to prove zero ambiguity.
    b) *Antagonistic Review (`egm-design`, `egm-codebase-details`)*: Skeptical Goldfish reviewers probe for AD-1 domain purity leaks, AD-4 punitive copy, AD-7 raw hex/dp token violations, concurrency races, and edge cases.
    c) *Implementation Readiness Go/No-Go Check (`egm-implementation-readiness`)*: Verifies exact file paths, Kotlin data structures, MVI `UiState`/`UiIntent` signatures, and test targets are 100% unambiguous.

## INSTRUCTIONS

1. Draft resume check. If `{spec_file}` exists with `status: draft`, read it and capture the verbatim `<frozen-after-approval>...</frozen-after-approval>` block as `preserved_intent`. Otherwise `preserved_intent` is empty.
2. Investigate codebase (The Elephant Context Loading). Load `docs/okf/index.md`, `ARCHITECTURE-SPINE.md` (AD-1 to AD-8), `.harness2/EXPERIENCE.md`, and `HANDOFF.md`. _Isolate deep exploration in synchronous subagents/tasks where available. To prevent context snowballing, instruct subagents to give you distilled summaries only._ Decide which findings actually matter for execution — the specific files, symbols/lines, reuse points, and read-only constraints — and carry those forward for the Code Map. This is where the investigation lands: the spec preserves it so it is never re-narrated to the implementer at dispatch time.
3. Read `[[bmad-snapshot:spec-template.md]]` fully. Fill it out based on the intent and investigation under the strict "No Code During Design" rule, resolving the template's `date` field to the current system date. Drain the investigation into the `## Code Map` section — annotated paths, symbol/line anchors, reuse pointers, and read-only evidence — so the spec is the implementer's investigation map and the step-03 handoff need only point at it. Concept briefs and preliminary design exploration go into `docs/designs/` (`/egm:egm-brainstorm` & `/egm:egm-draft`). If `preserved_intent` is non-empty, replace the `<frozen-after-approval>` block in the spec you just filled out with `preserved_intent`, before writing. Write the result to `{spec_file}`.
4. EGM 3-Stage Pre-Implementation Gate. Spawn fresh stateless zero-context subagents (`/egm:egm-review` / `/egm:egm-review-loop`) passing ONLY `{spec_file}` and referenced Kotlin files:
   - **a) Comprehension Check:** Confirm the Goldfish accurately reflects architecture, domain boundaries, and MVI flow without hallucination or confusion.
   - **b) Antagonistic Review:** Eliminate AD-1 domain leaks, AD-4 punitive phrasing, AD-7 token violations, and concurrency pitfalls.
   - **c) Implementation Readiness:** Confirm 100% unambiguous file paths, signatures, and test targets before proceeding.
5. Self-review against READY FOR DEVELOPMENT standard.
6. If intent gaps exist, do not fantasize, do not leave open questions, HALT and ask the human.
7. Token count check (see SCOPE STANDARD). If spec exceeds 1600 tokens:
   - Show user the token count.
   - HALT and ask human: `[S] Split — carve off secondary goals` | `[K] Keep full spec — accept the risks`
   - On **S**: Propose the split — name each secondary goal. For each deferred goal, append one new entry to `{{.implementation_artifacts}}/deferred-work.md` using this format. Do not modify existing entries or look for duplicates. Rewrite the current spec to cover only the main goal — do not surgically carve sections out; regenerate the spec for the narrowed scope. Continue to checkpoint.
     ```markdown
     - source_spec: `{spec_file}`
     - summary: <one sentence naming the deferred goal>
       evidence: <why this was split from the current spec>
     ```
   - On **K**: Continue to checkpoint with full spec.

### CHECKPOINT 1

Present summary. Display the spec file path as a CWD-relative path (no leading `/`) so it is clickable in the terminal. If token count exceeded 1600 and user chose [K], include the token count and explain why it may be a problem. Note that the EGM 3-Stage Goldfish Swarm Gate passed.

After presenting the summary, display this note:

---

Before approving, you can open the spec file in an editor or ask me questions and tell me what to change. You can also use `bmad-advanced-elicitation`, `bmad-party-mode`, or `bmad-code-review` skills, ideally in another session to avoid context bloat.

---

HALT and ask human: `[A] Approve` | `[E] Edit`

- **A**: Re-read `{spec_file}` from disk.
  - **If the file is missing:** HALT. Tell the user the spec file is gone and STOP — do not write anything to `{spec_file}`, do not set status, do not proceed to Step 3. Nothing below this point runs.
  - **If the file exists:** Compare the content to what you wrote. If it has changed since you wrote it, acknowledge the external edits — show a brief summary of what changed — and proceed with the updated version. Then set status `ready-for-dev` in `{spec_file}`. Everything inside `<frozen-after-approval>` is now locked — only the human can change it. → Step 3.
- **E**: Apply changes, then return to CHECKPOINT 1.

## NEXT

Read fully and follow `[[bmad-snapshot:step-03-implement.md]]`
