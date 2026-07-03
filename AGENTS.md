# AGENTS.md

## Jenka Production Safety
- Jenka Coffee is a deployed production business website with real data.
- `main` is production. Do not work on `main`, switch to `main`, or commit on `main`.
- Backend branch for this repo: `features`.
- Do not push. Commit locally only after validation passes.
- Preserve unrelated dirty files. Stage only files relevant to the requested task.
- Do not run destructive DB, Docker, or volume commands.
- Do not create destructive migrations.
- Do not add runtime dependencies unless clearly justified and approved.
- Do not touch env, Docker, deploy, lockfile, migration, or production config files without explicit approval.
- For backend changes, validate with `.\mvnw.cmd -DskipTests package` unless the change is docs/guidance only.
- Always run `git diff --check` before commit.

## Ponytail / YAGNI Coding Rule
- Ponytail is guidance only for coding style and scope control.
- Prefer the shortest safe diff.
- Reuse existing code before adding new code.
- Prefer native browser, standard library, and existing framework features before new dependencies.
- Avoid speculative abstractions, generic frameworks, or future-proofing not needed for the task.
- Delete or simplify over-engineering when safe and in scope.
- Do not rewrite large areas without need.

## Ponytail Must Not Simplify Away
- Production data safety.
- Migration safety.
- Security.
- Auth and authorization checks.
- Input validation at trust boundaries.
- Error handling that prevents data loss.
- Accessibility basics.
- Existing business rules.
- Tests and build validation for non-trivial changes.

## Ponytail Boundaries
- Do not add dependencies unless clearly justified.
- Do not copy the Ponytail repo into this repo.
- Do not add plugin files, hooks, or scripts from Ponytail to application source.
- If using the actual Codex plugin, install it in Codex separately, not as app code.
