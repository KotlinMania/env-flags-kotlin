// port-lint: ignore (Wasm-WASI implementation of EnvSource for src/lib.rs)
package io.github.kotlinmania.envflags

// Kotlin/Wasm-WASI does not expose process environment access through commonised stdlib bindings.
internal actual fun systemEnvVar(name: String): String? = null
