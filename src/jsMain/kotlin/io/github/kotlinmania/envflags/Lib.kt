// port-lint: source src/lib.rs (platform glue, JS target — Node process.env reachable at runtime, browser returns null)
package io.github.kotlinmania.envflags

public actual fun envGet(key: String): String? {
    val raw: dynamic = js(
        "(function(k){ try { return (typeof process !== 'undefined' && process.env && typeof process.env[k] === 'string') ? process.env[k] : null; } catch (e) { return null; } })(key)",
    )
    return raw as String?
}
