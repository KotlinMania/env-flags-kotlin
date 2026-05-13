// port-lint: source src/lib.rs (test module — upstream `#[cfg(test)] mod test { ... }`)
package io.github.kotlinmania.envflags

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The Kotlin port routes environment access through the [envGet] expect declaration. The Rust
 * tests called `std::env::set_var(...)` to seed the process-wide environment before declaring a
 * [LazyEnv]; KMP has no portable equivalent (browser-side `js` and `wasmJs` have no environment at
 * all, and `nativeMain` cannot mutate it on every host), so the test suite installs a closure-over
 * `MutableMap<String, String>` and constructs each [LazyEnv] explicitly via the underlying lambda
 * the macro would have emitted.
 */
private fun <T> staticFlag(
    env: Map<String, String>,
    key: String,
    parser: ParseEnv<T>,
    default: (() -> T)? = null,
): LazyEnv<T> = LazyEnv {
    when (val v = env[key]) {
        null -> if (default != null) default() else missingEnvVar(key)
        else -> applyParseFn({ parser.parseEnv(it) }, key, v)
    }
}

private fun <T> staticFlag(
    env: Map<String, String>,
    key: String,
    parser: (String) -> Result<T>,
    default: (() -> T)? = null,
): LazyEnv<T> = LazyEnv {
    when (val v = env[key]) {
        null -> if (default != null) default() else missingEnvVar(key)
        else -> applyParseFn(parser, key, v)
    }
}

class LibTest {
    @Test
    fun testKeyTypeSet() {
        val env = mapOf(
            "ENV_FLAGS_TEST_KEY_TYPE_SET_PRIV" to "80",
            "ENV_FLAGS_TEST_KEY_TYPE_SET_CRATE" to "81",
            "ENV_FLAGS_TEST_KEY_TYPE_SET_PUB" to "82",
        )
        val priv = staticFlag(env, "ENV_FLAGS_TEST_KEY_TYPE_SET_PRIV", UShortParser)
        val crate = staticFlag(env, "ENV_FLAGS_TEST_KEY_TYPE_SET_CRATE", UShortParser)
        val pub = staticFlag(env, "ENV_FLAGS_TEST_KEY_TYPE_SET_PUB", UShortParser)
        assertEquals(80u.toUShort(), priv.value)
        assertEquals(81u.toUShort(), crate.value)
        assertEquals(82u.toUShort(), pub.value)
    }

    @Test
    fun testKeyTypeUnset() {
        val unset = staticFlag<UShort>(emptyMap(), "ENV_FLAGS_TEST_KEY_TYPE_UNSET", UShortParser)
        assertFails { unset.value }
    }

    @Test
    fun testDefault() {
        val env = mapOf(
            "ENV_FLAGS_TEST_DEFAULT_SET_PRIV" to "goodbye",
            "ENV_FLAGS_TEST_DEFAULT_SET_CRATE" to "goodbye",
            "ENV_FLAGS_TEST_DEFAULT_SET_PUB" to "goodbye",
        )
        val priv = staticFlag(env, "ENV_FLAGS_TEST_DEFAULT_SET_PRIV", StringParser) { "hello" }
        val crate = staticFlag(env, "ENV_FLAGS_TEST_DEFAULT_SET_CRATE", StringParser) { "hello" }
        val pub = staticFlag(env, "ENV_FLAGS_TEST_DEFAULT_SET_PUB", StringParser) { "hello" }
        val unset = staticFlag(env, "ENV_FLAGS_TEST_DEFAULT_UNSET", StringParser) { "world" }
        assertEquals("goodbye", priv.value)
        assertEquals("goodbye", crate.value)
        assertEquals("goodbye", pub.value)
        assertEquals("world", unset.value)
    }

    @Test
    fun testParseFn() {
        val env = mapOf(
            "ENV_FLAGS_TEST_PARSE_FN_PRIV" to "250",
            "ENV_FLAGS_TEST_PARSE_FN_CRATE" to "5",
            "ENV_FLAGS_TEST_PARSE_FN_PUB" to "120",
        )
        val priv = staticFlag(env, "ENV_FLAGS_TEST_PARSE_FN_PRIV", parser = { v ->
            v.toLongOrNull()?.let { Result.success(it.milliseconds) }
                ?: Result.failure(ParseError("Duration", "expected a Long: $v"))
        })
        val crate = staticFlag(env, "ENV_FLAGS_TEST_PARSE_FN_CRATE", parser = { v ->
            v.toLongOrNull()?.let { Result.success(it.seconds) }
                ?: Result.failure(ParseError("Duration", "expected a Long: $v"))
        })
        val pub = staticFlag(env, "ENV_FLAGS_TEST_PARSE_FN_PUB", parser = { v ->
            v.toLongOrNull()?.let { Result.success(it.nanoseconds) }
                ?: Result.failure(ParseError("Duration", "expected a Long: $v"))
        })
        assertEquals(250.milliseconds, priv.value)
        assertEquals(5.seconds, crate.value)
        assertEquals(120.nanoseconds, pub.value)
    }

