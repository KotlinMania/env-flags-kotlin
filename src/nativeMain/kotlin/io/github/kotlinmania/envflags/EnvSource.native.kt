package io.github.kotlinmania.envflags

import io.github.kotlinmania.libc.unix.getenv

internal actual fun systemEnvVar(name: String): String? = getenv(name)
