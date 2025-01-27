package io.smallrye.common.net;

import static java.lang.Math.min;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import io.smallrye.common.constraint.Assert;

/**
 * A Classless Inter-Domain Routing address. This is the combination of an IP address and a netmask.
 */
public final class CidrAddress implements Serializable, Comparable<CidrAddress> {
    private static final long serialVersionUID = -6548529324373774149L;

    /**
     * The CIDR address representing all IPv4 addresses.
     */
    public static final CidrAddress INET4_ANY_CIDR = new CidrAddress(Inet.INET4_ANY, 0);

    /**
     * The CIDR address representing all IPv6 addresses.
     */
    public static final CidrAddress INET6_ANY_CIDR = new CidrAddress(Inet.INET6_ANY, 0);

    /**
     * The base network address of this CIDR address.
     */
    private final InetAddress networkAddress;
    /**
     * The cached bytes of this address.
     */
    private final byte[] cachedBytes;
    /**
     * The number of netmask bits.
     */
    private final int netmaskBits;
    /**
     * The cached broadcast address.
     */
    private Inet4Address broadcast;
    /**
     * The cached to-string value of this object.
     */
    private String toString;
    /**
     * The cached hash code of this object.
     */
    private int hashCode;

    private CidrAddress(final InetAddress networkAddress, final int netmaskBits) {
        this.networkAddress = networkAddress;
        cachedBytes = networkAddress.getAddress();
        this.netmaskBits = netmaskBits;
    }

    /**
     * Create a new CIDR address.
     *
     * @param networkAddress the network address (must not be {@code null})
     * @param netmaskBits the netmask bits (0-32 for IPv4, or 0-128 for IPv6)
     * @return the CIDR address (not {@code null})
     */
    public static CidrAddress create(InetAddress networkAddress, int netmaskBits) {
        Assert.checkNotNullParam("networkAddress", networkAddress);
        Assert.checkMinimumParameter("netmaskBits", 0, netmaskBits);
        int scopeId = Inet.getScopeId(networkAddress);
        if (networkAddress instanceof Inet4Address) {
            Assert.checkMaximumParameter("netmaskBits", 32, netmaskBits);
            if (netmaskBits == 0) {
                return INET4_ANY_CIDR;
            }
        } else if (networkAddress instanceof Inet6Address) {
            Assert.checkMaximumParameter("netmaskBits", 128, netmaskBits);
            if (netmaskBits == 0 && scopeId == 0) {
                return INET6_ANY_CIDR;
            }
        } else {
            throw Assert.unreachableCode();
        }
        final byte[] bytes = networkAddress.getAddress();
        maskBits0(bytes, netmaskBits);
        String name = Inet.toOptimalString(bytes);
        try {
            if (bytes.length == 4) {
                return new CidrAddress(InetAddress.getByAddress(name, bytes), netmaskBits);
            } else {
                return new CidrAddress(Inet6Address.getByAddress(name, bytes, scopeId), netmaskBits);
            }
        } catch (UnknownHostException e) {
            throw Assert.unreachableCode();
        }
    }

    /**
     * Create a new CIDR address.
     *
     * @param addressBytes the network address bytes (must not be {@code null}, must be 4 bytes for IPv4 or 16 bytes for
     *        IPv6)
     * @param netmaskBits the netmask bits (0-32 for IPv4, or 0-128 for IPv6)
     * @return the CIDR address (not {@code null})
     */
    public static CidrAddress create(byte[] addressBytes, int netmaskBits) {
        return create(addressBytes, netmaskBits, true);
    }

    static CidrAddress create(byte[] addressBytes, int netmaskBits, boolean clone) {
        Assert.checkNotNullParam("networkAddress", addressBytes);
        Assert.checkMinimumParameter("netmaskBits", 0, netmaskBits);
        final int length = addressBytes.length;
        if (length == 4) {
            Assert.checkMaximumParameter("netmaskBits", 32, netmaskBits);
            if (netmaskBits == 0) {
                return INET4_ANY_CIDR;
            }
        } else if (length == 16) {
            Assert.checkMaximumParameter("netmaskBits", 128, netmaskBits);
            if (netmaskBits == 0) {
                return INET6_ANY_CIDR;
            }
        } else {
            throw Messages.msg.invalidAddressBytes(length);
        }
        final byte[] bytes = clone ? addressBytes.clone() : addressBytes;
        maskBits0(bytes, netmaskBits);
        String name = Inet.toOptimalString(bytes);
        try {
            return new CidrAddress(InetAddress.getByAddress(name, bytes), netmaskBits);
        } catch (UnknownHostException e) {
            throw Assert.unreachableCode();
        }
    }

