// port-lint: source src/lib.rs (platform glue, Wasm-WASI target — env not currently bridged)
package io.github.kotlinmania.envflags

// Kotlin/Wasm-WASI does not yet expose the underlying `wasi_snapshot_preview1::environ_get`
// import through commonised stdlib bindings the way other native targets expose
// `platform.posix.getenv`. Returning `null` here surfaces the same behaviour as an unset
// variable, so [envFlag] consumers fall through to their `default` lambdas or panic on a
// required variable — matching the documented Rust semantics for an empty environment.
public actual fun envGet(key: String): String? = null
