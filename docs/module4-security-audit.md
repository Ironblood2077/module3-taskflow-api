# Module 4 - Security Audit (OWASP Dependency-Check)

This document records the dependency vulnerability audit performed for NTU
Module 4 **Stretch Goal 5 - Security Checks / Dependency Scanning**.

> **No remediation was performed.** This is an audit and triage only. No
> dependency, source, Dockerfile, CI or deployment change was made as a result
> of this scan. The scan completed, but it does **not** certify the
> application as vulnerability-free.

## 1. Objective

- Scan every Maven dependency of the TaskFlow API against known
  vulnerabilities (CVEs).
- Determine, for each finding, whether the vulnerable code is shipped in the
  deployable JAR and whether TaskFlow actually uses the affected feature.
- Identify false positives, open questions and the smallest supported fixes.
- Record what the scanner could **not** see.

## 2. Scan summary

| Item | Value |
|------|-------|
| Scanner | OWASP Dependency-Check Maven plugin **13.0.0** |
| Scan date | 28 October 2026 (report generated 2026-10-28T05:40:42Z) |
| Commit scanned | `c7a428a` (`main`) - Spring Boot 4.1.1, Spring AI 2.0.0, Java 21 |
| Vulnerability data | NVD API; last checked 2026-10-28 13:40 (UTC+8), data last modified 2026-10-28T05:16:59Z |
| Status | **Completed** - NVD database initialised and all dependencies analysed |
| Report formats | HTML and JSON (`target/dependency-check-report.html`, `target/dependency-check-report.json`, not committed) |
| Dependencies analysed | **52** |
| Vulnerability associations | **23** (one CVE is reported against two Kotlin JARs) |
| Unique CVEs | **22** |

Severity as reported by the scanner (NVD CVSS v3 unless stated):

| Severity | Associations | Unique CVEs |
|----------|-------------:|------------:|
| Critical | 8 | 7 |
| High | 8 | 8 |
| Medium | 7 | 7 |
| **Total** | **23** | **22** |

Findings by affected JAR:

| JAR | Associations |
|-----|-------------:|
| `tomcat-embed-core-11.0.24.jar` | 11 |
| `spring-ai-model-2.0.0.jar` | 7 |
| `kotlin-stdlib-common-1.9.10.jar` | 2 |
| `kotlin-stdlib-2.3.21.jar` | 1 |
| `netty-transport-4.2.17.Final.jar` | 1 |
| `spring-boot-devtools-4.1.1.jar` | 1 |

## 3. Methodology

1. Ran the Dependency-Check `check` goal from the command line (the plugin is
   **not** added to `pom.xml` or to the normal `verify` lifecycle).
2. Validated the JSON report structure and recounted all findings from it.
3. Traced every flagged JAR with `./mvnw dependency:tree` (direct vs
   transitive, and which dependency introduces it).
4. Listed `BOOT-INF/lib/` of the built Spring Boot JAR to see which libraries
   are actually **shipped**, and searched those JARs for the affected classes.
5. Reviewed TaskFlow's source and `application.properties` for use of the
   affected features (authentication, HTTP/2, WebSocket, rewrite rules, AI
   tools, etc.).
6. Checked vendor advisories (Apache Tomcat, Spring, Netty) for affected and
   fixed versions.

### Reproducing the scan

