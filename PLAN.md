# Raez Production-Ready Plan (v2 — 7-Day, Reviewed)

**Goal:** Make this repo something a recruiter respects in a 60-second skim and a 5-minute deep look.
**Stack (locked):** JavaFX `Timeline`/`Canvas` launcher + Cloudinary images + `jpackage` installer.
**v2 changes from review:** Compressed 10 → 7 days. Cut HikariCP (over-engineering for SQLite single-user). Cut premature pagination. Added `jpackage` Windows installer (Day 6) — biggest recruiter signal. Added pre-migration backups, demo Maven profile, troubleshooting README block, secret-scan gate.

**How to use:** Each step has an ID like `D2.3` and a "Tell Claude" prompt. To resume, paste:
> "I'm on PLAN.md step **D2.3**. Read PLAN.md, then do that step."

---

## Day 0 (15 min) — Pre-flight gates

These run once before Day 1. They protect you from shipping something embarrassing.

- [ ] **D0.1** — `git status` clean? If not, commit or stash before starting.
- [ ] **D0.2** — Create branch: `git checkout -b production-ready`
- [ ] **D0.3** — Secret-scan history. **Tell Claude:** *"Run `git log -p -- src/ pom.xml | grep -iE 'password|secret|api[_-]?key|token' | head -50` and report anything that looks like a real credential. If any found, we rotate before pushing."*
- [ ] **D0.4** — Snapshot the "before": run app, take 1 screenshot → `docs/before.png` (create the folder).

---

## Day 1 — Repo Hygiene + Test Skeleton (2 hours)

Tiny day. Just clearing rubble and putting a CI safety net under everything that follows.

- [ ] **D1.1** — Replace `.gitignore` contents with:
  ```
  target/
  *.class
  .idea/
  .vscode/
  *.db-shm
  *.db-wal
  .env
  config.properties
  /raez-local.db
  ~$*
  .DS_Store
  ```
- [ ] **D1.2** — Untrack: `git rm --cached raez.db-shm raez.db-wal && git rm git` (the empty stray file).
- [ ] **D1.3** — Decide on `raez.db`: keep for now, will be replaced by seed script on Day 5.
- [ ] **D1.4** — **Tell Claude:** *"Add JUnit 5 (`org.junit.jupiter:junit-jupiter:5.10.0`) to pom.xml if not present. Create `src/test/java/com/raez/SmokeTest.java` with one passing assertion. Run `mvn test` and confirm green."*
- [ ] **D1.5** — **Tell Claude:** *"Create `.github/workflows/ci.yml`: on push and PR, setup-java 21 Temurin, cache Maven, `mvn -B test`. Don't enable yet — we'll commit this on Day 7."*
- [ ] **D1.6** — Commit: `chore(day1): repo hygiene + test skeleton`

---

## Day 2 — Coded Launcher Animation (4-5 hours)

The headline visible upgrade. Spec it tightly so the result has personality, not generic-tutorial vibes.

**Brand decision (lock before coding):**
- Background: `#0a0e27` (deep navy) — feels intentional, not "white default"
- Wordmark: `#f5f5f5` at 92% opacity — softer than pure white
- Accent: `#e63946` (red, used sparingly for the sweep line and CTA buttons later)
- Font: Segoe UI Light 64px for wordmark (Windows-native, no font shipping needed)
- **Why these:** navy reads "fintech serious," red sweep adds energy, system font keeps app native and fast. These three colors become your app-wide palette.

Steps:

