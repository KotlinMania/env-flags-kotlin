// port-lint: source src/lib.rs
package io.github.kotlinmania.envflags

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

private fun src(vararg pairs: Pair<String, String>): EnvSource = MapEnvSource(pairs.toMap())

class LibTest {
    @Test
    fun testKeyTypeSet() {
        val source = src(
            "ENV_FLAGS_TEST_KEY_TYPE_SET_PRIV" to "80",
            "ENV_FLAGS_TEST_KEY_TYPE_SET_CRATE" to "81",
            "ENV_FLAGS_TEST_KEY_TYPE_SET_PUB" to "82",
        )
        val priv = envFlag("ENV_FLAGS_TEST_KEY_TYPE_SET_PRIV", Parsers.uShort, source)
        val crate = envFlag("ENV_FLAGS_TEST_KEY_TYPE_SET_CRATE", Parsers.uShort, source)
        val pub = envFlag("ENV_FLAGS_TEST_KEY_TYPE_SET_PUB", Parsers.uShort, source)
        assertEquals(80u.toUShort(), priv.value)
        assertEquals(81u.toUShort(), crate.value)
        assertEquals(82u.toUShort(), pub.value)
    }

    @Test
    fun testKeyTypeUnset() {
        val pub = envFlag(
            "ENV_FLAGS_TEST_KEY_TYPE_UNSET",
            Parsers.uShort,
            source = MapEnvSource(emptyMap()),
        )
        assertFailsWith<IllegalStateException> { pub.value }
    }

    @Test
    fun testDefault() {
        val source = src(
            "ENV_FLAGS_TEST_DEFAULT_SET_PRIV" to "goodbye",
            "ENV_FLAGS_TEST_DEFAULT_SET_CRATE" to "goodbye",
            "ENV_FLAGS_TEST_DEFAULT_SET_PUB" to "goodbye",
        )
        val priv = envFlag("ENV_FLAGS_TEST_DEFAULT_SET_PRIV", Parsers.string, source) { "hello" }
        val crate = envFlag("ENV_FLAGS_TEST_DEFAULT_SET_CRATE", Parsers.string, source) { "hello" }
        val pub = envFlag("ENV_FLAGS_TEST_DEFAULT_SET_PUB", Parsers.string, source) { "hello" }
        val unset = envFlag("ENV_FLAGS_TEST_DEFAULT_UNSET", Parsers.string, source) { "world" }

        assertEquals("goodbye", priv.value)
        assertEquals("goodbye", crate.value)
        assertEquals("goodbye", pub.value)
        assertEquals("world", unset.value)
    }

    @Test
    fun testParseFn() {
        val source = src(
            "ENV_FLAGS_TEST_PARSE_FN_PRIV" to "250",
            "ENV_FLAGS_TEST_PARSE_FN_CRATE" to "5",
            "ENV_FLAGS_TEST_PARSE_FN_PUB" to "120",
        )
        val priv = envFlag(
            "ENV_FLAGS_TEST_PARSE_FN_PRIV",
            parse = { it.toLong().milliseconds },
            source = source,
        )
        val crate = envFlag(
            "ENV_FLAGS_TEST_PARSE_FN_CRATE",
            parse = { it.toLong().seconds },
            source = source,
        )
        val pub = envFlag(
            "ENV_FLAGS_TEST_PARSE_FN_PUB",
            parse = { it.toLong().nanoseconds },
            source = source,
        )
        assertEquals(250.milliseconds, priv.value)
        assertEquals(5.seconds, crate.value)
        assertEquals(120.nanoseconds, pub.value)
    }

    @Test
    fun testParseFnUnset() {
        val unset = envFlag(
            "ENV_FLAGS_TEST_PARSE_FN_UNSET",
            parse = { it.toLong().milliseconds },
            source = MapEnvSource(emptyMap()),
        )
        assertFailsWith<IllegalStateException> { unset.value }
    }

