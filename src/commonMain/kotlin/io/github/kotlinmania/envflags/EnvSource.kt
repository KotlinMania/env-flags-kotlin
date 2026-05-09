// port-lint: ignore (platform abstraction over std::env::var used by src/lib.rs)
package io.github.kotlinmania.envflags

/** Pluggable view of the process environment used by [LazyEnv] / [envFlag]. */
public fun interface EnvSource {
    public fun get(name: String): String?

    public companion object {
        /** The platform's process environment. Returns `null` for unset variables. */
        public val SYSTEM: EnvSource = EnvSource { name -> systemEnvVar(name) }
    }
}

/** Convenience [EnvSource] backed by an in-memory map. */
public class MapEnvSource(private val values: Map<String, String>) : EnvSource {
    override fun get(name: String): String? = values[name]
}

internal expect fun systemEnvVar(name: String): String?