- [ ] **D2.1** — **Tell Claude:** *"Find the current splash code that plays the video. Search for `MediaPlayer`, `Media`, `.mp4`, the `videos/` resource folder. Show me the entry-point class and how the main scene gets shown after the splash."*
- [ ] **D2.2** — **Tell Claude:** *"Create `src/main/java/com/raez/ui/LauncherStage.java`: undecorated transparent Stage, 600x400, centered, hosts a StackPane with rounded background `#0a0e27` (radius 12px). Exposes `void show(Runnable onFinished)`."*
- [ ] **D2.3** — **Tell Claude:** *"Create `src/main/java/com/raez/ui/LogoCanvas.java`: a Canvas-based animator. Build the wordmark 'RAEZ' as 4 Text nodes in an HBox, each starting opacity 0 and scale 0.6. Animate with a ParallelTransition where each letter has a FadeTransition (300ms) and ScaleTransition (300ms) staggered 100ms apart. After the letters finish, an `AnimationTimer` draws a 2px `#e63946` line sweeping left-to-right under the wordmark over 400ms. Total ~1700ms. Fire callback when done. Use both APIs deliberately — comment one line: '// AnimationTimer used here for frame-accurate sweep; transitions used above for declarative letter intros'."*
- [ ] **D2.4** — **Tell Claude:** *"Create `src/main/resources/styles/launcher.css` with the brand palette from PLAN.md. The transparent stage shows the rounded card."*
- [ ] **D2.5** — **Tell Claude:** *"Wire LauncherStage into the Application.start: it shows first; on `onFinished` it closes and the existing main scene is shown via Platform.runLater. Preserve all existing DB init / login flow. Confirm no UI thread violations."*
- [ ] **D2.6** — Run: `mvn javafx:run`. Watch the animation. If it stutters: lower the AnimationTimer work (the sweep should be a single `strokeLine` per frame, not a loop).
- [ ] **D2.7** — **Tell Claude:** *"Once I confirm the launcher works, delete `src/main/resources/videos/` entirely and remove every dead Media/MediaPlayer import. Show me the diff before applying."*
- [ ] **D2.8** — Commit: `feat(ui): coded JavaFX launcher with brand palette`

**Quality gate before moving on:** the animation should feel intentional, not random. If it feels generic, iterate one more time on the timing.

---

## Day 3 — Security Pass: SQL + Passwords + Validation (3-4 hours)

The non-negotiable day. Skip nothing here.

- [ ] **D3.1** — **Tell Claude:** *"Grep `src/main/java` for raw `Statement` (not PreparedStatement) and any SQL built with `+` concat or String.format. List each with file:line. Skip false positives in tests."*
- [ ] **D3.2** — **Tell Claude:** *"For each offender from D3.1, convert to PreparedStatement with `?` placeholders, wrap in try-with-resources (Connection, PreparedStatement, ResultSet). Show me the diff per file, I'll approve in batches."*
- [ ] **D3.3** — **Tell Claude:** *"Review the existing `DBConnection`/`Database` class. If it's a singleton sharing one Connection across threads, that's a bug with WAL. Replace with a class that returns a fresh Connection per call (SQLite + WAL handles this fine). DO NOT add HikariCP — overkill for single-user JavaFX. Add a comment explaining the choice."*
- [ ] **D3.4** — Add jBCrypt: **Tell Claude:** *"Add `org.mindrot:jbcrypt:0.4` to pom.xml. Find every place passwords are stored or compared — show me the auth flow."*
- [ ] **D3.5** — **Tell Claude:** *"Create `com.raez.util.MigratePasswords` with main method:
   1. Backup: write a CSV `~/.raez/backups/users-{timestamp}.csv` of `(id, email, current_password_hash)` BEFORE any change.
   2. For each user where password doesn't start with `$2a$` or `$2b$`: hash with `BCrypt.hashpw(plain, BCrypt.gensalt(12))`, update.
   3. Print `Migrated N users (skipped M already-hashed)`.
   4. Idempotent.
   Use `System.getProperty('user.home')` for the backup path (Windows-safe)."*
