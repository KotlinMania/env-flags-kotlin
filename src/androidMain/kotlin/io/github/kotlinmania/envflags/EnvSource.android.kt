// port-lint: ignore (Android/JVM implementation of EnvSource for src/lib.rs)
package io.github.kotlinmania.envflags

internal actual fun systemEnvVar(name: String): String? = System.getenv(name)