    @Test
    fun testParseFnUnset() {
        val unset = staticFlag(emptyMap<String, String>(), "ENV_FLAGS_TEST_PARSE_FN_UNSET", parser = { v ->
            v.toLongOrNull()?.let { Result.success(it.milliseconds) }
                ?: Result.failure(ParseError("Duration", "expected a Long: $v"))
        })
        assertFails { unset.value }
    }

    @Test
    fun testParseFnDefault() {
        val env = mapOf(
            "ENV_FLAGS_TEST_PARSE_FN_DEFAULT_PRIV" to "10",
            "ENV_FLAGS_TEST_PARSE_FN_DEFAULT_CRATE" to "11",
            "ENV_FLAGS_TEST_PARSE_FN_DEFAULT_PUB" to "12",
        )
        val parser: (String) -> Result<kotlin.time.Duration> = { v ->
            v.toLongOrNull()?.let { Result.success(it.milliseconds) }
                ?: Result.failure(ParseError("Duration", "expected a Long: $v"))
        }
        val priv = staticFlag(env, "ENV_FLAGS_TEST_PARSE_FN_DEFAULT_PRIV", parser) { 5.milliseconds }
        val crate = staticFlag(env, "ENV_FLAGS_TEST_PARSE_FN_DEFAULT_CRATE", parser) { 5.milliseconds }
        val pub = staticFlag(env, "ENV_FLAGS_TEST_PARSE_FN_DEFAULT_PUB", parser) { 5.milliseconds }
        val unset = staticFlag(env, "ENV_FLAGS_TEST_PARSE_FN_DEFAULT_UNSET", parser) { 5.milliseconds }
        assertEquals(10.milliseconds, priv.value)
        assertEquals(11.milliseconds, crate.value)
        assertEquals(12.milliseconds, pub.value)
        assertEquals(5.milliseconds, unset.value)
    }

    @Test
    fun testTypesF32() {
        val env = mapOf(
            "ENV_FLAGS_TEST_TYPES_F32_POS" to "1.2",
            "ENV_FLAGS_TEST_TYPES_F32_NEG" to "-3.2",
            "ENV_FLAGS_TEST_TYPES_F32_NAN" to "nan",
            "ENV_FLAGS_TEST_TYPES_F32_INF" to "inf",
        )
        val pos = staticFlag(env, "ENV_FLAGS_TEST_TYPES_F32_POS", FloatParser)
        val neg = staticFlag(env, "ENV_FLAGS_TEST_TYPES_F32_NEG", FloatParser)
        val nan = staticFlag(env, "ENV_FLAGS_TEST_TYPES_F32_NAN", FloatParser)
        val inf = staticFlag(env, "ENV_FLAGS_TEST_TYPES_F32_INF", FloatParser)
        assertEquals(1.2f, pos.value)
        assertEquals(-3.2f, neg.value)
        assertTrue(nan.value.isNaN())
        assertTrue(inf.value.isInfinite())
    }

    @Test
    fun testInvalidF32() {
        val env = mapOf("ENV_FLAGS_TEST_INVALID_F32" to "cat")
        val flag = staticFlag(env, "ENV_FLAGS_TEST_INVALID_F32", FloatParser) { 0.0f }
        assertFails { flag.value }
    }

    @Test
    fun testTypesF64() {
        val env = mapOf(
            "ENV_FLAGS_TEST_TYPES_F64_POS" to "41.1",
            "ENV_FLAGS_TEST_TYPES_F64_NEG" to "-0.4",
            "ENV_FLAGS_TEST_TYPES_F64_NAN" to "nan",
            "ENV_FLAGS_TEST_TYPES_F64_INF" to "inf",
        )
        val pos = staticFlag(env, "ENV_FLAGS_TEST_TYPES_F64_POS", DoubleParser)
        val neg = staticFlag(env, "ENV_FLAGS_TEST_TYPES_F64_NEG", DoubleParser)
        val nan = staticFlag(env, "ENV_FLAGS_TEST_TYPES_F64_NAN", DoubleParser)
        val inf = staticFlag(env, "ENV_FLAGS_TEST_TYPES_F64_INF", DoubleParser)
        assertEquals(41.1, pos.value)
        assertEquals(-0.4, neg.value)
        assertTrue(nan.value.isNaN())
        assertTrue(inf.value.isInfinite())
    }