- [ ] **D3.6** — Run: `mvn exec:java -Dexec.mainClass=com.raez.util.MigratePasswords`. Verify with DB Browser for SQLite that hashes look like `$2a$12$...`.
- [ ] **D3.7** — **Tell Claude:** *"Update auth: signup uses `BCrypt.hashpw`, login uses `BCrypt.checkpw`. Remove every plaintext comparison."*
- [ ] **D3.8** — **Tell Claude:** *"Create `com.raez.util.Validators` with: `email(s)`, `nonEmpty(s, maxLen)`, `positive(double)`, `positiveInt(int)`. Throw IllegalArgumentException with friendly messages. Call from controllers in signup, product create/edit, order placement."*
- [ ] **D3.9** — Manual smoke test: signup new user, login, place order. Anything broken → fix before moving on.
- [ ] **D3.10** — Commit: `feat(security): PreparedStatements + BCrypt + input validation`

---

## Day 4 — Cloudinary Storage Layer (3 hours)

Build the abstraction. Migration runs Day 5 alongside the seed script.

- [ ] **D4.1** — Sign up at cloudinary.com. From dashboard, copy `cloud_name`, `api_key`, `api_secret`.
- [ ] **D4.2** — Create `%USERPROFILE%\.raez\config.properties` (Windows) with:
  ```
  cloudinary.cloud_name=xxx
  cloudinary.api_key=xxx
  cloudinary.api_secret=xxx
  ```
- [ ] **D4.3** — Create `config.properties.example` in repo root with same keys, blank values, plus a comment: `# Copy to ~/.raez/config.properties (or %USERPROFILE%\.raez\ on Windows). Optional — app falls back to local storage.`
- [ ] **D4.4** — **Tell Claude:** *"Add `com.cloudinary:cloudinary-http44:1.36.0` to pom.xml."*
- [ ] **D4.5** — **Tell Claude:** *"Create:
   - `com.raez.storage.ImageStorage` interface: `String upload(File) throws IOException`, `void delete(String publicId)`, `String getPublicIdFromUrl(String url)`.
   - `CloudinaryImageStorage`: reads config from `Path.of(System.getProperty('user.home'), '.raez', 'config.properties')`. Uploads to folder `raez/products`. Returns `secure_url`. SLF4J-logs each upload at INFO with timing.
   - `LocalImageStorage`: copies to `~/.raez/images/<uuid>.<ext>`, returns `file:///` URL. Used when config is missing OR network upload fails (with a single retry).
   - `ImageStorageFactory.create()`: returns Cloudinary if config exists and reachable (5s probe), else Local. Logs which one and why at startup.
   Wrap network calls in try/catch — never let an upload exception crash the UI thread."*
- [ ] **D4.6** — **Tell Claude:** *"Create `com.raez.storage.TestUpload` with a main that uploads any file from `src/main/resources/images/products/`. Run it. Print the URL. I'll open it in a browser."*
- [ ] **D4.7** — Commit: `feat(storage): Cloudinary + Local fallback abstraction`

---

## Day 5 — Migrate Images + Seed Script + DB Bootstrap (4 hours)

Two related changes batched: move images to cloud, replace committed `raez.db` with a seed script. After today the repo no longer ships any binary data.

- [ ] **D5.1** — **Tell Claude:** *"Add a migration step in `Database` init: `ALTER TABLE products ADD COLUMN image_url TEXT` and `image_public_id TEXT` if missing. Keep the old `image_path` column for now."*
- [ ] **D5.2** — **Tell Claude:** *"Create `com.raez.util.MigrateImagesToCloud` with main:
   1. **Backup first:** write `~/.raez/backups/images-map-{timestamp}.csv` of `(product_id, image_path)` before any DB write.
   2. Load all products with non-null `image_path` and null `image_url`.
   3. For each: resolve the local file (try `src/main/resources/images/products/<path>` first, then absolute), upload via `ImageStorage`, update `image_url` and `image_public_id`.
   4. Print progress `Migrating 12/47: <name>`. Catch per-row exceptions, log, continue. Print summary `Done. N succeeded, M failed.` Idempotent.
   5. If any failed, print the IDs and exit code 1 so I see it."*
