// port-lint: source src/lib.rs (platform glue, Android target via java.lang.System.getenv)
package io.github.kotlinmania.envflags

public actual fun envGet(key: String): String? = System.getenv(key)