An NVD API key is required in practice: Dependency-Check 13.0.0 cannot update
its database without one (upstream issue
[DependencyCheck#8715](https://github.com/dependency-check/DependencyCheck/issues/8715)),
and the first full download takes several hours. Keep the key in an
environment variable; never put it in a file in this repository, on the
command line, or in logs.

```xml
<!-- ~/.m2/settings.xml (outside the repository) -->
<settings>
  <servers>
    <server>
      <id>nvd</id>
      <password>${env.NVD_API_KEY}</password>
    </server>
  </servers>
</settings>
```

```bash
# Key from https://nvd.nist.gov/developers/request-an-api-key - set it in your
# shell profile, never commit it
export NVD_API_KEY=...

./mvnw org.owasp:dependency-check-maven:13.0.0:check \
  -Dformats=HTML,JSON \
  -DnvdApiServerId=nvd
```

Reports are written to `target/`. They are intentionally **not committed**:
they are regenerated on every scan and need a separate content review before
publication.

## 4. Findings and applicability

Terms used:

- **Shipped** - the vulnerable library is inside the deployable JAR / Docker image.
- **Not applicable** - shipped (or flagged), but the vulnerable feature or
  configuration is not used by TaskFlow. The code is still present, so this is
  a statement about the *current* configuration, not a guarantee.
- **False positive** - the scanner matched the wrong product.

### 4.1 Apache Tomcat 11.0.24 - 11 CVEs

- **Introduced by:** `spring-boot-starter-webmvc` → `spring-boot-starter-tomcat`
  (transitive; version managed by Spring Boot 4.1.1). **Shipped.**
- **Vendor advisory:** [Apache Tomcat 11 security page](https://tomcat.apache.org/security-11.html)
  lists all 11 CVEs below under *Fixed in Apache Tomcat 11.0.25*.
- **TaskFlow context:** no Spring Security or servlet security constraints,
  no authentication of any kind, no `server.*` settings (HTTP/2 is off by
  default), no TLS in Tomcat (Render terminates HTTPS in front of the
  container), no WebSocket endpoints, no rewrite rules, no Unix domain socket,
  and embedded Tomcat ships no examples web application.

| CVE | NVD CVSS | Apache severity | Condition required | Assessment |
|-----|---------:|-----------------|--------------------|------------|
| CVE-2026-65637 | 9.8 | Moderate | HTTP/2 + TLS strict SNI validation (incomplete fix of CVE-2026-32990) | Not applicable - no TLS or HTTP/2 in Tomcat |
| CVE-2026-65905 | 9.8 | Low | DIGEST authentication | Not applicable |
| CVE-2026-65182 | 9.1 | Important | Overlapping security constraints | Not applicable |
| CVE-2026-68525 | 9.1 | Low | FORM authentication | Not applicable |
| CVE-2026-65183 | 8.1 | Low | Unix domain socket, local attacker | Not applicable |
| CVE-2026-66422 | 8.1 | Low | `security-role-ref` role checks | Not applicable |
| CVE-2026-68569 | 8.1 | Important | Realm principal lookup (e.g. DataSourceRealm with CLIENT-CERT/SPNEGO) | Not applicable |
| CVE-2026-65927 | 7.5 | Important | RewriteValve `[N]` flag | Not applicable |
| CVE-2026-68763 | 7.5 | Important | HTTP/2 stream reset | **Further investigation** - HTTP/2 is not enabled in TaskFlow; that Render forwards HTTP/1.1 to the container is assumed, not verified |
| CVE-2026-73180 | 6.8 | Low | Authenticated WebSocket session | Not applicable |
| CVE-2026-66299 | 5.3 | Low | Tomcat examples web application | Not applicable |

The NVD scores are much higher than Apache's own ratings because NVD scores
the worst-case configuration.

**Not reported by the scanner:** the same Apache page lists 19 further CVEs
fixed in Tomcat **11.0.26** and **11.0.27** whose affected ranges include
11.0.24 (for example CVE-2026-77756 *Transfer-Encoding honored for HTTP/1.0
requests* and CVE-2026-101356 *HTTP/2 request/response mix-up*). None of them
appeared in this scan, most likely because NVD had not yet attached product
data to them. They have **not** been assessed individually and are a reason
to upgrade to 11.0.27 rather than the minimum 11.0.25.

### 4.2 Spring AI 2.0.0 - 7 CVEs

- **Introduced by:** `spring-ai-starter-model-openai` → `spring-ai-openai` →
  `spring-ai-model` (transitive; version set by the directly imported
  `spring-ai-bom` 2.0.0). **Shipped.**
- **Vendor advisories:** each CVE links to its spring.io advisory, which lists
  **2.0.1 (OSS)** as the fixed version for the 2.0.x line.
- **Why modules TaskFlow does not use appear:** the scanner matches the whole
  *Spring AI* product to `spring-ai-model-2.0.0.jar`. TaskFlow only ships the
  OpenAI chat modules; the affected classes were searched for in every shipped
  JAR.
- **TaskFlow context:** a single `ChatClient` prompt in
  `TaskService.summariseTasks()`. No tools (`@Tool` / tool callbacks), no MCP,
  no document readers, no embeddings, no vector store, no Redis.

| CVE | NVD CVSS | Affected component | Affected class shipped? | Assessment |
|-----|---------:|--------------------|-------------------------|------------|
| [CVE-2026-59318](https://spring.io/security/cve-2026-59318) | 9.8 | Tool calling - per-request tool list not enforced | **Yes** (`ToolCallingManager` in `spring-ai-model`) | Not applicable today (no tools registered), but the code is shipped; **highest-priority Spring AI fix** because adding any tool later would expose it |
| [CVE-2026-59279](https://spring.io/security/cve-2026-59279) | 7.5 | MCP Streamable HTTP server sessions | No | Not applicable |
| [CVE-2026-47851](https://spring.io/security/cve-2026-47851) | 7.5 | PDF document reader | No | Not applicable |
| [CVE-2026-47852](https://spring.io/security/cve-2026-47852) | 7.5 | ONNX model cache (Transformers) | No | Not applicable |
| [CVE-2026-59294](https://spring.io/security/cve-2026-59294) | 6.5 | `ResourceCacheService` path traversal | No | Not applicable |
| [CVE-2026-59308](https://spring.io/security/cve-2026-59308) | 4.3 | Semantic cache | No | Not applicable |
| [CVE-2026-59319](https://spring.io/security/cve-2026-59319) | 4.3 | Redis chat memory repository | No | Not applicable |

### 4.3 Netty 4.2.17.Final - 1 CVE

| CVE | NVD CVSS | Description |
|-----|---------:|-------------|
| CVE-2026-89044 | 6.5 (v3) / 6.9 (v4) | HTTP request smuggling: `Transfer-Encoding` final coding not validated in the HTTP/1 decoder |

- **Introduced by:** `spring-ai-starter-model-openai` →
  `spring-boot-starter-webclient` → `reactor-netty-http` 1.3.7 →
  `netty-codec-http` (transitive). **Shipped.** The scanner attached the CVE to
  `netty-transport`; the affected decoder is in `netty-codec-http`.
- **Vendor advisory:** Netty advisory
  [GHSA-hcvj-94mj-jp5c](https://github.com/netty/netty/security/advisories/GHSA-hcvj-94mj-jp5c),
  listed in the [4.2.18.Final release notes](https://github.com/netty/netty/releases/tag/netty-4.2.18.Final)
  (fixed in **4.2.18.Final**).
- **TaskFlow context:** Tomcat is the web server; no Netty server is started.
  Spring AI's OpenAI module calls OpenAI through OkHttp (its classes reference
  OkHttp, not Reactor Netty), and TaskFlow's own code does not use WebClient.
- **Assessment:** not applicable at runtime (decoder shipped but unused).

The same 4.2.18 release fixes about 30 other Netty advisories that this scan
did not report, and warns that the new validation may reject traffic that
was previously accepted.

### 4.4 Kotlin standard libraries - 2 CVEs, 3 associations

TaskFlow is written in Java; Kotlin arrives transitively through the OpenAI
SDK and OkHttp:

- `kotlin-stdlib` **2.3.21** - via `kotlin-reflect` and `openai-java-core`
  (version managed by Spring Boot).
- `kotlin-stdlib-common` **1.9.10** - via `okhttp` 4.12.0 → `okio-jvm` 3.6.0.
  Not managed by Spring Boot, hence the version mismatch.

| CVE | NVD CVSS | Flagged JAR(s) | Assessment |
|-----|---------:|----------------|------------|
| CVE-2026-53914 | 9.8 | `kotlin-stdlib-2.3.21`, `kotlin-stdlib-common-1.9.10` | **Suspected false positive** - "unsafe deserialization in the build cache metadata", i.e. the Kotlin compiler/build tooling, not the runtime standard library. Matched through the generic `jetbrains:kotlin` product identifier. TaskFlow compiles no Kotlin. The vendor reference ([JetBrains fixed issues](https://www.jetbrains.com/privacy-security/issues-fixed/)) gives no component detail, so this remains suspected rather than confirmed. |
| CVE-2020-29582 | 5.3 | `kotlin-stdlib-common-1.9.10` | **Not applicable** - this JAR contains **0 `.class` files** (386 entries, all `.kotlin_metadata`), so it has no executable code. NVD's configured range (< 2.1.0) also conflicts with its own description ("before 1.4.21"; see [JetBrains Q4 2020 bulletin](https://blog.jetbrains.com/blog/2021/02/03/jetbrains-security-bulletin-q4-2020/)). |

### 4.5 Spring Boot DevTools 4.1.1 - 1 CVE

| CVE | NVD CVSS | Assessment |
|-----|---------:|------------|
| [CVE-2022-31691](https://spring.io/security/cve-2022-31691) | 9.8 | **Confirmed false positive** |

- The CVE affects the **Spring Tools IDE products** (Spring Tools 4 for
  Eclipse 4.0.0-4.16.0 and several VS Code extensions), not Spring Boot.
- The scanner assigned IDE product identifiers (`spring_tools`,
  `spring_boot_tools`) to `spring-boot-devtools-4.1.1.jar`, and 4.1.1
  coincidentally falls inside the Spring Tools 4.0.0-4.16.0 range.
- In addition, DevTools is an `optional` runtime dependency and is **not
  packaged** in the deployable JAR (confirmed by listing `BOOT-INF/lib/`).

## 5. Disposition summary

| Disposition | CVEs |
|-------------|------|
| Confirmed applicable (vulnerable code shipped **and** feature used) | None identified |
| Not applicable in the current configuration | 10 Tomcat CVEs, all 7 Spring AI CVEs, CVE-2026-89044 (Netty), CVE-2020-29582 (Kotlin) |
| Confirmed false positive | CVE-2022-31691 (DevTools) |
| Suspected false positive | CVE-2026-53914 (Kotlin) |
| Requires further investigation | CVE-2026-68763 (Tomcat HTTP/2), CVE-2026-53914 (confirm with JetBrains) |

"Not applicable" does not mean harmless: in most cases the vulnerable code is
still shipped and would become reachable if the related feature were enabled
later (for example adding Spring AI tools, HTTP/2, authentication or WebSocket
endpoints).

## 6. Remediation - deferred

### Recommended updates (NOT implemented)

These are preliminary candidates. They have **not** been applied, built,
tested or rescanned, so they must not be treated as resolving any finding
until a subsequent complete scan confirms it.

| Order | Change | Expected effect (per vendor advisories) | Notes |
|-------|--------|-----------------------------------------|-------|
| 1 | Spring Boot parent **4.1.1 → 4.1.2** | Tomcat 11.0.24 → 11.0.27 (≥ 11.0.25 fixes the 11 reported CVEs; 11.0.27 also covers the 19 unreported ones); Netty 4.2.17 → 4.2.19 (≥ 4.2.18 fixes CVE-2026-89044) | Patch release; also moves Spring Framework 7.0.9 → 7.0.10 and Jackson 3.1.5 → 3.1.7 |
| 2 | `spring-ai-bom` **2.0.0 → 2.0.1** | Fixes all 7 Spring AI CVEs (2.0.1 OSS per spring.io) | Moves `openai-java-core` 4.39.1 → 4.49.0; compatibility with Boot 4.1.2 still to be confirmed |
| 3 | Rescan with Dependency-Check | Confirms which findings are actually resolved | Required before any finding is marked fixed |
| 4 (optional) | Manage `kotlin-stdlib-common` at 2.3.21 | Removes the stale metadata-only 1.9.10 JAR | Hygiene only |
| 5 | Suppression file for evidence-backed false positives | Reduces noise in future scans | Only CVE-2022-31691 (and CVE-2026-53914 once confirmed), each with justification and review date |

### Why remediation is deferred

- Stretch Goal 5 requires the scan and its analysis; it does not require every
  CVE to be fixed.
- No finding was shown to be exploitable in TaskFlow's current configuration.
- Dependency upgrades change runtime behaviour (the Netty 4.2.18 notes warn
  about stricter validation) and need their own branch, tests, CI run and
  rescan, separate from this documentation change.
- The current test suite has a single `contextLoads` test; controller-level
  tests should be added before upgrading so regressions are caught.

### Regression checks required when remediating

- `./mvnw clean verify` and a before/after `dependency:tree` comparison.
- Inspect `BOOT-INF/lib/` for Tomcat 11.0.27, Netty ≥ 4.2.18, Spring AI 2.0.1
  and DevTools still absent.
- `docker build`, then a container smoke test of all seven endpoints
  (including 404 handling and `/api/tasks/summary`).
- Optional local `/api/tasks/summary` call with a real OpenAI key to confirm
  Spring AI still works.
- Green GitHub Actions run, then a new complete Dependency-Check scan.

## 7. Scanner limitations

- **NVD analysis completed**, but Dependency-Check relies on NVD product (CPE)
  matching. This produced the false positives above and attributes Spring AI
  CVEs to modules TaskFlow does not ship.
- **NVD lag:** vendor advisories list at least 19 Tomcat CVEs and around 30
  Netty advisories affecting the scanned versions that the scan did not report.
- **.NET Assembly Analyzer did not initialise.** The .NET runtime it requires
  is not installed. The only DLL in the scan is
  `netty_quiche42_windows_x86_64.dll`, inside
  `netty-codec-native-quic-4.2.17.Final-windows-x86_64.jar`; it is a native
  QUIC library, not a .NET assembly. Disabling this analyzer
  (`-DassemblyAnalyzerEnabled=false`) is justified for a Java-only project
  when documented, but native libraries are still not checked for their own
  CVEs.
- **Sonatype OSS Index was unavailable** (it now requires authentication), so
  there was no second vulnerability source.
- **Dependency-Check 13.0.0 bug:** it cannot update without an NVD API key
  (upstream issue #8715).
- The scan covers Maven dependencies only - not the base Docker image
  (`eclipse-temurin:21-jre-alpine`), the JVM or OS packages.

**Conclusion:** the dependency scan was completed and analysed. TaskFlow is
**not** certified vulnerability-free: vulnerable library code is shipped, some
known advisories were not reported by the scanner, and the recommended
updates have not yet been implemented or verified.