- [ ] **D5.3** — Run: `mvn exec:java -Dexec.mainClass=com.raez.util.MigrateImagesToCloud`. Spot-check 3 URLs in browser.
- [ ] **D5.4** — **Tell Claude:** *"Find every place reading `image_path` to build an ImageView. Switch to `image_url` with `new Image(url, true)` (background loading). For thumbnails in list views, inject Cloudinary transform `/c_fill,w_300,h_300,q_auto,f_auto/` after `/upload/`. For detail views, just `/q_auto,f_auto/`. Add a helper `ImageStorage.thumbnail(url, w, h)` so this isn't sprinkled."*
- [ ] **D5.5** — **Tell Claude:** *"Update product create/edit: upload via `ImageStorage.upload()` first, store URL. On edit-replace, delete old via `ImageStorage.delete(oldPublicId)` AFTER the new one is committed (never both pending)."*
- [ ] **D5.6** — Verify: browse products, add new with image, edit existing image. All show Cloudinary URLs.
- [ ] **D5.7** — Drop the old column once verified: **Tell Claude:** *"Add migration: `ALTER TABLE products DROP COLUMN image_path` (SQLite 3.35+). If SQLite version is older, do the table-rebuild dance. Then delete `src/main/resources/images/products/` from the repo."*
- [ ] **D5.8** — **Tell Claude:** *"Create `src/main/resources/db/seed.sql` with full CREATE TABLE schema (matching current state) + CREATE INDEX statements (`orders.customer_id`, `order_items.order_id`, `products.category`, `users.email`) + INSERT demo data: 3 users (with already-hashed BCrypt passwords I'll provide), 10 products with placeholder Cloudinary URLs, 2 orders. Modify `Database` init: if no DB file exists, create from seed.sql."*
- [ ] **D5.9** — Test bootstrap from clean: rename `raez.db` to `raez.db.bak`, run app, confirm fresh DB self-builds. Restore backup if you want your real data back, but this proves the seed works.
- [ ] **D5.10** — Once confirmed: `git rm raez.db`
- [ ] **D5.11** — Commit: `feat: Cloudinary image migration + seed-based DB bootstrap`

---

## Day 6 — Threading + Logging + Tests + Installer (5 hours)

The professional-polish day. Two pieces in here are recruiter gold: real tests + a downloadable `.exe`.

- [ ] **D6.1** — **Tell Claude:** *"Add `org.slf4j:slf4j-api:2.0.x` and `ch.qos.logback:logback-classic:1.4.x` to pom. Create `src/main/resources/logback.xml`: console appender INFO, file appender DEBUG to `${user.home}/.raez/logs/raez.log` rolling 10MB×5 files."*
- [ ] **D6.2** — **Tell Claude:** *"Replace every `System.out.println`, `System.err.println`, and `e.printStackTrace()` in `src/main/java` with proper SLF4J logger calls (private static final Logger log = LoggerFactory.getLogger(...)). Show summary of files changed."*
- [ ] **D6.3** — **Tell Claude:** *"Find every controller method calling a DAO directly from a JavaFX event handler. Wrap each in `javafx.concurrent.Task`, update UI in `setOnSucceeded` via Platform.runLater. Add a small ProgressIndicator overlay that's hidden by default and shown during the task. List every file touched."*
- [ ] **D6.4** — **Tell Claude:** *"Create five JUnit 5 tests, each using an in-memory SQLite (`jdbc:sqlite::memory:`) bootstrapped from `seed.sql`:
   1. `OrderServiceTest.placeOrder_happyPath_persistsAndDecrementsStock`
   2. `OrderServiceTest.placeOrder_insufficientStock_throwsAndDoesNotMutate`
   3. `AuthServiceTest.login_wrongPassword_returnsFailure`
   4. `AuthServiceTest.login_correctPassword_returnsUser`
   5. `ProductDaoTest.findByCategory_returnsOnlyMatching`
   Run `mvn test`. Confirm 5 passing."*
