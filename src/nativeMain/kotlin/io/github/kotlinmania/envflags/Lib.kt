// port-lint: source src/lib.rs (platform glue, native targets via platform.posix.getenv)
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.kotlinmania.envflags

import kotlinx.cinterop.toKString
import platform.posix.getenv

public actual fun envGet(key: String): String? = getenv(key)?.toKString()
