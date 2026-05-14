// port-lint: ignore (Kotlin counterparts of the std::net and std::path types referenced by src/lib.rs)
package io.github.kotlinmania.envflags

/** Wrapper for a filesystem path string, mirroring the upstream `PathBuf` use. */
public data class PathBuf(val path: String) {
    override fun toString(): String = path

    public companion object {
        public fun from(path: String): PathBuf = PathBuf(path)
    }
}

/** IPv4 address, four octets in network order. */
public data class Ipv4Addr(val a: UByte, val b: UByte, val c: UByte, val d: UByte) {
    override fun toString(): String = "$a.$b.$c.$d"

    public companion object {
        public fun parse(text: String): Ipv4Addr {
            val parts = text.split(".")
            require(parts.size == 4) { "invalid IPv4 address" }
            return Ipv4Addr(
                a = parts[0].toUByte(),
                b = parts[1].toUByte(),
                c = parts[2].toUByte(),
                d = parts[3].toUByte(),
            )
        }
    }
}

/** IPv6 address, eight 16-bit groups. */
public data class Ipv6Addr(val groups: List<UShort>) {
    init {
        require(groups.size == 8) { "IPv6 address must contain 8 groups" }
    }

    override fun toString(): String =
        groups.joinToString(":") { it.toUInt().toString(16).padStart(4, '0').uppercase() }

    public companion object {
        public fun parse(text: String): Ipv6Addr {
            val expanded: List<String> = if ("::" in text) {
                val (left, right) = text.split("::", limit = 2)
                val leftParts = if (left.isEmpty()) emptyList() else left.split(":")
                val rightParts = if (right.isEmpty()) emptyList() else right.split(":")
                val missing = 8 - (leftParts.size + rightParts.size)
                require(missing >= 0) { "IPv6 address has too many groups" }
                leftParts + List(missing) { "0" } + rightParts
            } else {
                text.split(":")
            }
            require(expanded.size == 8) { "IPv6 address must contain 8 groups" }
            return Ipv6Addr(expanded.map { it.toUInt(16).toUShort() })
        }
    }
}

/** Tagged union over [Ipv4Addr] and [Ipv6Addr]. */
public sealed class IpAddr {
    public data class V4(val addr: Ipv4Addr) : IpAddr() {
        override fun toString(): String = addr.toString()
    }

    public data class V6(val addr: Ipv6Addr) : IpAddr() {
        override fun toString(): String = addr.toString()
    }

    public companion object {
        public fun parse(text: String): IpAddr =
            if (":" in text) V6(Ipv6Addr.parse(text)) else V4(Ipv4Addr.parse(text))
    }
}

/** A pairing of [IpAddr] with a port number. */
public data class SocketAddr(val ip: IpAddr, val port: UShort) {
    override fun toString(): String = "$ip:$port"

    public companion object {
        public fun parse(text: String): SocketAddr {
            val lastColon = text.lastIndexOf(':')
            require(lastColon > 0) { "invalid socket address" }
            val host = text.substring(0, lastColon)
            val port = text.substring(lastColon + 1).toUShort()
            return SocketAddr(IpAddr.parse(host), port)
        }
    }
}
