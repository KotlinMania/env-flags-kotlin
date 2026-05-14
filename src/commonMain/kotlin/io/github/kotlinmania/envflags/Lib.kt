// port-lint: source src/lib.rs
package io.github.kotlinmania.envflags

import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * This package exports the `envFlag` factory, which provides a convenient way to declare
 * lazy-initialized environment variable accessors with optional default values and custom
 * parsing functions.
 *
 * # Examples
 * ```kotlin
 * import io.github.kotlinmania.envflags.envFlag
 * import io.github.kotlinmania.envflags.Parsers
 * import kotlin.time.Duration
 *
 * /** Required env var, panics if missing. */
 * val AUTH_TOKEN: LazyEnv<String> = envFlag("AUTH_TOKEN", Parsers.string)
 *
 * /** Env var with a default value if not specified. */
 * val PORT: LazyEnv<UShort> = envFlag("PORT", Parsers.uShort) { 8080u }
 *
 * /** An optional env var. */
 * val OVERRIDE_HOSTNAME: LazyEnv<String?> = envFlag("OVERRIDE_HOSTNAME", Parsers.optional(Parsers.string)) { null }
 *
 * /** `Duration` by default is parsed as `Double` seconds. */
 * val TIMEOUT: LazyEnv<Duration> = envFlag("TIMEOUT", Parsers.duration) { Duration.parse("PT5S") }
 * /** Custom parsing function, takes a `String` and returns a parsed `Duration`. */
 * val TIMEOUT_MS: LazyEnv<Duration> = envFlag("TIMEOUT_MS", { value ->
 *     value.toLong().toDuration(DurationUnit.MILLISECONDS)
 * }) { 30L.toDuration(DurationUnit.MILLISECONDS) }
 *
 * /** `Boolean` can be true, false, 1, or 0 (case insensitive). */
 * val ENABLE_FEATURE: LazyEnv<Boolean> = envFlag("ENABLE_FEATURE", Parsers.boolean) { true }
 *
 * /** `List<T>` by default is parsed as a comma-separated string. */
 * val VALID_PORTS: LazyEnv<List<UShort>> = envFlag("VALID_PORTS", Parsers.list(Parsers.uShort)) {
 *     listOf(80u, 443u, 9121u)
 * }
 * ```
 *
 * For custom types, you can either specify a parsing function manually (see above `TIMEOUT_MS`
 * example), or you can implement a [ParseEnv] instance. Implementations for [ParseEnv] are
 * provided in [Parsers] for most stdlib types.
 */

/**
 * Define the parsing function for a type from a `String` environment variable.
 *
 * Check [Parsers] for the built-in type definitions if you're concerned about the parsing logic.
 * The thrown exception's `message` will appear in the panic message when parsing fails.
 */
public fun interface ParseEnv<out T> {
    /** Tries to parse the value retrieved from the platform environment. */
    public fun parseEnv(value: String): T
}

/** Intermediate error type used in parsing failures to generate helpful messages. */
public class ParseError(
    public val typeName: String,
    public val msg: String,
) : RuntimeException("failed to parse as $typeName: $msg") {
    public companion object {
        public fun fromMsg(typeName: String, msg: Any?): ParseError =
            ParseError(typeName = typeName, msg = msg.toString())
    }

    internal fun withTypeName(newTypeName: String): ParseError =
        ParseError(typeName = newTypeName, msg = msg)
}

/**
 * Built-in [ParseEnv] instances mirroring the upstream `gen_parse_env_using_fromstr!`,
 * `String`, `&'static str`, `Duration`, `Vec<T>`, `HashSet<T>`, `Option<T>`, and `bool` impls.
 *
 * Where Kotlin's stdlib does not provide a direct counterpart of the Rust standard library type
 * (`IpAddr`, `Ipv4Addr`, `Ipv6Addr`, `SocketAddr`, `PathBuf`), this module exposes a small
 * value class with the same parsing behavior so the public API surface matches the upstream.
 */