    @Test
    fun testParseFnDefault() {
        val source = src(
            "ENV_FLAGS_TEST_PARSE_FN_DEFAULT_PRIV" to "10",
            "ENV_FLAGS_TEST_PARSE_FN_DEFAULT_CRATE" to "11",
            "ENV_FLAGS_TEST_PARSE_FN_DEFAULT_PUB" to "12",
        )
        val priv = envFlag(
            "ENV_FLAGS_TEST_PARSE_FN_DEFAULT_PRIV",
            parse = { it.toLong().milliseconds },
            source = source,
        ) { 5.milliseconds }
        val crate = envFlag(
            "ENV_FLAGS_TEST_PARSE_FN_DEFAULT_CRATE",
            parse = { it.toLong().milliseconds },
            source = source,
        ) { 5.milliseconds }
        val pub = envFlag(
            "ENV_FLAGS_TEST_PARSE_FN_DEFAULT_PUB",
            parse = { it.toLong().milliseconds },
            source = source,
        ) { 5.milliseconds }
        val unset = envFlag(
            "ENV_FLAGS_TEST_PARSE_FN_DEFAULT_UNSET",
            parse = { it.toLong().milliseconds },
            source = source,
        ) { 5.milliseconds }

        assertEquals(10.milliseconds, priv.value)
        assertEquals(11.milliseconds, crate.value)
        assertEquals(12.milliseconds, pub.value)
        assertEquals(5.milliseconds, unset.value)
    }

    @Test
    fun testTypesFloat() {
        val source = src(
            "ENV_FLAGS_TEST_TYPES_F32_POS" to "1.2",
            "ENV_FLAGS_TEST_TYPES_F32_NEG" to "-3.2",
            "ENV_FLAGS_TEST_TYPES_F32_NAN" to "nan",
            "ENV_FLAGS_TEST_TYPES_F32_INF" to "inf",
        )
        assertEquals(1.2f, envFlag("ENV_FLAGS_TEST_TYPES_F32_POS", Parsers.float, source).value)
        assertEquals(-3.2f, envFlag("ENV_FLAGS_TEST_TYPES_F32_NEG", Parsers.float, source).value)
        assertTrue(envFlag("ENV_FLAGS_TEST_TYPES_F32_NAN", Parsers.float, source).value.isNaN())
        assertTrue(envFlag("ENV_FLAGS_TEST_TYPES_F32_INF", Parsers.float, source).value.isInfinite())
    }

    @Test
    fun testInvalidFloat() {
        val flag = envFlag(
            "ENV_FLAGS_TEST_INVALID_F32",
            Parsers.float,
            source = src("ENV_FLAGS_TEST_INVALID_F32" to "cat"),
        ) { 0.0f }
        assertFailsWith<IllegalStateException> { flag.value }
    }

    @Test
    fun testTypesDouble() {
        val source = src(
            "ENV_FLAGS_TEST_TYPES_F64_POS" to "41.1",
            "ENV_FLAGS_TEST_TYPES_F64_NEG" to "-0.4",
            "ENV_FLAGS_TEST_TYPES_F64_NAN" to "nan",
            "ENV_FLAGS_TEST_TYPES_F64_INF" to "inf",
        )
        assertEquals(41.1, envFlag("ENV_FLAGS_TEST_TYPES_F64_POS", Parsers.double, source).value)
        assertEquals(-0.4, envFlag("ENV_FLAGS_TEST_TYPES_F64_NEG", Parsers.double, source).value)
        assertTrue(envFlag("ENV_FLAGS_TEST_TYPES_F64_NAN", Parsers.double, source).value.isNaN())
        assertTrue(envFlag("ENV_FLAGS_TEST_TYPES_F64_INF", Parsers.double, source).value.isInfinite())
    }

