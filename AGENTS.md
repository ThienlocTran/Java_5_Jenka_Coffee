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

## Cloudinary Safety
- Development uploads should use `jenka/dev/...` folders.
- Production uploads should use `jenka/prod/...` folders.
- Do not delete `jenka/prod` assets during dev cleanup.
- Do not move or rewrite existing Cloudinary URLs without a separate migration/cleanup plan.
- Do not delete assets by URL guessing.
- Cleanup must use `publicId` and must check whether another record still references the asset.

## UTF-8 And Vietnamese Text
- All source files must be UTF-8.
- Preserve Vietnamese accents.
- After touching Vietnamese text, search touched files for mojibake patterns: `Ãƒ`, `Ã„`, `Ã†`, `Ã¡Âº`, `Ã¡Â»`, `ï¿½`.
- Do not commit if mojibake is found in touched files.
- Backend validation/error messages must be readable Vietnamese.
- Prefer existing message style; do not mass rewrite all messages.

## Ponytail Boundaries
- Do not add dependencies unless clearly justified.
- Do not copy the Ponytail repo into this repo.
- Do not add plugin files, hooks, or scripts from Ponytail to application source.
- If using the actual Codex plugin, install it in Codex separately, not as app code.