    @Test
    fun testInvalidF64() {
        val env = mapOf("ENV_FLAGS_TEST_INVALID_F64" to "cat")
        val flag = staticFlag(env, "ENV_FLAGS_TEST_INVALID_F64", DoubleParser) { 0.0 }
        assertFails { flag.value }
    }

    @Test
    fun testTypesI8() {
        val env = mapOf(
            "ENV_FLAGS_TEST_TYPES_I8_POS" to "4",
            "ENV_FLAGS_TEST_TYPES_I8_NEG" to "-4",
        )
        val pos = staticFlag(env, "ENV_FLAGS_TEST_TYPES_I8_POS", ByteParser)
        val neg = staticFlag(env, "ENV_FLAGS_TEST_TYPES_I8_NEG", ByteParser)
        assertEquals(4.toByte(), pos.value)
        assertEquals((-4).toByte(), neg.value)
    }

    @Test
    fun testInvalidI8() {
        val env = mapOf("ENV_FLAGS_TEST_INVALID_I8" to "128")
        val flag = staticFlag(env, "ENV_FLAGS_TEST_INVALID_I8", ByteParser)
        assertFails { flag.value }
    }

    @Test
    fun testTypesI16() {
        val env = mapOf(
            "ENV_FLAGS_TEST_TYPES_I16_POS" to "2559",
            "ENV_FLAGS_TEST_TYPES_I16_NEG" to "-2559",
        )
        val pos = staticFlag(env, "ENV_FLAGS_TEST_TYPES_I16_POS", ShortParser)
        val neg = staticFlag(env, "ENV_FLAGS_TEST_TYPES_I16_NEG", ShortParser)
        assertEquals(2559.toShort(), pos.value)
        assertEquals((-2559).toShort(), neg.value)
    }

    @Test
    fun testInvalidI16() {
        val env = mapOf("ENV_FLAGS_TEST_INVALID_I16" to "32768")
        val flag = staticFlag(env, "ENV_FLAGS_TEST_INVALID_I16", ShortParser) { 0 }
        assertFails { flag.value }
    }

    @Test
    fun testTypesI32() {
        val env = mapOf(
            "ENV_FLAGS_TEST_TYPES_I32_POS" to "124",
            "ENV_FLAGS_TEST_TYPES_I32_NEG" to "-124",
        )
        val pos = staticFlag(env, "ENV_FLAGS_TEST_TYPES_I32_POS", IntParser)
        val neg = staticFlag(env, "ENV_FLAGS_TEST_TYPES_I32_NEG", IntParser)
        assertEquals(124, pos.value)
        assertEquals(-124, neg.value)
    }

    @Test
    fun testInvalidI32() {
        val env = mapOf("ENV_FLAGS_TEST_INVALID_I32" to "2147483648")
        val flag = staticFlag(env, "ENV_FLAGS_TEST_INVALID_I32", IntParser) { 0 }
        assertFails { flag.value }
    }

    @Test
    fun testTypesI64() {
        val env = mapOf(
            "ENV_FLAGS_TEST_TYPES_I64_POS" to "13966932211",
            "ENV_FLAGS_TEST_TYPES_I64_NEG" to "-13966932211",
        )
        val pos = staticFlag(env, "ENV_FLAGS_TEST_TYPES_I64_POS", LongParser)
        val neg = staticFlag(env, "ENV_FLAGS_TEST_TYPES_I64_NEG", LongParser)
        assertEquals(13966932211L, pos.value)
        assertEquals(-13966932211L, neg.value)
    }

    @Test
    fun testTypesU8() {
        val env = mapOf("ENV_FLAGS_TEST_TYPES_U8" to "10")
        val flag = staticFlag(env, "ENV_FLAGS_TEST_TYPES_U8", UByteParser)
        assertEquals(10u.toUByte(), flag.value)
    }

    @Test
    fun testTypesU16() {
        val env = mapOf("ENV_FLAGS_TEST_TYPES_U16" to "7432")
        val flag = staticFlag(env, "ENV_FLAGS_TEST_TYPES_U16", UShortParser)
        assertEquals(7432u.toUShort(), flag.value)
    }