    @Test
    fun testInvalidDouble() {
        val flag = envFlag(
            "ENV_FLAGS_TEST_INVALID_F64",
            Parsers.double,
            source = src("ENV_FLAGS_TEST_INVALID_F64" to "cat"),
        ) { 0.0 }
        assertFailsWith<IllegalStateException> { flag.value }
    }

    @Test
    fun testTypesByte() {
        val source = src(
            "ENV_FLAGS_TEST_TYPES_I8_POS" to "4",
            "ENV_FLAGS_TEST_TYPES_I8_NEG" to "-4",
        )
        assertEquals(4, envFlag("ENV_FLAGS_TEST_TYPES_I8_POS", Parsers.byte, source).value.toInt())
        assertEquals(-4, envFlag("ENV_FLAGS_TEST_TYPES_I8_NEG", Parsers.byte, source).value.toInt())
    }

    @Test
    fun testInvalidByte() {
        val flag = envFlag(
            "ENV_FLAGS_TEST_INVALID_I8",
            Parsers.byte,
            source = src("ENV_FLAGS_TEST_INVALID_I8" to "128"),
        )
        assertFailsWith<IllegalStateException> { flag.value }
    }

    @Test
    fun testTypesShort() {
        val source = src(
            "ENV_FLAGS_TEST_TYPES_I16_POS" to "2559",
            "ENV_FLAGS_TEST_TYPES_I16_NEG" to "-2559",
        )
        assertEquals(2559, envFlag("ENV_FLAGS_TEST_TYPES_I16_POS", Parsers.short, source).value.toInt())
        assertEquals(-2559, envFlag("ENV_FLAGS_TEST_TYPES_I16_NEG", Parsers.short, source).value.toInt())
    }

    @Test
    fun testInvalidShort() {
        val flag = envFlag(
            "ENV_FLAGS_TEST_INVALID_I16",
            Parsers.short,
            source = src("ENV_FLAGS_TEST_INVALID_I16" to "32768"),
        ) { 0 }
        assertFailsWith<IllegalStateException> { flag.value }
    }

    @Test
    fun testTypesInt() {
        val source = src(
            "ENV_FLAGS_TEST_TYPES_I32_POS" to "124",
            "ENV_FLAGS_TEST_TYPES_I32_NEG" to "-124",
        )
        assertEquals(124, envFlag("ENV_FLAGS_TEST_TYPES_I32_POS", Parsers.int, source).value)
        assertEquals(-124, envFlag("ENV_FLAGS_TEST_TYPES_I32_NEG", Parsers.int, source).value)
    }

    @Test
    fun testInvalidInt() {
        val flag = envFlag(
            "ENV_FLAGS_TEST_INVALID_I32",
            Parsers.int,
            source = src("ENV_FLAGS_TEST_INVALID_I32" to "2147483648"),
        ) { 0 }
        assertFailsWith<IllegalStateException> { flag.value }
    }

    @Test
    fun testTypesLong() {
        val source = src(
            "ENV_FLAGS_TEST_TYPES_I64_POS" to "13966932211",
            "ENV_FLAGS_TEST_TYPES_I64_NEG" to "-13966932211",
        )
        assertEquals(13966932211L, envFlag("ENV_FLAGS_TEST_TYPES_I64_POS", Parsers.long, source).value)
        assertEquals(-13966932211L, envFlag("ENV_FLAGS_TEST_TYPES_I64_NEG", Parsers.long, source).value)
    }

    @Test
    fun testTypesLong128() {
        val source = src(
            "ENV_FLAGS_TEST_TYPES_I128_POS" to "1020304995959399",
            "ENV_FLAGS_TEST_TYPES_I128_NEG" to "-1020304995959399",
        )
        assertEquals(1020304995959399L, envFlag("ENV_FLAGS_TEST_TYPES_I128_POS", Parsers.long128, source).value)
        assertEquals(-1020304995959399L, envFlag("ENV_FLAGS_TEST_TYPES_I128_NEG", Parsers.long128, source).value)
    }