- [ ] **D6.5** — Add `demo` Maven profile: **Tell Claude:** *"Add a `<profile id='demo'>` to pom.xml that sets a system property `raez.storage=local` and `raez.seed=true`. Update `ImageStorageFactory` to honor this. Document: `mvn javafx:run -Pdemo` runs offline with local storage and a fresh seed DB."*
- [ ] **D6.6** — Build the installer: **Tell Claude:** *"Add a Maven profile `installer` using `maven-jpackage-plugin` (or exec-maven-plugin invoking `jpackage`) to produce a Windows `.exe` installer. Configure: app name 'Raez', vendor my name, icon from a 256x256 PNG I'll add at `src/main/resources/icon.png`, main class, JDK module list (`java.sql`, `javafx.controls`, `javafx.fxml`, `javafx.media` if still needed). Output to `target/installer/`. Document the command: `mvn -Pinstaller package`."*
- [ ] **D6.7** — Make a 256x256 icon (use the launcher wordmark on the navy background — even MS Paint export is fine for v1). Save to `src/main/resources/icon.png`.
- [ ] **D6.8** — Run `mvn -Pinstaller package`. Test the produced `.exe` on your machine. Uninstall after.
- [ ] **D6.9** — Commit: `feat: logging, background tasks, JUnit tests, demo profile, jpackage installer`

---

## Day 7 — README + GIF + Release + Merge (3-4 hours)

The recruiter-facing day. Everything before this was internal. This is the storefront.

- [ ] **D7.1** — Install ScreenToGif (free).
- [ ] **D7.2** — Record demo GIF, 6-8s, 15fps: launcher animation → main screen → one feature interaction (add to cart or admin product create with image upload). Save to `docs/demo.gif`. Keep under 5 MB.
- [ ] **D7.3** — Take 3 fresh screenshots, same DPI, same window size: `docs/screen-products.png`, `docs/screen-cart.png`, `docs/screen-admin.png`. Crop consistently.
- [ ] **D7.4** — **Tell Claude:** *"Rewrite README.md exactly:
   1. H1 'Raez' + one-line tagline (suggest 3 options).
   2. `![demo](docs/demo.gif)` directly under.
   3. CI status badge from GitHub Actions + license badge.
   4. ## What it is — 2 sentences.
   5. ## Features — bullet list of what's actually built.
   6. ## Tech stack — table | Layer | Choice | Why | for: UI/JavaFX 21/native + zero-dep, DB/SQLite + WAL/zero-config single-user, Images/Cloudinary/on-the-fly transforms, Auth/BCrypt/standard, Build/Maven, Tests/JUnit 5, Installer/jpackage/native .exe.
   7. ## Architecture — 5-line bullet list of packages.
   8. ## Run it
      ```
      git clone <url>
      cd raez-ecommerce-app-main
      mvn -Pdemo javafx:run    # offline, no Cloudinary needed
      ```
      Plus a section for full mode with config.properties.
   9. ## Download — link to GitHub Release with the `.exe` (we'll add the release after merging).
   10. ## Screenshots — 3 PNG embeds.
   11. ## Engineering case study — short 'before/after' table: startup time, JAR size, repo size, image load latency. Numbers I'll fill in.
   12. ## What I'd build next — 4 bullets showing judgment: 'Postgres swap for multi-user', 'Stripe checkout', 'REST API extraction', 'native macOS/Linux installers'.
   13. ## Troubleshooting — collapsible block: 'Cloudinary upload fails' → uses local fallback automatically; 'JDK 21 not found' → install Temurin; 'mvn javafx:run hangs on Windows' → use `-Pdemo`.
   14. Footer: my name + LinkedIn link.
   No fluff."*
