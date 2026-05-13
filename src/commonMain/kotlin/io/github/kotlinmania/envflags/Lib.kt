// port-lint: source src/lib.rs
package io.github.kotlinmania.envflags

import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * This crate exports the [envFlag] helper, which allows a convenient way to declare static
 * environment variables with optional default values and custom parsing functions.
 *
 * Currently, this crate requires Kotlin's [kotlin.Lazy] under the hood (the Kotlin analogue of
 * Rust's `std::sync::LazyLock`).
 *
 * # Examples
 *
 * ```kotlin
 * import io.github.kotlinmania.envflags.envFlag
 * import io.github.kotlinmania.envflags.IntParser
 * import io.github.kotlinmania.envflags.StringParser
 * import io.github.kotlinmania.envflags.NullableParser
 * import io.github.kotlinmania.envflags.DurationParser
 * import io.github.kotlinmania.envflags.BoolParser
 * import io.github.kotlinmania.envflags.UShortParser
 * import io.github.kotlinmania.envflags.ListParser
 *
 * // Required env var, panics if missing.
 * val AUTH_TOKEN by envFlag("AUTH_TOKEN", StringParser)
 * // Env var with a default value if not specified.
 * val PORT by envFlag("PORT", UShortParser, default = { 8080u })
 * // An optional env var.
 * val OVERRIDE_HOSTNAME by envFlag("OVERRIDE_HOSTNAME", NullableParser(StringParser), default = { null })
 *
 * // Duration by default is parsed as seconds (f64).
 * val TIMEOUT by envFlag("TIMEOUT", DurationParser, default = { 5.seconds })
 * // Custom parsing function, takes a String and returns a Result<Duration>.
 * val TIMEOUT_MS by envFlag(
 *     "TIMEOUT_MS",
 *     parser = { v -> v.toLongOrNull()?.let { Result.success(it.milliseconds) }
 *         ?: Result.failure(ParseError("Duration", "expected a long: $v")) },
 *     default = { 30.milliseconds },
 * )
 *
 * // Boolean can be true, false, 1, or 0 (case insensitive).
 * // eg. export ENABLE_FEATURE="true"
 * val ENABLE_FEATURE by envFlag("ENABLE_FEATURE", BoolParser, default = { true })
 *
 * // List<T> by default is parsed as a comma-separated string.
 * // eg. export VALID_PORTS="80,443,9121"
 * val VALID_PORTS by envFlag("VALID_PORTS", ListParser(UShortParser), default = { listOf(80u, 443u, 9121u) })
 * ```
 *
 * For custom types, you can either specify a parsing function manually (see above `TIMEOUT_MS`
 * example), or you can implement the [ParseEnv] interface. An implementation of [ParseEnv] is
 * included for most common Kotlin types.
 *
 * # Platform notes
 *
 * Environment-variable access is wired through the [envGet] expect declaration, which provides a
 * String value or `null`. Each KMP target supplies its own actual; the browser variants of `js`
 * and `wasmJs` always return `null` because no environment is reachable from a browser document.
 *
 * # Omitted Rust types
 *
 * The upstream Rust crate also implements [ParseEnv] for `i128`, `u128`, `isize`, `usize`,
 * `IpAddr`, `Ipv4Addr`, `Ipv6Addr`, `SocketAddr`, and `PathBuf`. None of those have a
 * Kotlin-stdlib analogue available in commonMain, and pulling in a third-party numeric or
 * networking library to back them would change the published surface area of every consumer.
 * Until separate `*-kotlin` ports of those Rust types exist, callers that need them can use a
 * custom parser function. The associated upstream Rust integration tests are translated below
 * as parser tests against the surviving Kotlin shape.
 */

/**
 * Define the parsing function for a type from a [String] environment variable.
 *
 * Check the source for the built-in type definitions for this trait if you're concerned about
 * the parsing logic.
 */
public interface ParseEnv<T> {
    /**
     * Tries to parse the value from [envGet]. The failure path returns a [Throwable] whose
     * [Throwable.toString] (or [Throwable.message]) appears in the panic message produced when
     * a required environment variable cannot be parsed.
     */
    public fun parseEnv(value: String): Result<T>
}

/**
 * Intermediate error type used in parsing failures to generate helpful messages.
 */
public class ParseError(
    public val typeName: String,
    public val msg: String,
) : RuntimeException("failed to parse as $typeName: $msg") {
    public fun withTypeName(newTypeName: String): ParseError = ParseError(newTypeName, msg)

    public companion object {
        public fun fromMsg(typeName: String, msg: Any?): ParseError =
            ParseError(typeName = typeName, msg = msg.toString())
    }
}