    @Test
    fun testTypesNativeInt() {
        val source = src(
            "ENV_FLAGS_TEST_TYPES_ISIZE_POS" to "29294",
            "ENV_FLAGS_TEST_TYPES_ISIZE_NEG" to "-29294",
        )
        assertEquals(29294L, envFlag("ENV_FLAGS_TEST_TYPES_ISIZE_POS", Parsers.nativeInt, source).value)
        assertEquals(-29294L, envFlag("ENV_FLAGS_TEST_TYPES_ISIZE_NEG", Parsers.nativeInt, source).value)
    }

    @Test
    fun testTypesUByte() {
        val source = src("ENV_FLAGS_TEST_TYPES_U8" to "10")
        assertEquals(10u.toUByte(), envFlag("ENV_FLAGS_TEST_TYPES_U8", Parsers.uByte, source).value)
    }

    @Test
    fun testTypesUShort() {
        val source = src("ENV_FLAGS_TEST_TYPES_U16" to "7432")
        assertEquals(7432u.toUShort(), envFlag("ENV_FLAGS_TEST_TYPES_U16", Parsers.uShort, source).value)
    }

    @Test
    fun testTypesUInt() {
        val source = src("ENV_FLAGS_TEST_TYPES_U32" to "305528")
        assertEquals(305528u, envFlag("ENV_FLAGS_TEST_TYPES_U32", Parsers.uInt, source).value)
    }

    @Test
    fun testTypesULong() {
        val source = src("ENV_FLAGS_TEST_TYPES_U64" to "123456789")
        assertEquals(123456789uL, envFlag("ENV_FLAGS_TEST_TYPES_U64", Parsers.uLong, source).value)
    }

    @Test
    fun testTypesULong128() {
        val source = src("ENV_FLAGS_TEST_TYPES_U128" to "2919239")
        assertEquals(2919239uL, envFlag("ENV_FLAGS_TEST_TYPES_U128", Parsers.uLong128, source).value)
    }

    @Test
    fun testTypesNativeUInt() {
        val source = src("ENV_FLAGS_TEST_TYPES_USIZE" to "2939")
        assertEquals(2939uL, envFlag("ENV_FLAGS_TEST_TYPES_USIZE", Parsers.nativeUInt, source).value)
    }

    @Test
    fun testTypesIpAddr() {
        val source = src("ENV_FLAGS_TEST_TYPES_IPADDR" to "0.0.0.0")
        val expected = IpAddr.V4(Ipv4Addr.parse("0.0.0.0"))
        assertEquals(expected, envFlag("ENV_FLAGS_TEST_TYPES_IPADDR", Parsers.ipAddr, source).value)
    }

    @Test
    fun testTypesIpv4Addr() {
        val source = src("ENV_FLAGS_TEST_TYPES_IPV4ADDR" to "127.0.0.1")
        val expected = Ipv4Addr.parse("127.0.0.1")
        assertEquals(expected, envFlag("ENV_FLAGS_TEST_TYPES_IPV4ADDR", Parsers.ipv4Addr, source).value)
    }

    @Test
    fun testTypesIpv6Addr() {
        val source = src(
            "ENV_FLAGS_TEST_TYPES_IPV6ADDR" to "2001:0000:130F:0000:0000:09C0:876A:130B",
        )
        val expected = Ipv6Addr.parse("2001:0000:130F:0000:0000:09C0:876A:130B")
        assertEquals(expected, envFlag("ENV_FLAGS_TEST_TYPES_IPV6ADDR", Parsers.ipv6Addr, source).value)
    }

    @Test
    fun testTypesSocketAddr() {
        val source = src("ENV_FLAGS_TEST_TYPES_SOCKETADDR" to "192.168.0.1:8080")
        val expected = SocketAddr.parse("192.168.0.1:8080")
        assertEquals(expected, envFlag("ENV_FLAGS_TEST_TYPES_SOCKETADDR", Parsers.socketAddr, source).value)
    }