- [ ] **D7.5** — Fill in the case-study numbers. Easy to measure: `Measure-Command { mvn javafx:run }` (kill app after launcher), `(Get-Item target/raez-*.jar).Length`, `git count-objects -vH`.
- [ ] **D7.6** — Add `LICENSE` (MIT, GitHub UI generates it).
- [ ] **D7.7** — Enable CI: the `.github/workflows/ci.yml` from Day 1 is already in repo — just make sure it's committed.
- [ ] **D7.8** — Final review pass: **Tell Claude:** *"List unused imports, unused private methods, leftover TODO comments, commented-out code blocks. I'll cherry-pick fixes."*
- [ ] **D7.9** — Final smoke: `mvn clean test` passes, `mvn -Pdemo javafx:run` works, `mvn -Pinstaller package` produces .exe.
- [ ] **D7.10** — Merge to main:
   ```
   git checkout main
   git merge --no-ff production-ready
   git push origin main
   ```
- [ ] **D7.11** — Delete the old branch on GitHub: `git push origin --delete final-integrated-version`. Locally too if it exists.
- [ ] **D7.12** — Cut a GitHub Release: tag `v1.0.0`, attach the `.exe` from `target/installer/`. Release notes: short, recruiter-readable.
- [ ] **D7.13** — Pin the repo on your GitHub profile.
- [ ] **D7.14** — Open repo in incognito. Read README as a stranger. Click around. If it looks like real engineering, you're done.

---

## Cut from v1 (and why)

| Cut | Why |
|---|---|
| HikariCP connection pool | Single-user JavaFX app on local SQLite. Pooling is theater, experienced reviewers will flag it as resume-padding. Fresh `Connection` per call with SQLite WAL is correct here. |
| Pagination on product list | YAGNI. With 47 demo products no one will notice. Add only if you scale demo data past 500 rows. |
| Spending a full day on perf | Lazy image loading + indexes are folded into Day 5/6. A separate "perf day" was busywork. |
| 10-day timeline | 7 is honest. Days 1 and 7 are short. The work fits if you don't get stuck on Day 2 or Day 5. |

## Added in v2

| Added | Why |
|---|---|
| Day 0 secret-scan | Catches accidental key commits before going public. One command, huge downside protection. |
| Pre-migration CSV backups (D3.5, D5.2) | One-way migrations without a rollback path are unprofessional. CSV is enough — no need for transactions across an HTTP boundary. |
| `demo` Maven profile (D6.5) | Recruiter clones repo, has no Cloudinary account → app works offline. Removes the #1 onboarding failure. |
| `jpackage` installer (D6.6-D6.8) | A double-clickable `.exe` on a GitHub Release is the highest-signal portfolio artifact you can ship. Most JavaFX student projects stop at "run with Maven." This is the gap. |
| Brand palette decided up front (Day 2) | "Use blue and red" produces generic results. Locking `#0a0e27 / #f5f5f5 / #e63946` with reasoning means the launcher, buttons, and screenshots all match. |
| Engineering case study in README (D7.4 #11) | Recruiters read tables. Showing measured before/after numbers proves you reason about engineering, not just write code. |
| Troubleshooting block (D7.4 #13) | Tells reviewers you've thought about the user. Costs 10 minutes, avoids the "doesn't run on my machine" rejection. |

---

## Recovery rules

- **Stuck more than 90 minutes on a step:** message me with the error. Don't grind.
- **Cloudinary signup fails or you don't want to:** D4 → just `LocalImageStorage`. Skip D5.2-D5.3 cloud upload. Days 6-7 still work; remove Cloudinary from the README tech-stack table.
- **`jpackage` fails:** ship without it. Keep the JAR + `mvn -Pdemo` instructions. Note in README "native installer planned."
- **Only have 4 days:** do Days 0, 1, 2, 3, 7. Cut images and tests. Launcher + security + README is still a strong portfolio piece.

---

## Tell-Claude resume preamble

Top of any new session, paste this:
> "I'm working through PLAN.md (v2). I'm on step **D{X.Y}**. Read PLAN.md for context, then execute that step. If anything is ambiguous, ask before changing files."