public object Parsers {
    private inline fun <T> fromStr(typeName: String, crossinline body: (String) -> T): ParseEnv<T> =
        ParseEnv { value ->
            try {
                body(value)
            } catch (e: NumberFormatException) {
                throw ParseError.fromMsg(typeName, e.message ?: e)
            } catch (e: IllegalArgumentException) {
                throw ParseError.fromMsg(typeName, e.message ?: e)
            }
        }

    public val float: ParseEnv<Float> = ParseEnv { value ->
        when (value.lowercase()) {
            "nan", "+nan", "-nan" -> Float.NaN
            "inf", "+inf", "infinity", "+infinity" -> Float.POSITIVE_INFINITY
            "-inf", "-infinity" -> Float.NEGATIVE_INFINITY
            else -> try {
                value.toFloat()
            } catch (e: NumberFormatException) {
                throw ParseError.fromMsg("Float", e.message ?: e)
            }
        }
    }

    public val double: ParseEnv<Double> = ParseEnv { value ->
        when (value.lowercase()) {
            "nan", "+nan", "-nan" -> Double.NaN
            "inf", "+inf", "infinity", "+infinity" -> Double.POSITIVE_INFINITY
            "-inf", "-infinity" -> Double.NEGATIVE_INFINITY
            else -> try {
                value.toDouble()
            } catch (e: NumberFormatException) {
                throw ParseError.fromMsg("Double", e.message ?: e)
            }
        }
    }
    public val byte: ParseEnv<Byte> = fromStr("Byte") { it.toByte() }
    public val short: ParseEnv<Short> = fromStr("Short") { it.toShort() }
    public val int: ParseEnv<Int> = fromStr("Int") { it.toInt() }
    public val long: ParseEnv<Long> = fromStr("Long") { it.toLong() }

    /** 128-bit signed integer; Kotlin stdlib has no native i128, so values are clamped to [Long]. */
    public val long128: ParseEnv<Long> = fromStr("Long128") { it.toLong() }

    /** Pointer-sized signed integer; mapped to [Long] on all supported targets. */
    public val nativeInt: ParseEnv<Long> = fromStr("NativeInt") { it.toLong() }

    public val uByte: ParseEnv<UByte> = fromStr("UByte") { it.toUByte() }
    public val uShort: ParseEnv<UShort> = fromStr("UShort") { it.toUShort() }
    public val uInt: ParseEnv<UInt> = fromStr("UInt") { it.toUInt() }
    public val uLong: ParseEnv<ULong> = fromStr("ULong") { it.toULong() }

    /** 128-bit unsigned integer; Kotlin stdlib has no native u128, so values are clamped to [ULong]. */
    public val uLong128: ParseEnv<ULong> = fromStr("ULong128") { it.toULong() }

    /** Pointer-sized unsigned integer; mapped to [ULong] on all supported targets. */
    public val nativeUInt: ParseEnv<ULong> = fromStr("NativeUInt") { it.toULong() }

    public val ipAddr: ParseEnv<IpAddr> = fromStr("IpAddr") { IpAddr.parse(it) }
    public val ipv4Addr: ParseEnv<Ipv4Addr> = fromStr("Ipv4Addr") { Ipv4Addr.parse(it) }
    public val ipv6Addr: ParseEnv<Ipv6Addr> = fromStr("Ipv6Addr") { Ipv6Addr.parse(it) }
    public val pathBuf: ParseEnv<PathBuf> = fromStr("PathBuf") { PathBuf(it) }
    public val socketAddr: ParseEnv<SocketAddr> = fromStr("SocketAddr") { SocketAddr.parse(it) }

    /** [String] is returned verbatim. */
    public val string: ParseEnv<String> = ParseEnv { it }

    /**
     * `Duration` by default is parsed as a `Double` count of seconds.
     */
    public val duration: ParseEnv<Duration> = ParseEnv { value ->
        try {
            value.toDouble().toDuration(DurationUnit.SECONDS)
        } catch (e: NumberFormatException) {
            throw ParseError(typeName = "Duration", msg = e.message ?: e.toString())
        }
    }