    /**
     * Determine if this CIDR address matches the given address.
     *
     * @param address the address to test
     * @return {@code true} if the address matches, {@code false} otherwise
     */
    public boolean matches(InetAddress address) {
        Assert.checkNotNullParam("address", address);
        if (address instanceof Inet4Address) {
            return matches((Inet4Address) address);
        } else if (address instanceof Inet6Address) {
            return matches((Inet6Address) address);
        } else {
            throw Assert.unreachableCode();
        }
    }

    /**
     * Determine if this CIDR address matches the given address bytes.
     *
     * @param bytes the address bytes to test
     * @return {@code true} if the address bytes match, {@code false} otherwise
     */
    public boolean matches(byte[] bytes) {
        return matches(bytes, 0);
    }

    /**
     * Determine if this CIDR address matches the given address bytes.
     *
     * @param bytes the address bytes to test
     * @param scopeId the scope ID, or 0 to match no scope
     * @return {@code true} if the address bytes match, {@code false} otherwise
     */
    public boolean matches(byte[] bytes, int scopeId) {
        Assert.checkNotNullParam("bytes", bytes);
        return cachedBytes.length == bytes.length && (getScopeId() == 0 || getScopeId() == scopeId)
                && bitsMatch(cachedBytes, bytes, netmaskBits);
    }

    /**
     * Determine if this CIDR address matches the given address.
     *
     * @param address the address to test
     * @return {@code true} if the address matches, {@code false} otherwise
     */
    public boolean matches(Inet4Address address) {
        Assert.checkNotNullParam("address", address);
        return networkAddress instanceof Inet4Address && bitsMatch(cachedBytes, address.getAddress(), netmaskBits);
    }

    /**
     * Determine if this CIDR address matches the given address.
     *
     * @param address the address to test
     * @return {@code true} if the address matches, {@code false} otherwise
     */
    public boolean matches(Inet6Address address) {
        Assert.checkNotNullParam("address", address);
        return networkAddress instanceof Inet6Address && bitsMatch(cachedBytes, address.getAddress(), netmaskBits)
                && (getScopeId() == 0 || getScopeId() == address.getScopeId());
    }

    /**
     * Determine if this CIDR address matches the given CIDR address. This will be true only when the given CIDR
     * block is wholly enclosed by this one.
     *
     * @param address the address to test
     * @return {@code true} if the given block is enclosed by this one, {@code false} otherwise
     */
    public boolean matches(CidrAddress address) {
        Assert.checkNotNullParam("address", address);
        return netmaskBits <= address.netmaskBits && matches(address.cachedBytes)
                && (getScopeId() == 0 || getScopeId() == address.getScopeId());
    }

    /**
     * Get the network address. The returned address has a resolved name consisting of the most compact valid string
     * representation of the network of this CIDR address.
     *
     * @return the network address (not {@code null})
     */
    public InetAddress getNetworkAddress() {
        return networkAddress;
    }

    /**
     * Get the broadcast address for this CIDR block. If the block has no broadcast address (either because it is IPv6
     * or it is too small) then {@code null} is returned.
     *
     * @return the broadcast address for this CIDR block, or {@code null} if there is none
     */
    public Inet4Address getBroadcastAddress() {
        final Inet4Address broadcast = this.broadcast;
        if (broadcast == null) {
            final int netmaskBits = this.netmaskBits;
            if (netmaskBits >= 31) {
                // definitely IPv6 or too small
                return null;
            }
            // still maybe IPv6
            final byte[] cachedBytes = this.cachedBytes;
            if (cachedBytes.length == 4) {
                // calculate
                if (netmaskBits == 0) {
                    return this.broadcast = Inet.INET4_BROADCAST;
                } else {
                    final byte[] bytes = maskBits1(cachedBytes.clone(), netmaskBits);
                    try {
                        return this.broadcast = (Inet4Address) InetAddress.getByAddress(Inet.toOptimalString(bytes), bytes);
                    } catch (UnknownHostException e) {
                        throw Assert.unreachableCode();
                    }
                }
            }
            return null;
        }
        return broadcast;
    }

    /**
     * Get the netmask bits. This will be in the range 0-32 for IPv4 addresses, and 0-128 for IPv6 addresses.
     *
     * @return the netmask bits
     */
    public int getNetmaskBits() {
        return netmaskBits;
    }

    /**
     * Get the match address scope ID (if it is an IPv6 address).
     *
     * @return the scope ID, or 0 if there is none or the address is an IPv4 address
     */
    public int getScopeId() {
        return Inet.getScopeId(getNetworkAddress());
    }

    public int compareTo(final CidrAddress other) {
        Assert.checkNotNullParam("other", other);
        if (this == other)
            return 0;
        return compareAddressBytesTo(other.cachedBytes, other.netmaskBits, other.getScopeId());
    }

