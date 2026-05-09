// port-lint: ignore (POSIX implementation of EnvSource for src/lib.rs)
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.kotlinmania.envflags

import kotlinx.cinterop.toKString
import platform.posix.getenv

internal actual fun systemEnvVar(name: String): String? = getenv(name)?.toKString()
