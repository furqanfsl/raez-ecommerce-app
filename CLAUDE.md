# gstack

Use the `/browse` skill from gstack for **all** web browsing. Never use `mcp__claude-in-chrome__*` tools.

## How to pick a skill (decision tree)

When the user describes intent in plain English, map it to a skill instead of asking which one to run:

- **Exploring an idea, not sure if worth building** → `/office-hours`
- **Have a plan/spec, want it stress-tested before coding** → `/autoplan` (or individual: `/plan-ceo-review`, `/plan-eng-review`, `/plan-design-review`, `/plan-devex-review`)
- **Want a design system / brand foundation** → `/design-consultation`
- **Want to see UI variants** → `/design-shotgun`
- **Approved a design, turn it into HTML** → `/design-html`
- **Site looks off, needs visual polish** → `/design-review`
- **Something is broken / unexpected error** → `/investigate` (root-cause first, no fixes without it)
- **"Does this work?" / feature ready to test** → `/qa` (test + fix) or `/qa-only` (report only)
- **About to merge, want a code review** → `/review`
- **Want a second opinion from another model** → `/codex`
- **Security check** → `/cso`
- **Ready to push / open PR** → `/ship`
- **PR open, merge + verify prod** → `/land-and-deploy`
- **Watch prod after deploy** → `/canary`
- **Perf regression check** → `/benchmark`
- **Update docs after shipping** → `/document-release`
- **Weekly recap / what did we ship** → `/retro`
- **Touching prod / want safety rails** → `/careful`, `/freeze`, or `/guard`
- **Need to log into a site for QA** → `/setup-browser-cookies`
- **Configure deploy platform once** → `/setup-deploy`

If multiple skills could fit, prefer the planning skill before the building skill (e.g. `/autoplan` before writing code, `/review` before `/ship`).

## Available skills

- `/office-hours` — YC-style forcing questions / brainstorm before code
- `/plan-ceo-review` — founder-mode plan review (scope, ambition)
- `/plan-eng-review` — eng-manager plan review (architecture, edge cases)
- `/plan-design-review` — designer's-eye plan review (interactive scoring)
- `/plan-devex-review` — DX plan review (APIs, CLIs, SDKs, docs)
- `/design-consultation` — propose a design system, write DESIGN.md
- `/design-shotgun` — generate multiple AI design variants for comparison
- `/design-html` — finalize approved designs as production HTML/CSS
- `/design-review` — live visual QA on the running site (fixes issues)
- `/devex-review` — live DX audit using the browse tool
- `/review` — pre-landing PR review (SQL safety, trust boundaries, etc.)
- `/codex` — Codex CLI second opinion (review / challenge / consult)
- `/cso` — security audit (OWASP, STRIDE, supply chain)
- `/autoplan` — run CEO + eng + design + DX plan reviews back-to-back
- `/qa` — systematic QA test + fix loop
- `/qa-only` — QA report only, no fixes
- `/browse` — fast headless browser for testing/dogfooding
- `/connect-chrome` — launch visible AI-controlled Chromium with sidebar
- `/setup-browser-cookies` — import real-browser cookies into the headless session
- `/canary` — post-deploy monitoring (errors, perf regressions)
- `/benchmark` — page-load / Core Web Vitals regression detection
- `/ship` — bump VERSION, changelog, commit, push, open PR
- `/land-and-deploy` — merge PR, wait for CI, verify prod
- `/setup-deploy` — configure deploy platform for `/land-and-deploy`
- `/setup-gbrain` — install gbrain CLI + local brain + MCP registration
- `/document-release` — sync docs (README, ARCHITECTURE, CHANGELOG) post-ship
- `/retro` — weekly engineering retrospective
- `/investigate` — root-cause debugging (no fixes without root cause)
- `/learn` — review/search/prune project learnings
- `/careful` — warn before destructive commands
- `/freeze` — restrict edits to a single directory
- `/guard` — `/careful` + `/freeze` combined
- `/unfreeze` — clear the freeze boundary
- `/gstack-upgrade` — upgrade gstack to latest
