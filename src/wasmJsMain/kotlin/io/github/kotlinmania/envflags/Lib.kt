// port-lint: source src/lib.rs (platform glue, Wasm-JS target — Node process.env reachable at runtime, browser returns null)
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.kotlinmania.envflags

@JsFun(
    "(k) => { try { return (typeof process !== 'undefined' && process.env && typeof process.env[k] === 'string') ? process.env[k] : null; } catch (e) { return null; } }",
)
private external fun envGetJs(key: String): String?

public actual fun envGet(key: String): String? = envGetJs(key)