// Implements ParseEnv for common types based on the Kotlin stdlib `String.toXxx()` family — the
// Kotlin analogue of Rust's `std::str::FromStr`. Each upstream `gen_parse_env_using_fromstr!(t)`
// instantiation translates to one Kotlin object below.
private inline fun <T : Any> parseFromStr(
    typeName: String,
    value: String,
    parse: (String) -> T?,
): Result<T> {
    val parsed = try {
        parse(value)
    } catch (e: NumberFormatException) {
        return Result.failure(ParseError.fromMsg(typeName, e.message ?: value))
    }
    return if (parsed != null) {
        Result.success(parsed)
    } else {
        Result.failure(ParseError.fromMsg(typeName, "invalid $typeName: $value"))
    }
}

public object FloatParser : ParseEnv<Float> {
    override fun parseEnv(value: String): Result<Float> = parseFromStr("Float", value) {
        when (it.lowercase()) {
            "nan" -> Float.NaN
            "inf", "+inf", "infinity", "+infinity" -> Float.POSITIVE_INFINITY
            "-inf", "-infinity" -> Float.NEGATIVE_INFINITY
            else -> it.toFloatOrNull()
        }
    }
}

public object DoubleParser : ParseEnv<Double> {
    override fun parseEnv(value: String): Result<Double> = parseFromStr("Double", value) {
        when (it.lowercase()) {
            "nan" -> Double.NaN
            "inf", "+inf", "infinity", "+infinity" -> Double.POSITIVE_INFINITY
            "-inf", "-infinity" -> Double.NEGATIVE_INFINITY
            else -> it.toDoubleOrNull()
        }
    }
}

public object ByteParser : ParseEnv<Byte> {
    override fun parseEnv(value: String): Result<Byte> =
        parseFromStr("Byte", value) { it.toByteOrNull() }
}

public object ShortParser : ParseEnv<Short> {
    override fun parseEnv(value: String): Result<Short> =
        parseFromStr("Short", value) { it.toShortOrNull() }
}

public object IntParser : ParseEnv<Int> {
    override fun parseEnv(value: String): Result<Int> =
        parseFromStr("Int", value) { it.toIntOrNull() }
}

public object LongParser : ParseEnv<Long> {
    override fun parseEnv(value: String): Result<Long> =
        parseFromStr("Long", value) { it.toLongOrNull() }
}

public object UByteParser : ParseEnv<UByte> {
    override fun parseEnv(value: String): Result<UByte> =
        parseFromStr("UByte", value) { it.toUByteOrNull() }
}

public object UShortParser : ParseEnv<UShort> {
    override fun parseEnv(value: String): Result<UShort> =
        parseFromStr("UShort", value) { it.toUShortOrNull() }
}

public object UIntParser : ParseEnv<UInt> {
    override fun parseEnv(value: String): Result<UInt> =
        parseFromStr("UInt", value) { it.toUIntOrNull() }
}

public object ULongParser : ParseEnv<ULong> {
    override fun parseEnv(value: String): Result<ULong> =
        parseFromStr("ULong", value) { it.toULongOrNull() }
}

public object StringParser : ParseEnv<String> {
    override fun parseEnv(value: String): Result<String> = Result.success(value)
}

/**
 * [Duration] by default is parsed as `Double` seconds.
 */
public object DurationParser : ParseEnv<Duration> {
    override fun parseEnv(value: String): Result<Duration> =
        when (val parsed = DoubleParser.parseEnv(value)) {
            else -> parsed.fold(
                onSuccess = { Result.success(it.seconds) },
                onFailure = { err ->
                    if (err is ParseError) {
                        Result.failure(err.withTypeName("Duration"))
                    } else {
                        Result.failure(err)
                    }
                },
            )
        }
}

/**
 * `List<T>` is by default parsed as comma-separated values.
 */
public class ListParser<T : Any>(private val inner: ParseEnv<T>) : ParseEnv<List<T>> {
    override fun parseEnv(value: String): Result<List<T>> {
        val out = ArrayList<T>()
        for (part in value.split(',')) {
            val r = inner.parseEnv(part)
            r.exceptionOrNull()?.let { return Result.failure(it) }
            out += r.getOrThrow()
        }
        return Result.success(out)
    }
}

/**
 * `Set<T>` is by default parsed as comma-separated values.
 */
