// port-lint: ignore (Wasm-JS implementation of EnvSource for src/lib.rs)
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.kotlinmania.envflags

internal actual fun systemEnvVar(name: String): String? = jsGetEnv(name)

private fun jsGetEnv(name: String): String? = js(
    "(typeof process !== 'undefined' && process && process.env && typeof process.env[name] === 'string') ? process.env[name] : null",
)