    /**
     * Compare the bytes of this address to the bytes in the given array.
     *
     * @param otherBytes the bytes to compare to (must not be {@code null})
     * @param otherNetmaskBits the netmask bits for the byte array
     * @param scopeId the IPv6 scope ID, if any, or else zero
     * @return {@code -1}, {@code 0}, or {@code 1} if this address is less than, equal to, or greater than the given address
     */
    public int compareAddressBytesTo(final byte[] otherBytes, final int otherNetmaskBits, final int scopeId) {
        Assert.checkNotNullParam("bytes", otherBytes);
        final int otherLength = otherBytes.length;
        if (otherLength != 4 && otherLength != 16) {
            throw Messages.msg.invalidAddressBytes(otherLength);
        }
        // IPv4 before IPv6
        final byte[] cachedBytes = this.cachedBytes;
        int res = Integer.compare(cachedBytes.length, otherLength);
        if (res != 0)
            return res;
        res = Integer.compare(scopeId, getScopeId());
        if (res != 0)
            return res;
        // sorted numerically with long matches coming later
        final int netmaskBits = this.netmaskBits;
        int commonPrefix = min(netmaskBits, otherNetmaskBits);
        // compare byte-wise as far as we can
        int i = 0;
        while (commonPrefix >= 8) {
            res = Integer.compare(Byte.toUnsignedInt(cachedBytes[i]), Byte.toUnsignedInt(otherBytes[i]));
            if (res != 0)
                return res;
            i++;
            commonPrefix -= 8;
        }
        while (commonPrefix > 0) {
            final int bit = 1 << commonPrefix;
            res = Integer.compare(cachedBytes[i] & bit, otherBytes[i] & bit);
            if (res != 0)
                return res;
            commonPrefix--;
        }
        // common prefix is a match; now the shortest mask wins
        return Integer.compare(netmaskBits, otherNetmaskBits);
    }

    /**
     * {@return {@code true} if this address is equal to the given object, or {@code false} if it is not}
     *
     * @param obj the other address
     */
    public boolean equals(final Object obj) {
        return obj instanceof CidrAddress && equals((CidrAddress) obj);
    }

    /**
     * {@return {@code true} if this address is equal to the given address, or {@code false} if it is not}
     *
     * @param obj the other address
     */
    public boolean equals(final CidrAddress obj) {
        return obj == this || obj != null && netmaskBits == obj.netmaskBits && Arrays.equals(cachedBytes, obj.cachedBytes);
    }

    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = netmaskBits * 19 + Arrays.hashCode(cachedBytes);
            if (hashCode == 0) {
                hashCode = 1;
            }
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    public String toString() {
        final String toString = this.toString;
        if (toString == null) {
            final int scopeId = getScopeId();
            if (scopeId == 0) {
                return this.toString = String.format("%s/%d", Inet.toOptimalString(cachedBytes), Integer.valueOf(netmaskBits));
            } else {
                return this.toString = String.format("%s%%%d/%d", Inet.toOptimalString(cachedBytes), Integer.valueOf(scopeId),
                        Integer.valueOf(netmaskBits));
            }
        }
        return toString;
    }

    /**
     * {@return the serialization replacement for this object}
     */
    Object writeReplace() {
        return new Ser(cachedBytes, netmaskBits);
    }

    static final class Ser implements Serializable {
        private static final long serialVersionUID = 6367919693596329038L;

        final byte[] b;
        final int m;

        Ser(final byte[] b, final int m) {
            this.b = b;
            this.m = m;
        }

        Object readResolve() {
            return create(b, m, false);
        }
    }

    private static boolean bitsMatch(byte[] address, byte[] test, int bits) {
        final int length = address.length;
        assert length == test.length;
        // bytes are in big-endian form.
        int i = 0;
        while (bits >= 8 && i < length) {
            if (address[i] != test[i]) {
                return false;
            }
            i++;
            bits -= 8;
        }
        if (bits > 0) {
            assert bits < 8;
            int mask = 0xff << 8 - bits;
            if ((address[i] & 0xff & mask) != (test[i] & 0xff & mask)) {
                return false;
            }
        }
        return true;
    }

    private static byte[] maskBits0(byte[] address, int bits) {
        final int length = address.length;
        // bytes are in big-endian form.
        int i = 0;
        while (bits >= 8 && i < length) {
            i++;
            bits -= 8;
        }
        if (bits > 0) {
            assert bits < 8;
            int mask = 0xff << 8 - bits;
            address[i++] &= mask;
        }
        while (i < length) {
            address[i++] = 0;
        }
        return address;
    }

    private static byte[] maskBits1(byte[] address, int bits) {
        final int length = address.length;
        // bytes are in big-endian form.
        int i = 0;
        while (bits >= 8 && i < length) {
            i++;
            bits -= 8;
        }
        if (bits > 0) {
            assert bits < 8;
            int mask = 0xff >>> 8 - bits;
            address[i++] |= mask;
        }
        while (i < length) {
            address[i++] = (byte) 0xff;
        }
        return address;
    }
}