public class SetParser<T : Any>(private val inner: ParseEnv<T>) : ParseEnv<Set<T>> {
    override fun parseEnv(value: String): Result<Set<T>> {
        val out = LinkedHashSet<T>()
        for (part in value.split(',')) {
            val r = inner.parseEnv(part)
            r.exceptionOrNull()?.let { return Result.failure(it) }
            out += r.getOrThrow()
        }
        return Result.success(out)
    }
}

/**
 * Translates Rust's `impl<T> ParseEnv for Option<T>`. A present environment variable parses to a
 * non-null `T`; the upstream impl only ever produces `Some(_)` from a present value, so this
 * Kotlin shape mirrors that by wrapping the inner parser's success in `T?`. The "unset" branch is
 * handled by the [envFlag] caller's `default` lambda, exactly as in Rust where `Option<T>` flags
 * always carry a default of `None`.
 */
public class NullableParser<T : Any>(private val inner: ParseEnv<T>) : ParseEnv<T?> {
    override fun parseEnv(value: String): Result<T?> = inner.parseEnv(value).map { it }
}

/**
 * Boolean allows two common conventions:
 *  - String either "true" or "false" (case insensitive)
 *  - Integer either 0 or 1
 *
 * Anything else will result in a [ParseError].
 */
public object BoolParser : ParseEnv<Boolean> {
    override fun parseEnv(value: String): Result<Boolean> {
        return when (value.lowercase()) {
            "true", "1" -> Result.success(true)
            "false", "0" -> Result.success(false)
            else -> Result.failure(
                ParseError.fromMsg("Boolean", "expected either true or false"),
            )
        }
    }
}

/**
 * Static lazily evaluated environment variable.
 *
 * Wraps a [kotlin.Lazy] so that callers can either read [LazyEnv.value] directly or use property
 * delegation: `val PORT: UShort by envFlag(...)`.
 */
public class LazyEnv<T>(initFn: () -> T) {
    private val inner: Lazy<T> = lazy(initFn)

    public val value: T get() = inner.value

    public operator fun getValue(thisRef: Any?, property: KProperty<*>): T = inner.value

    override fun toString(): String = inner.value.toString()
}

/**
 * Helper function for better failure-mode error messages — the Kotlin analogue of Rust's
 * `__apply_parse_fn` macro internal.
 */
@PublishedApi
internal fun <T> applyParseFn(
    parse: (String) -> Result<T>,
    key: String,
    value: String,
): T {
    return parse(value).fold(
        onSuccess = { it },
        onFailure = { invalidEnvVar(key, it) },
    )
}

@PublishedApi
internal fun invalidEnvVar(key: String, err: Throwable): Nothing {
    val msg = (err as? ParseError)?.let { "failed to parse as ${it.typeName}: ${it.msg}" }
        ?: err.message
        ?: err.toString()
    throw IllegalStateException("Invalid environment variable $key, $msg")
}

@PublishedApi
internal fun missingEnvVar(key: String): Nothing {
    throw IllegalStateException("Missing required environment variable $key")
}

/**
 * Look up the value of an environment variable.
 *
 * Returns the trimmed-and-decoded String value of the variable, or `null` when the variable is
 * unset for any reason. Each KMP target supplies its own actual; browser-side `js` and `wasmJs`
 * always return `null` because no environment is reachable from a browser document.
 */
public expect fun envGet(key: String): String?

/**
 * Declare an environment variable with optional default and parsing function.
 *
 * Values are static and lazily evaluated once the first time they are dereferenced. The Kotlin
 * function is the runtime analogue of the upstream `env_flags!` macro: each Rust declaration
 *
 * ```text
 * pub PORT: u16 = 8080;
 * ```
 *
 * translates to one Kotlin call site
 *
 * ```kotlin
 * val PORT by envFlag("PORT", UShortParser, default = { 8080u })
 * ```
 *
 * See the module-level documentation for examples.
 */
public fun <T> envFlag(
    key: String,
    parser: ParseEnv<T>,
    default: (() -> T)? = null,
): LazyEnv<T> = envFlag(key = key, parser = { parser.parseEnv(it) }, default = default)

/**
 * Declare an environment variable with an inline parsing function.
 *
 * See [envFlag] above for the [ParseEnv]-typed entry point.
 */
public fun <T> envFlag(
    key: String,
    parser: (String) -> Result<T>,
    default: (() -> T)? = null,
): LazyEnv<T> = LazyEnv {
    when (val v = envGet(key)) {
        null -> if (default != null) default() else missingEnvVar(key)
        else -> applyParseFn(parser, key, v)
    }
}