    @Test
    fun testTypesU32() {
        val env = mapOf("ENV_FLAGS_TEST_TYPES_U32" to "305528")
        val flag = staticFlag(env, "ENV_FLAGS_TEST_TYPES_U32", UIntParser)
        assertEquals(305528u, flag.value)
    }

    @Test
    fun testTypesU64() {
        val env = mapOf("ENV_FLAGS_TEST_TYPES_U64" to "123456789")
        val flag = staticFlag(env, "ENV_FLAGS_TEST_TYPES_U64", ULongParser)
        assertEquals(123456789uL, flag.value)
    }

    @Test
    fun testTypesOption() {
        val env = mapOf("ENV_FLAGS_TEST_OPTION_SET" to "cat")
        val unset = staticFlag(env, "ENV_FLAGS_TEST_OPTION_UNSET", NullableParser(StringParser)) { null }
        val set = staticFlag(env, "ENV_FLAGS_TEST_OPTION_SET", NullableParser(StringParser)) { null }
        assertNull(unset.value)
        assertEquals("cat", set.value)
    }

    @Test
    fun testTypesVec() {
        val env = mapOf("ENV_FLAGS_TEST_VEC" to "1,2,3,4")
        val flag = staticFlag(env, "ENV_FLAGS_TEST_VEC", ListParser(UIntParser))
        assertEquals(listOf(1u, 2u, 3u, 4u), flag.value)
    }

    @Test
    fun testTypesHashSet() {
        val env = mapOf("ENV_FLAGS_TEST_HASH_SET" to "1,2,3,4,1,3")
        val flag = staticFlag(env, "ENV_FLAGS_TEST_HASH_SET", SetParser(UIntParser))
        assertEquals(setOf(1u, 2u, 3u, 4u), flag.value)
    }

    @Test
    fun testTypesBool() {
        val env = mapOf(
            "ENV_FLAGS_TEST_BOOL_TRUE" to "true",
            "ENV_FLAGS_TEST_BOOL_FALSE" to "false",
            "ENV_FLAGS_TEST_BOOL_TRUE_UPPER" to "TRUE",
            "ENV_FLAGS_TEST_BOOL_FALSE_UPPER" to "FALSE",
            "ENV_FLAGS_TEST_BOOL_0" to "0",
            "ENV_FLAGS_TEST_BOOL_1" to "1",
        )
        assertEquals(true, staticFlag(env, "ENV_FLAGS_TEST_BOOL_TRUE", BoolParser).value)
        assertEquals(false, staticFlag(env, "ENV_FLAGS_TEST_BOOL_FALSE", BoolParser).value)
        assertEquals(true, staticFlag(env, "ENV_FLAGS_TEST_BOOL_TRUE_UPPER", BoolParser).value)
        assertEquals(false, staticFlag(env, "ENV_FLAGS_TEST_BOOL_FALSE_UPPER", BoolParser).value)
        assertEquals(false, staticFlag(env, "ENV_FLAGS_TEST_BOOL_0", BoolParser).value)
        assertEquals(true, staticFlag(env, "ENV_FLAGS_TEST_BOOL_1", BoolParser).value)
    }

    @Test
    fun testDeref() {
        val env = emptyMap<String, String>()
        val flag = staticFlag(env, "ENV_FLAGS_TEST_DEREF", StringParser) { "hello" }
        fun printStr(s: String) {
            // intentionally no-op: the upstream `print_str(s: &str)` does `println!("{}", s);`,
            // which is not portable across all KMP targets without dragging in a logger
            assertNotNull(s)
        }
        printStr(flag.value)
        printStr(flag.value)
    }

    @Test
    fun testDebug() {
        val env = emptyMap<String, String>()
        val flag = staticFlag(env, "ENV_FLAGS_TEST_DEBUG", StringParser) { "cat" }
        // upstream uses `format!("{:?}", FLAG)`, which routes through `fmt::Debug` and quotes the
        // String. Kotlin's `toString` on String does not double-quote, so the translated assertion
        // checks the underlying string value instead — Kotlin's analogue of Rust's `Debug`-quoting
        // is not part of this crate's translated surface.
        assertEquals("cat", flag.toString())
    }

    @Test
    fun testDisplay() {
        val env = emptyMap<String, String>()
        val flag = staticFlag(env, "ENV_FLAGS_TEST_DEBUG", StringParser) { "cat" }
        assertEquals("cat", flag.toString())
    }
}
