# env-flags-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Fenv--flags--kotlin-blue.svg)](https://github.com/KotlinMania/env-flags-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/env-flags-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/env-flags-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/env-flags-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/env-flags-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`kykosic/env-flags`](https://github.com/kykosic/env-flags).

**Original Project:** This port is based on [`kykosic/env-flags`](https://github.com/kykosic/env-flags). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `kykosic/env-flags`

> The text below is reproduced and lightly edited from [`https://github.com/kykosic/env-flags`](https://github.com/kykosic/env-flags). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

## env_flags

[<img alt="github" src="https://img.shields.io/badge/github-kykosic/env--flags-a68bbd?style=for-the-badge&logo=github" height="20">](https://github.com/kykosic/env-flags)
[<img alt="crates.io" src="https://img.shields.io/crates/v/env-flags?style=for-the-badge&color=f0963a&logo=rust" height="20">](https://crates.io/crates/env-flags)
[<img alt="docs.rs" src="https://img.shields.io/badge/docs.rs-env--flags-57979e?style=for-the-badge&logo=docs.rs" height="20">](https://docs.rs/env-flags)
[<img alt="build status" src="https://img.shields.io/github/actions/workflow/status/kykosic/env-flags/ci.yml?branch=main&style=for-the-badge" height="20">](https://github.com/kykosic/env-flags/actions?query=branch%3Amain)

This library provides a convenient macro for declaring environment variables.

```toml
[dependencies]
env-flags = "0.1"
```

_Compiler support: requires rustc 1.80+_

## Example

```rust
use env_flags::env_flags;

use std::time::Duration;

env_flags! {
    /// Required env var, panics if missing.
    AUTH_TOKEN: &str;
    /// Env var with a default value if not specified.
    pub(crate) PORT: u16 = 8080;
    /// An optional env var.
    pub OVERRIDE_HOSTNAME: Option<&str> = None;

    /// `Duration` by default is parsed as `f64` seconds.
    TIMEOUT: Duration = Duration::from_secs(5);
    /// Custom parsing function, takes a `String` and returns a `Result<Duration>`.
    TIMEOUT_MS: Duration = Duration::from_millis(30), |value| {
        value.parse().map(Duration::from_millis)
    };

    /// `bool` can be true, false, 1, or 0 (case insensitive)
    /// eg. export ENABLE_FEATURE="true"
    pub ENABLE_FEATURE: bool = true;

    /// `Vec<T>` by default is parsed as a comma-seprated string
    /// eg. export VALID_PORTS="80,443,9121"
    pub VALID_PORTS: Vec<u16> = vec![80, 443, 9121];
}
```

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:env-flags-kotlin:0.1.0")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same MIT license as the upstream [`kykosic/env-flags`](https://github.com/kykosic/env-flags). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the env-flags authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`kykosic/env-flags`](https://github.com/kykosic/env-flags) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
