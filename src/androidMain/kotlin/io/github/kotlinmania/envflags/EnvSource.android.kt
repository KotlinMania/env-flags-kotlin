package io.github.kotlinmania.envflags

internal actual fun systemEnvVar(name: String): String? = System.getenv(name)