    @Test
    fun testTypesPathBuf() {
        val source = src("ENV_FLAGS_TEST_TYPES_PATHBUF" to "/var/lib/file.txt")
        assertEquals(
            PathBuf.from("/var/lib/file.txt"),
            envFlag("ENV_FLAGS_TEST_TYPES_PATHBUF", Parsers.pathBuf, source).value,
        )
    }

    @Test
    fun testTypesOption() {
        val source = src("ENV_FLAGS_TEST_OPTION_SET" to "cat")
        val unset = envFlag(
            "ENV_FLAGS_TEST_OPTION_UNSET",
            Parsers.optional(Parsers.string),
            source,
        ) { null }
        val set = envFlag(
            "ENV_FLAGS_TEST_OPTION_SET",
            Parsers.optional(Parsers.string),
            source,
        ) { null }
        assertNull(unset.value)
        assertEquals("cat", set.value)
    }

    @Test
    fun testTypesList() {
        val source = src("ENV_FLAGS_TEST_VEC" to "1,2,3,4")
        val flag = envFlag("ENV_FLAGS_TEST_VEC", Parsers.list(Parsers.uInt), source)
        assertEquals(listOf(1u, 2u, 3u, 4u), flag.value)
    }

    @Test
    fun testTypesSet() {
        val source = src("ENV_FLAGS_TEST_HASH_SET" to "1,2,3,4,1,3")
        val flag = envFlag("ENV_FLAGS_TEST_HASH_SET", Parsers.set(Parsers.uInt), source)
        assertEquals(setOf(1u, 2u, 3u, 4u), flag.value)
    }

    @Test
    fun testTypesBoolean() {
        val source = src(
            "ENV_FLAGS_TEST_BOOL_TRUE" to "true",
            "ENV_FLAGS_TEST_BOOL_FALSE" to "false",
            "ENV_FLAGS_TEST_BOOL_TRUE_UPPER" to "TRUE",
            "ENV_FLAGS_TEST_BOOL_FALSE_UPPER" to "FALSE",
            "ENV_FLAGS_TEST_BOOL_0" to "0",
            "ENV_FLAGS_TEST_BOOL_1" to "1",
        )
        assertEquals(true, envFlag("ENV_FLAGS_TEST_BOOL_TRUE", Parsers.boolean, source).value)
        assertEquals(false, envFlag("ENV_FLAGS_TEST_BOOL_FALSE", Parsers.boolean, source).value)
        assertEquals(true, envFlag("ENV_FLAGS_TEST_BOOL_TRUE_UPPER", Parsers.boolean, source).value)
        assertEquals(false, envFlag("ENV_FLAGS_TEST_BOOL_FALSE_UPPER", Parsers.boolean, source).value)
        assertEquals(false, envFlag("ENV_FLAGS_TEST_BOOL_0", Parsers.boolean, source).value)
        assertEquals(true, envFlag("ENV_FLAGS_TEST_BOOL_1", Parsers.boolean, source).value)
    }

    @Test
    fun testDeref() {
        val flag = envFlag(
            "ENV_FLAGS_TEST_DEREF",
            Parsers.string,
            source = MapEnvSource(emptyMap()),
        ) { "hello" }

        fun printStr(s: String): String = s

        assertEquals("hello", printStr(flag.value))
        assertEquals("hello", printStr(flag.value))
    }

    @Test
    fun testDebug() {
        val flag = envFlag(
            "ENV_FLAGS_TEST_DEBUG",
            Parsers.string,
            source = MapEnvSource(emptyMap()),
        ) { "cat" }
        assertEquals("cat", flag.toString())
    }

    @Test
    fun testDisplay() {
        val flag = envFlag(
            "ENV_FLAGS_TEST_DEBUG",
            Parsers.string,
            source = MapEnvSource(emptyMap()),
        ) { "cat" }
        assertEquals("cat", flag.toString())
    }
}