    /** `List<T>` is by default parsed as comma-separated values. */
    public fun <T> list(inner: ParseEnv<T>): ParseEnv<List<T>> = ParseEnv { value ->
        value.split(",").map { inner.parseEnv(it) }
    }

    /** `Set<T>` is by default parsed as comma-separated values. */
    public fun <T> set(inner: ParseEnv<T>): ParseEnv<Set<T>> = ParseEnv { value ->
        value.split(",").map { inner.parseEnv(it) }.toSet()
    }

    /** Wraps another parser's result in a non-null value; the default is consulted when missing. */
    public fun <T : Any> optional(inner: ParseEnv<T>): ParseEnv<T?> = ParseEnv { value ->
        inner.parseEnv(value)
    }

    /**
     * `Boolean` allows two common conventions:
     *  - String either `"true"` or `"false"` (case insensitive)
     *  - Integer either `0` or `1`
     *
     * Anything else will result in a [ParseError].
     */
    public val boolean: ParseEnv<Boolean> = ParseEnv { rawValue ->
        when (rawValue.lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> throw ParseError.fromMsg("Boolean", "expected either true or false")
        }
    }
}

/** Static lazily evaluated environment variable. */
public class LazyEnv<out T> internal constructor(initFn: () -> T) {
    private val inner: Lazy<T> = lazy(initFn)

    /** Eagerly resolves and returns the parsed value, mirroring the upstream `Deref` impl. */
    public val value: T get() = inner.value

    /** Allows `by` delegation: `val PORT: UShort by envFlag(...)`. */
    public operator fun getValue(thisRef: Any?, property: KProperty<*>): T = inner.value

    override fun toString(): String = inner.value.toString()
}

/** Helper function for better resolution errors. */
internal fun <T> applyParseFn(func: (String) -> T, key: String, value: String): T {
    return try {
        func(value)
    } catch (e: ParseError) {
        invalidEnvVar(key, e)
    } catch (e: Throwable) {
        invalidEnvVar(key, e)
    }
}

internal fun invalidEnvVar(key: String, err: Any?): Nothing {
    throw IllegalStateException(
        "Invalid environment variable $key, ${(err as? Throwable)?.message ?: err}",
    )
}

internal fun missingEnvVar(key: String): Nothing {
    throw IllegalStateException("Missing required environment variable $key")
}

/**
 * Declare a lazy environment variable with an optional default and an explicit parser.
 *
 * Values are lazily evaluated once the first time they are dereferenced.
 *
 * @param key the environment variable name; this is also the panic message subject when missing
 * or invalid
 * @param parse the [ParseEnv] used to convert the raw string into [T]
 * @param source the [EnvSource] consulted at resolution time (defaults to the platform's
 * process environment)
 * @param default a thunk producing the value when the environment variable is unset; if
 * `null`, dereferencing will panic with a "missing required environment variable" message
 */
public fun <T> envFlag(
    key: String,
    parse: ParseEnv<T>,
    source: EnvSource = EnvSource.SYSTEM,
    default: (() -> T)? = null,
): LazyEnv<T> = LazyEnv {
    val raw = source.get(key)
    when {
        raw != null -> applyParseFn(parse::parseEnv, key, raw)
        default != null -> default.invoke()
        else -> missingEnvVar(key)
    }
}

/**
 * Convenience overload used when the upstream invocation supplied both a default and a parser
 * positionally, e.g. `KEY: Duration = Duration::from_millis(30), |value| value.parse()...`.
 */
public fun <T> envFlag(
    key: String,
    parse: (String) -> T,
    source: EnvSource = EnvSource.SYSTEM,
    default: (() -> T)? = null,
): LazyEnv<T> = envFlag(key = key, parse = ParseEnv(parse), source = source, default = default)
