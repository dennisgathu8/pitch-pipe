# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please report it responsibly:

- **Email:** dennis.gathu@protonmail.com
- **Subject line:** `[SECURITY] pitch-pipe — <brief description>`
- **Response time:** I will acknowledge receipt within 48 hours and provide an initial assessment within 7 days.

Please do **not** open a public GitHub issue for security vulnerabilities.

## Security Posture

### Network exposure
- **No network listeners.** pitch-pipe reads local JSON files and writes to stdout. It makes no outbound HTTP requests in default mode.
- **nREPL binds to localhost only.** The `:repl-options {:host "127.0.0.1"}` setting in `project.clj` ensures the development REPL is never exposed on public interfaces.

### Configuration and secrets
- All configuration is loaded from environment variables or `config.edn` via the [environ](https://github.com/weavejester/environ) library.
- No API keys, tokens, or credentials are used by this project.
- The `.gitignore` excludes `.env`, `*.secret`, `.creds/`, and `secrets/` to prevent accidental secret commits.

### Data validation
- Every JSON event loaded from disk is validated against a `clojure.spec.alpha` schema before entering the processing pipeline.
- Malformed events throw descriptive `ex-info` errors with `:type :pitch-pipe/spec-violation` — never silent `nil` propagation or `NullPointerException`.

### Dependency Scanning

Dependencies are scanned using nvd-clojure 5.2.0 against the
NIST National Vulnerability Database. Scanning runs on every
push via GitHub Actions. The NVD API key is stored as a GitHub
repository secret (NVD_API_TOKEN) and never committed to source.

**Known CVE Disposition — as of 2026-03-31:** No known CVEs in
project dependencies. This section will be updated if future
scans report findings.

## StatsBomb Data Licence Obligations

This project uses [StatsBomb open data](https://github.com/statsbomb/open-data) under the [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International licence](https://creativecommons.org/licenses/by-nc-sa/4.0/).

- The data may be used for **non-commercial purposes only**.
- Any derivative work must attribute StatsBomb as the data source.
- Test fixtures in this repository use **synthetic data** — no real player biometric or health data is included.
