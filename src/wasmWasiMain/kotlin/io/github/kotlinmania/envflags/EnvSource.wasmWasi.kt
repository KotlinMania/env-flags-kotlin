package io.github.kotlinmania.envflags

import io.github.kotlinmania.libc.wasi.getenv

internal actual fun systemEnvVar(name: String): String? = getenv(name)
