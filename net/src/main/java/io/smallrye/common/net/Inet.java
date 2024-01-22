package io.smallrye.common.net;

import static java.security.AccessController.doPrivileged;

import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.regex.Pattern;

import io.smallrye.common.constraint.Assert;

/**
 * Utilities relating to Internet protocol (a.k.a. "INET" or "IP") address manipulation.
 */
public final class Inet {
    private Inet() {
    }

    /**
     * The "any" address for IPv4.
     */
    public static final Inet4Address INET4_ANY;

    /**
     * The traditional loopback address for IPv4.
     */
    public static final Inet4Address INET4_LOOPBACK;

    /**
     * The broadcast-all address for IPv4.
     */
    public static final Inet4Address INET4_BROADCAST;

    static {
        byte[] bytes = new byte[4];
        // 0.0.0.0
        bytes[0] = 0;
        bytes[1] = 0;
        bytes[2] = 0;
        bytes[3] = 0;
        try {
            INET4_ANY = (Inet4Address) InetAddress.getByAddress("0.0.0.0", bytes);
        } catch (UnknownHostException e) {
            // not possible
            throw new IllegalStateException(e);
        }
        // 127.0.0.1
        bytes[0] = 127;
        bytes[1] = 0;
        bytes[2] = 0;
        bytes[3] = 1;
        try {
            INET4_LOOPBACK = (Inet4Address) InetAddress.getByAddress("127.0.0.1", bytes);
        } catch (UnknownHostException e) {
            // not possible
            throw new IllegalStateException(e);
        }
        // 255.255.255.255
        bytes[0] = (byte) 255;
        bytes[1] = (byte) 255;
        bytes[2] = (byte) 255;
        bytes[3] = (byte) 255;
        try {
            INET4_BROADCAST = (Inet4Address) InetAddress.getByAddress("255.255.255.255", bytes);
        } catch (UnknownHostException e) {
            // not possible
            throw new IllegalStateException(e);
        }
    }

    /**
     * The "any" address for IPv6.
     */
    public static final Inet6Address INET6_ANY = getInet6Address(0, 0, 0, 0, 0, 0, 0, 0);

    /**
     * The loopback address for IPv6.
     */
    public static final Inet6Address INET6_LOOPBACK = getInet6Address(0, 0, 0, 0, 0, 0, 0, 1);

    /**
     * Get the optimal string representation of an IP address. For IPv6 addresses, this representation will be
     * more compact that the default.
     *
     * @param inetAddress the address (must not be {@code null})
     * @return the string representation (not {@code null})
     */
    public static String toOptimalString(InetAddress inetAddress) {
        Assert.checkNotNullParam("inetAddress", inetAddress);
        return inetAddress instanceof Inet6Address ? toOptimalStringV6(inetAddress.getAddress()) : inetAddress.getHostAddress();
    }

    /**
     * Get the optimal string representation of the bytes of an IP address.
     *
     * @param addressBytes the address bytes (must not be {@code null})
     * @return the string representation (not {@code null})
     */
    public static String toOptimalString(byte[] addressBytes) {
        Assert.checkNotNullParam("addressBytes", addressBytes);
        if (addressBytes.length == 4) {
            return (addressBytes[0] & 0xff) + "." + (addressBytes[1] & 0xff) + "." + (addressBytes[2] & 0xff) + "."
                    + (addressBytes[3] & 0xff);
        } else if (addressBytes.length == 16) {
            return toOptimalStringV6(addressBytes);
        } else {
            throw Messages.msg.invalidAddressBytes(addressBytes.length);
        }
    }

    /**
     * Get a string representation of the given address which is suitable for use as the host component of a URL.
     *
     * @param inetAddress the address (must not be {@code null})
     * @param useHostNameIfPresent {@code true} to preserve the host name string in the address, {@code false} to always
     *        give
     *        an IP address string
     * @return the string representation (not {@code null})
     */
    public static String toURLString(InetAddress inetAddress, boolean useHostNameIfPresent) {
        Assert.checkNotNullParam("inetAddress", inetAddress);
        if (useHostNameIfPresent) {
            final String hostName = getHostNameIfResolved(inetAddress);
            if (hostName != null) {
                if (inetAddress instanceof Inet6Address && isInet6Address(hostName)) {
                    return "[" + hostName + "]";
                } else {
                    // return it even if it's an IP address or whatever
                    return hostName;
                }
            }
        }
        if (inetAddress instanceof Inet6Address) {
            return "[" + toOptimalString(inetAddress) + "]";
        } else {
            return toOptimalString(inetAddress);
        }
    }

    /**
     * Get a string representation of the given address bytes which is suitable for use as the host component of a URL.
     *
     * @param addressBytes the address bytes (must not be {@code null})
     * @return the string representation (not {@code null})
     */
    public static String toURLString(byte[] addressBytes) {
        Assert.checkNotNullParam("addressBytes", addressBytes);
        if (addressBytes.length == 4) {
            return (addressBytes[0] & 0xff) + "." + (addressBytes[1] & 0xff) + "." + (addressBytes[2] & 0xff) + "."
                    + (addressBytes[3] & 0xff);
        } else if (addressBytes.length == 16) {
            return "[" + toOptimalStringV6(addressBytes) + "]";
        } else {
            throw Messages.msg.invalidAddressBytes(addressBytes.length);
        }
    }

    /**
     * Get the IPv6 equivalent of the given address. If the address is IPv4 then it is returned as a compatibility
     * address.
     *
     * @param inetAddress the address to convert (must not be {@code null})
     * @return the converted address (not {@code null})
     */
    public static Inet6Address toInet6Address(InetAddress inetAddress) {
        if (inetAddress instanceof Inet6Address) {
            return (Inet6Address) inetAddress;
        } else {
            assert inetAddress instanceof Inet4Address;
            final byte[] addr = new byte[16];
            addr[10] = addr[11] = (byte) 0xff;
            System.arraycopy(inetAddress.getAddress(), 0, addr, 12, 4);
            // get unresolved host name
            try {
                return Inet6Address.getByAddress(getHostNameIfResolved(inetAddress), addr, 0);
            } catch (UnknownHostException e) {
                // not possible
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Get the host name of the given address, if it is resolved. Otherwise, return {@code null}.
     *
     * @param inetAddress the address to check (must not be {@code null})
     * @return the host name, or {@code null} if the address has no host name and is unresolved
     */
    public static String getHostNameIfResolved(InetAddress inetAddress) {
        Assert.checkNotNullParam("inetAddress", inetAddress);
        return getHostNameIfResolved(new InetSocketAddress(inetAddress, 0));
    }

    /**
     * Get the host name of the given address, if it is resolved. Otherwise, return {@code null}.
     *
     * @param socketAddress the socket address to check (must not be {@code null})
     * @return the host name, or {@code null} if the address has no host name and is unresolved
     */
    public static String getHostNameIfResolved(InetSocketAddress socketAddress) {
        Assert.checkNotNullParam("socketAddress", socketAddress);
        final String hostString = socketAddress.getHostString();
        final String toString = socketAddress.toString();
        final int slash = toString.lastIndexOf('/');
        if (slash == 0) {
            // it might be unresolved or it might explicitly be ""
            return hostString.isEmpty() ? "" : null;
        }
        return hostString;
    }

    /**
     * Get a resolved socket address from the given URI.
     *
     * @param uri the URI (must not be {@code null})
     * @param defaultPort the default port to use if none is given (must be in the range {@code 1 ≤ n ≤ 65535}
     * @param addressType the class of the {@code InetAddress} to search for (must not be {@code null})
     * @return the socket address, or {@code null} if the URI does not have a host component
     * @throws UnknownHostException if address resolution failed
     */
    public static InetSocketAddress getResolved(URI uri, int defaultPort, Class<? extends InetAddress> addressType)
            throws UnknownHostException {
        Assert.checkNotNullParam("uri", uri);
        Assert.checkMinimumParameter("defaultPort", 1, defaultPort);
        Assert.checkMaximumParameter("defaultPort", 65535, defaultPort);
        Assert.checkNotNullParam("addressType", addressType);
        final InetAddress resolved = getResolvedInetAddress(uri, addressType);
        if (resolved == null) {
            return null;
        }
        final int uriPort = uri.getPort();
        return uriPort != -1 ? new InetSocketAddress(resolved, uriPort) : new InetSocketAddress(resolved, defaultPort);
    }

    /**
     * Get the resolved socket address from the given URI.
     *
     * @param uri the URI (must not be {@code null})
     * @param defaultPort the default port to use if none is given (must be in the range {@code 1 ≤ n ≤ 65535}
     * @return the socket address, or {@code null} if the URI does not have a host component
     * @throws UnknownHostException if address resolution failed
     */
    public static InetSocketAddress getResolved(URI uri, int defaultPort) throws UnknownHostException {
        return getResolved(uri, defaultPort, InetAddress.class);
    }

    /**
     * Get an Internet address for a URI destination, resolving the host name if necessary.
     *
     * @param uri the destination URI
     * @param <T> the type of the {@code InetAddress} to search for
     * @return the address, or {@code null} if no authority is present in the URI
     * @throws UnknownHostException if the URI host was existent but could not be resolved to a valid address
     */
    public static <T extends InetAddress> T getResolvedInetAddress(URI uri, Class<T> addressType) throws UnknownHostException {
        final String uriHost = uri.getHost();
        if (uriHost == null) {
            return null;
        }
        final int length = uriHost.length();
        if (length == 0) {
            return null;
        }
        return getAddressByNameAndType(uriHost, addressType);
    }

    /**
     * Get an Internet address for a URI destination, resolving the host name if necessary.
     *
     * @param uri the destination URI
     * @return the address, or {@code null} if no authority is present in the URI
     * @throws UnknownHostException if the URI host was existent but could not be resolved to a valid address
     */
    public static InetAddress getResolvedInetAddress(URI uri) throws UnknownHostException {
        return getResolvedInetAddress(uri, InetAddress.class);
    }

    /**
     * Get a copy of the given socket address, but with a resolved address component.
     *
     * @param address the (possibly unresolved) address (must not be {@code null})
     * @return the resolved address (not {@code null})
     * @throws UnknownHostException if address resolution failed
     */
    public static InetSocketAddress getResolved(InetSocketAddress address) throws UnknownHostException {
        return getResolved(address, InetAddress.class);
    }

    /**
     * Get a copy of the given socket address, but with a resolved address component of the given type.
     *
     * @param address the (possibly unresolved) address (must not be {@code null})
     * @param addressType the class of the {@code InetAddress} to search for (must not be {@code null})
     * @return the resolved address (not {@code null})
     * @throws UnknownHostException if address resolution failed, or if no addresses of the given type were found, or
     *         if the given address was already resolved but is not of the given address type
     */
    public static InetSocketAddress getResolved(InetSocketAddress address, Class<? extends InetAddress> addressType)
            throws UnknownHostException {
        Assert.checkNotNullParam("address", address);
        Assert.checkNotNullParam("addressType", addressType);
        if (!address.isUnresolved()) {
            if (!addressType.isInstance(address.getAddress())) {
                // the address part does not match
                throw new UnknownHostException(address.getHostString());
            }
            return address;
        }
        return new InetSocketAddress(getAddressByNameAndType(address.getHostString(), addressType), address.getPort());
    }

    /**
     * Resolve the given host name, returning the first answer with the given address type.
     *
     * @param hostName the host name to resolve (must not be {@code null})
     * @param addressType the class of the {@code InetAddress} to search for (must not be {@code null})
     * @param <T> the type of the {@code InetAddress} to search for
     * @return the resolved address (not {@code null})
     * @throws UnknownHostException if address resolution failed or if no addresses of the given type were found
     */
    public static <T extends InetAddress> T getAddressByNameAndType(String hostName, Class<T> addressType)
            throws UnknownHostException {
        Assert.checkNotNullParam("hostName", hostName);
        Assert.checkNotNullParam("addressType", addressType);
        if (addressType == InetAddress.class) {
            return addressType.cast(InetAddress.getByName(hostName));
        }
        for (InetAddress inetAddress : InetAddress.getAllByName(hostName)) {
            if (addressType.isInstance(inetAddress)) {
                return addressType.cast(inetAddress);
            }
        }
        // no i18n here because this is a "standard" exception
        throw new UnknownHostException(hostName);
    }

    /**
     * Resolve the given host name, returning all answers with the given address type.
     *
     * @param hostName the host name to resolve (must not be {@code null})
     * @param addressType the class of the {@code InetAddress} to search for (must not be {@code null})
     * @param <T> the type of the {@code InetAddress} to search for
     * @return the resolved addresses (not {@code null})
     * @throws UnknownHostException if address resolution failed or if no addresses of the given type were found
     */
    @SuppressWarnings("unchecked")
    public static <T extends InetAddress> T[] getAllAddressesByNameAndType(String hostName, Class<T> addressType)
            throws UnknownHostException {
        Assert.checkNotNullParam("hostName", hostName);
        Assert.checkNotNullParam("addressType", addressType);
        if (addressType == InetAddress.class) {
            // safe because T == InetAddress
            return (T[]) InetAddress.getAllByName(hostName);
        }
        final InetAddress[] addresses = InetAddress.getAllByName(hostName);
        final int length = addresses.length;
        int count = 0;
        for (InetAddress inetAddress : addresses) {
            if (addressType.isInstance(inetAddress)) {
                count++;
            }
        }
        if (count == 0) {
            // no i18n here because this is a "standard" exception
            throw new UnknownHostException(hostName);
        }
        final T[] newArray = (T[]) Array.newInstance(addressType, count);
        if (count == length) {
            //noinspection SuspiciousSystemArraycopy
            System.arraycopy(addresses, 0, newArray, 0, length);
        } else {
            int idx = 0;
            for (InetAddress inetAddress : addresses) {
                if (addressType.isInstance(inetAddress)) {
                    newArray[idx] = addressType.cast(inetAddress);
                }
            }
        }
        return newArray;
    }

    /**
     * Get an IPv4 address from four integer segments. Each segment must be between 0 and 255.
     *
     * @param s1 the first segment
     * @param s2 the second segment
     * @param s3 the third segment
     * @param s4 the fourth segment
     * @return the address (not {@code null})
     */
    public static Inet4Address getInet4Address(int s1, int s2, int s3, int s4) {
        Assert.checkMinimumParameter("s1", 0, s1);
        Assert.checkMaximumParameter("s1", 255, s1);
        Assert.checkMinimumParameter("s2", 0, s2);
        Assert.checkMaximumParameter("s2", 255, s2);
        Assert.checkMinimumParameter("s3", 0, s3);
        Assert.checkMaximumParameter("s3", 255, s3);
        Assert.checkMinimumParameter("s4", 0, s4);
        Assert.checkMaximumParameter("s4", 255, s4);
        byte[] bytes = new byte[4];
        bytes[0] = (byte) s1;
        bytes[1] = (byte) s2;
        bytes[2] = (byte) s3;
        bytes[3] = (byte) s4;
        // pre-compute the digits required
        int digitsForS1 = s1 < 10 ? 1 : s1 < 100 ? 2 : 3;
        int digitsForS2 = s2 < 10 ? 1 : s2 < 100 ? 2 : 3;
        int digitsForS3 = s3 < 10 ? 1 : s3 < 100 ? 2 : 3;
        int digitsForS4 = s4 < 10 ? 1 : s4 < 100 ? 2 : 3;
        byte[] hostBytes = new byte[3 + digitsForS1 + digitsForS2 + digitsForS3 + digitsForS4];
        // use encodeUnsignedByte to encode s1,s2,s3,s4 into hostBytes
        encodeUnsignedByte(s1, hostBytes, 0, digitsForS1);
        hostBytes[digitsForS1] = '.';
        encodeUnsignedByte(s2, hostBytes, digitsForS1 + 1, digitsForS2);
        hostBytes[digitsForS1 + digitsForS2 + 1] = '.';
        encodeUnsignedByte(s3, hostBytes, digitsForS1 + digitsForS2 + 2, digitsForS3);
        hostBytes[digitsForS1 + digitsForS2 + digitsForS3 + 2] = '.';
        encodeUnsignedByte(s4, hostBytes, digitsForS1 + digitsForS2 + digitsForS3 + 3, digitsForS4);
        String hostName = new String(hostBytes, 0);
        try {
            return (Inet4Address) InetAddress.getByAddress(hostName, bytes);
        } catch (UnknownHostException e) {
            // not possible
            throw new IllegalStateException(e);
        }
    }

    private static void encodeUnsignedByte(int value, byte[] bytes, int offset, int digits) {
        assert value >= 0 && value <= 255 && digits >= 1 && digits <= 3;
        if (digits == 3) {
            bytes[offset + 2] = (byte) ('0' + (value % 10));
            value /= 10;
        }
        if (digits == 2) {
            bytes[offset + 1] = (byte) ('0' + (value % 10));
            value /= 10;
        }
        bytes[offset] = (byte) ('0' + (value % 10));
    }

    /**
     * Get an IPv6 address from eight integer segments. Each segment must be between 0 and 65535 ({@code 0xffff}).
     *
     * @param s1 the first segment
     * @param s2 the second segment
     * @param s3 the third segment
     * @param s4 the fourth segment
     * @param s5 the fifth segment
     * @param s6 the sixth segment
     * @param s7 the seventh segment
     * @param s8 the eighth segment
     * @return the address (not {@code null})
     */
    public static Inet6Address getInet6Address(int s1, int s2, int s3, int s4, int s5, int s6, int s7, int s8) {
        byte[] bytes = new byte[16];
        Assert.checkMinimumParameter("s1", 0, s1);
        Assert.checkMaximumParameter("s1", 0xffff, s1);
        Assert.checkMinimumParameter("s2", 0, s2);
        Assert.checkMaximumParameter("s2", 0xffff, s2);
        Assert.checkMinimumParameter("s3", 0, s3);
        Assert.checkMaximumParameter("s3", 0xffff, s3);
        Assert.checkMinimumParameter("s4", 0, s4);
        Assert.checkMaximumParameter("s4", 0xffff, s4);
        Assert.checkMinimumParameter("s5", 0, s5);
        Assert.checkMaximumParameter("s5", 0xffff, s5);
        Assert.checkMinimumParameter("s6", 0, s6);
        Assert.checkMaximumParameter("s6", 0xffff, s6);
        Assert.checkMinimumParameter("s7", 0, s7);
        Assert.checkMaximumParameter("s7", 0xffff, s7);
        Assert.checkMinimumParameter("s8", 0, s8);
        Assert.checkMaximumParameter("s8", 0xffff, s8);
        bytes[0] = (byte) (s1 >> 8);
        bytes[1] = (byte) s1;
        bytes[2] = (byte) (s2 >> 8);
        bytes[3] = (byte) s2;
        bytes[4] = (byte) (s3 >> 8);
        bytes[5] = (byte) s3;
        bytes[6] = (byte) (s4 >> 8);
        bytes[7] = (byte) s4;
        bytes[8] = (byte) (s5 >> 8);
        bytes[9] = (byte) s5;
        bytes[10] = (byte) (s6 >> 8);
        bytes[11] = (byte) s6;
        bytes[12] = (byte) (s7 >> 8);
        bytes[13] = (byte) s7;
        bytes[14] = (byte) (s8 >> 8);
        bytes[15] = (byte) s8;
        try {
            return Inet6Address.getByAddress(toOptimalStringV6(bytes), bytes, 0);
        } catch (UnknownHostException e) {
            // not possible
            throw new IllegalStateException(e);
        }
    }

    /**
     * Checks whether given String is a valid IPv6 address.
     *
     * @param address address textual representation
     * @return {@code true} if {@code address} is a valid IPv6 address, {@code false} otherwise
     */
    public static boolean isInet6Address(String address) {
        return parseInet6AddressToBytes(address) != null;
    }

    /**
     * Parse an IPv6 address into an {@code Inet6Address} object.
     *
     * @param address the address to parse
     * @return the parsed address, or {@code null} if the address is not valid
     */
    public static Inet6Address parseInet6Address(String address) {
        return parseInet6Address(address, null);
    }

    /**
     * Parse an IPv6 address into an {@code Inet6Address} object.
     *
     * @param address the address to parse (must not be {@code null})
     * @param hostName the host name to use in the resultant object, or {@code null} to use the string representation of
     *        the address
     * @return the parsed address, or {@code null} if the address is not valid
     */
    public static Inet6Address parseInet6Address(String address, String hostName) {
        final byte[] bytes = parseInet6AddressToBytes(address);
        if (bytes == null) {
            return null;
        }
        int scopeId = 0;
        Inet6Address inetAddress;
        try {
            inetAddress = Inet6Address.getByAddress(hostName == null ? toOptimalStringV6(bytes) : hostName, bytes, 0);
        } catch (UnknownHostException e) {
            // not possible
            throw new IllegalStateException(e);
        }
        final int pctIdx = address.indexOf('%');
        if (pctIdx != -1) {
            scopeId = getScopeId(address.substring(pctIdx + 1), inetAddress);
            if (scopeId == 0) {
                // address not valid after all...
                return null;
            }
            try {
                inetAddress = Inet6Address.getByAddress(hostName == null ? toOptimalStringV6(bytes) : hostName, bytes, scopeId);
            } catch (UnknownHostException e) {
                // not possible
                throw new IllegalStateException(e);
            }
        }
        return inetAddress;
    }

    /**
     * Parse an IPv6 address into an {@code Inet6Address} object, throwing an exception on failure.
     *
     * @param address the address to parse
     * @return the parsed address (not {@code null})
     * @throws IllegalArgumentException if the address is not valid
     */
    public static Inet6Address parseInet6AddressOrFail(String address) {
        final Inet6Address result = parseInet6Address(address, null);
        if (result == null)
            throw Messages.msg.invalidAddress(address);
        return result;
    }

    /**
     * Parse an IPv6 address into an {@code Inet6Address} object.
     *
     * @param address the address to parse (must not be {@code null})
     * @param hostName the host name to use in the resultant object, or {@code null} to use the string representation of
     *        the address
     * @return the parsed address (not {@code null})
     * @throws IllegalArgumentException if the address is not valid
     */
    public static Inet6Address parseInet6AddressOrFail(String address, String hostName) {
        final Inet6Address result = parseInet6Address(address, hostName);
        if (result == null)
            throw Messages.msg.invalidAddress(address);
        return result;
    }

    /**
     * Checks whether given String is a valid IPv4 address.
     *
     * @param address address textual representation
     * @return {@code true} if {@code address} is a valid IPv4 address, {@code false} otherwise
     */
    public static boolean isInet4Address(String address) {
        return parseInet4AddressToBytes(address) != null;
    }

    /**
     * Parse an IPv4 address into an {@code Inet4Address} object.
     *
     * @param address the address to parse
     * @return the parsed address, or {@code null} if the address is not valid
     */
    public static Inet4Address parseInet4Address(String address) {
        return parseInet4Address(address, null);
    }

    /**
     * Parse an IPv4 address into an {@code Inet4Address} object.
     *
     * @param address the address to parse
     * @param hostName the host name to use in the resultant object, or {@code null} to use the string representation of
     *        the address
     * @return the parsed address, or {@code null} if the address is not valid
     */
    public static Inet4Address parseInet4Address(String address, String hostName) {
        final byte[] bytes = parseInet4AddressToBytes(address);
        if (bytes == null) {
            return null;
        }
        try {
            return (Inet4Address) Inet4Address.getByAddress(hostName == null ? toOptimalString(bytes) : hostName, bytes);
        } catch (UnknownHostException e) {
            // not possible
            throw new IllegalStateException(e);
        }
    }

    /**
     * Parse an IPv4 address into an {@code Inet4Address} object, throwing an exception on failure.
     *
     * @param address the address to parse
     * @return the parsed address (not {@code null})
     * @throws IllegalArgumentException if the address is not valid
     */
    public static Inet4Address parseInet4AddressOrFail(String address) {
        final Inet4Address result = parseInet4Address(address, null);
        if (result == null)
            throw Messages.msg.invalidAddress(address);
        return result;
    }

    /**
     * Parse an IPv4 address into an {@code Inet4Address} object.
     *
     * @param address the address to parse (must not be {@code null})
     * @param hostName the host name to use in the resultant object, or {@code null} to use the string representation of
     *        the address
     * @return the parsed address (not {@code null})
     * @throws IllegalArgumentException if the address is not valid
     */
    public static Inet4Address parseInet4AddressOrFail(String address, String hostName) {
        final Inet4Address result = parseInet4Address(address, hostName);
        if (result == null)
            throw Messages.msg.invalidAddress(address);
        return result;
    }

    /**
     * Parse an IP address into an {@code InetAddress} object.
     *
     * @param address the address to parse
     * @return the parsed address, or {@code null} if the address is not valid
     */
    public static InetAddress parseInetAddress(String address) {
        return parseInetAddress(address, null);
    }

    /**
     * Parse an IP address into an {@code InetAddress} object.
     *
     * @param address the address to parse
     * @param hostName the host name to use in the resultant object, or {@code null} to use the string representation of
     *        the address
     * @return the parsed address, or {@code null} if the address is not valid
     */
    public static InetAddress parseInetAddress(String address, String hostName) {
        // simple heuristic
        if (address.indexOf(':') != -1) {
            return parseInet6Address(address, hostName);
        } else {
            return parseInet4Address(address, hostName);
        }
    }

    /**
     * Parse an IP address into an {@code InetAddress} object, throwing an exception on failure.
     *
     * @param address the address to parse
     * @return the parsed address (not {@code null})
     * @throws IllegalArgumentException if the address is not valid
     */
    public static InetAddress parseInetAddressOrFail(String address) {
        final InetAddress result = parseInetAddress(address, null);
        if (result == null)
            throw Messages.msg.invalidAddress(address);
        return result;
    }

    /**
     * Parse an IP address into an {@code InetAddress} object.
     *
     * @param address the address to parse (must not be {@code null})
     * @param hostName the host name to use in the resultant object, or {@code null} to use the string representation of
     *        the address
     * @return the parsed address (not {@code null})
     * @throws IllegalArgumentException if the address is not valid
     */
    public static InetAddress parseInetAddressOrFail(String address, String hostName) {
        final InetAddress result = parseInetAddress(address, hostName);
        if (result == null)
            throw Messages.msg.invalidAddress(address);
        return result;
    }

    /**
     * Parse a CIDR address into a {@code CidrAddress} object.
     *
     * @param address the address to parse
     * @return the parsed address, or {@code null} if the address is not valid
     */
    public static CidrAddress parseCidrAddress(String address) {
        final int idx = address.indexOf('/');
        if (idx == -1) {
            return null;
        }
        int mask;
        try {
            mask = Integer.parseInt(address.substring(idx + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        final byte[] addressBytes = parseInetAddressToBytes(address.substring(0, idx));
        if (addressBytes == null) {
            return null;
        }
        try {
            return CidrAddress.create(addressBytes, mask, false);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Converts IPv6 address from textual representation to bytes.
     * <p>
     * If given string doesn't represent valid IPv6 address, the method returns {@code null}.
     *
     * @param address address textual representation
     * @return byte array representing the address, or {@code null} if the address is not valid
     */
    public static byte[] parseInet6AddressToBytes(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }

        // remove brackets if present
        if (address.startsWith("[") && address.endsWith("]")) {
            address = address.substring(1, address.length() - 1);
        }

        final int pctIdx = address.indexOf('%');
        if (pctIdx != -1) {
            address = address.substring(0, pctIdx);
        }

        String[] segments = address.split(":", 10);

        // there can be minimum of 2 and maximum of 8 colons, which makes 3 respectively 9 segments
        if (segments.length > 9 || segments.length < 3) {
            return null;
        }
        // if the first segment is empty, the second one must be too - "::<address end>"
        if (segments[0].length() == 0 && segments[1].length() != 0) {
            return null;
        }
        // if the last segment is empty, the segment before it must be too - "<address beginning>::"
        if (segments[segments.length - 1].length() == 0 && segments[segments.length - 2].length() != 0) {
            return null;
        }

        // validate segments
        for (int i = 0; i < segments.length; i++) {
            for (int charIdx = 0; charIdx < segments[i].length(); charIdx++) {
                char c = segments[i].charAt(charIdx);
                if (c == '.' && i != segments.length - 1) {
                    return null; // "." is allowed in the last segment only
                } else if (c != '.' && c != ':' && Character.digit(c, 16) == -1) {
                    return null; // not ".", ":" or a digit
                }
            }
        }

        // look for an empty segment - "::"
        int emptyIndex = -1;
        for (int i = 0; i < segments.length - 1; i++) {
            if (segments[i].length() == 0) {
                if (emptyIndex > 0) {
                    return null; // more than one occurrence of "::", invalid address
                } else if (emptyIndex != 0) { // don't rewrite skipIndex=0, when address starts with "::"
                    emptyIndex = i;
                }
            }
        }

        boolean containsIPv4 = segments[segments.length - 1].contains(".");
        int totalSegments = containsIPv4 ? 7 : 8; // if the last segment contains IPv4 notation ("::ffff:192.0.0.1"), the address only has 7 segments
        if (emptyIndex == -1 && segments.length != totalSegments) {
            return null; // no substitution but incorrect number of segments
        }

        int skipIndex;
        int skippedSegments;
        boolean isDefaultRoute = segments.length == 3
                && segments[0].isEmpty() && segments[1].isEmpty() && segments[2].isEmpty(); // is address just "::"?
        if (isDefaultRoute) {
            skipIndex = 0;
            skippedSegments = 8;
        } else if (segments[0].isEmpty() || segments[segments.length - 1].isEmpty()) {
            // "::" is at the beginning or end of the address
            skipIndex = emptyIndex;
            skippedSegments = totalSegments - segments.length + 2;
        } else if (emptyIndex > -1) {
            // "::" somewhere in the middle
            skipIndex = emptyIndex;
            skippedSegments = totalSegments - segments.length + 1;
        } else {
            // no substitution
            skipIndex = 0;
            skippedSegments = 0;
        }

        ByteBuffer bytes = ByteBuffer.allocate(16);

        try {
            // convert segments before "::"
            for (int i = 0; i < skipIndex; i++) {
                bytes.putShort(parseHexadecimal(segments[i]));
            }
            // fill "0" characters into expanded segments
            for (int i = skipIndex; i < skipIndex + skippedSegments; i++) {
                bytes.putShort((short) 0);
            }
            // convert segments after "::"
            for (int i = skipIndex + skippedSegments; i < totalSegments; i++) {
                int segmentIdx = segments.length - (totalSegments - i);
                if (containsIPv4 && i == totalSegments - 1) {
                    // we are at the last segment and it contains IPv4 address
                    String[] ipV4Segments = segments[segmentIdx].split("\\.");
                    if (ipV4Segments.length != 4) {
                        return null; // incorrect number of segments in IPv4
                    }
                    for (int idxV4 = 0; idxV4 < 4; idxV4++) {
                        bytes.put(parseDecimal(ipV4Segments[idxV4]));
                    }
                } else {
                    bytes.putShort(parseHexadecimal(segments[segmentIdx]));
                }
            }

            return bytes.array();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Converts IPv4 address from textual representation to bytes.
     * <p>
     * If given string doesn't represent valid IPv4 address, the method returns {@code null}.
     * <p>
     * This only supports decimal notation.
     *
     * @param address address textual representation
     * @return byte array representing the address, or {@code null} if the address is not valid
     */
    public static byte[] parseInet4AddressToBytes(String address) {
        String[] segments = address.split("\\.", 5);
        if (segments.length != 4) {
            return null; // require 4 segments
        }
        // validate segments
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].length() < 1) {
                return null; // empty segment
            }
            for (int cidx = 0; cidx < segments[i].length(); cidx++) {
                if (Character.digit(segments[i].charAt(cidx), 10) < 0) {
                    return null; // not a digit
                }
            }
        }

        byte[] bytes = new byte[4];
        try {
            for (int i = 0; i < segments.length; i++) {
                bytes[i] = parseDecimal(segments[i]);
            }
            return bytes;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Converts an IP address from textual representation to bytes.
     * <p>
     * If given string doesn't represent valid IP address, the method returns {@code null}.
     *
     * @param address address textual representation
     * @return byte array representing the address, or {@code null} if the address is not valid
     */
    public static byte[] parseInetAddressToBytes(String address) {
        // simple heuristic
        if (address.indexOf(':') != -1) {
            return parseInet6AddressToBytes(address);
        } else {
            return parseInet4AddressToBytes(address);
        }
    }

    /**
     * Get the scope ID of the given address (if it is an IPv6 address).
     *
     * @return the scope ID, or 0 if there is none or the address is an IPv4 address
     */
    public static int getScopeId(InetAddress address) {
        return address instanceof Inet6Address ? ((Inet6Address) address).getScopeId() : 0;
    }

    private static final Pattern NUMERIC = Pattern.compile("\\d+");

    /**
     * Attempt to get the scope ID of the given string. If the string is numeric then the number is parsed
     * and returned as-is. If the scope is a string, then a search for the matching network interface will occur.
     *
     * @param scopeName the scope number or name as a string (must not be {@code null})
     * @return the scope ID, or 0 if no matching scope could be found
     */
    public static int getScopeId(String scopeName) {
        return getScopeId(scopeName, null);
    }

    /**
     * Attempt to get the scope ID of the given string. If the string is numeric then the number is parsed
     * and returned as-is. If the scope is a string, then a search for the matching network interface will occur.
     *
     * @param scopeName the scope number or name as a string (must not be {@code null})
     * @param compareWith the address to compare with, to ensure that the wrong local scope is not selected (may be
     *        {@code null})
     * @return the scope ID, or 0 if no matching scope could be found
     */
    public static int getScopeId(String scopeName, InetAddress compareWith) {
        Assert.checkNotNullParam("scopeName", scopeName);
        if (NUMERIC.matcher(scopeName).matches())
            try {
                return Integer.parseInt(scopeName);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        final NetworkInterface ni = findInterfaceWithScopeId(scopeName);
        if (ni == null)
            return 0;
        return getScopeId(ni, compareWith);
    }

    public static NetworkInterface findInterfaceWithScopeId(String scopeName) {
        final Enumeration<NetworkInterface> enumeration;
        try {
            enumeration = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ignored) {
            return null;
        }
        while (enumeration.hasMoreElements()) {
            final NetworkInterface net = enumeration.nextElement();
            if (net.getName().equals(scopeName)) {
                return net;
            }
        }
        return null;
    }

    public static int getScopeId(NetworkInterface networkInterface) {
        return getScopeId(networkInterface, null);
    }

    public static int getScopeId(NetworkInterface networkInterface, InetAddress compareWith) {
        Assert.checkNotNullParam("networkInterface", networkInterface);
        Inet6Address cw6 = compareWith instanceof Inet6Address ? (Inet6Address) compareWith : null;
        Inet6Address address = doPrivileged((PrivilegedAction<Inet6Address>) () -> {
            final Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                final InetAddress a = addresses.nextElement();
                if (a instanceof Inet6Address) {
                    final Inet6Address a6 = (Inet6Address) a;
                    if (cw6 == null ||
                            a6.isLinkLocalAddress() == cw6.isLinkLocalAddress() &&
                                    a6.isSiteLocalAddress() == cw6.isSiteLocalAddress()) {
                        return a6;
                    }
                }
            }
            return null;
        });
        return address == null ? 0 : address.getScopeId();
    }

    /**
     * Extract a base URI from the given scheme and socket address. The address is not resolved.
     *
     * @param scheme the URI scheme
     * @param socketAddress the host socket address
     * @param defaultPort the protocol default port, or -1 if there is none
     * @return the URI instance
     * @throws URISyntaxException if the URI failed to be constructed for some reason
     */
    public static URI getURIFromAddress(String scheme, InetSocketAddress socketAddress, int defaultPort)
            throws URISyntaxException {
        String host = getHostNameIfResolved(socketAddress);
        if (isInet6Address(host)) {
            host = '[' + host + ']';
        }
        final int port = socketAddress.getPort();
        return new URI(scheme, null, host, port == defaultPort ? -1 : port, null, null, null);
    }

    /**
     * Get the protocol family of the given Internet address.
     *
     * @param inetAddress the address (must not be {@code null})
     * @return the protocol family (not {@code null})
     */
    public static ProtocolFamily getProtocolFamily(InetAddress inetAddress) {
        Assert.checkNotNullParam("inetAddress", inetAddress);
        return inetAddress instanceof Inet6Address ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
    }

    private static byte parseDecimal(String number) {
        int i = Integer.parseInt(number);
        if (i < 0 || i > 255) {
            throw new NumberFormatException();
        }
        return (byte) i;
    }

    private static short parseHexadecimal(String hexNumber) {
        int i = Integer.parseInt(hexNumber, 16);
        if (i > 0xffff) {
            throw new NumberFormatException();
        }
        return (short) i;
    }

    private static String toOptimalStringV6(final byte[] bytes) {
        final int[] segments = new int[8];
        for (int i = 0; i < 8; i++) {
            segments[i] = (bytes[i << 1] & 0xff) << 8 | bytes[(i << 1) + 1] & 0xff;
        }
        // now loop through the segments and add them as optimally as possible
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (segments[i] == 0) {
                if (i == 7) {
                    b.append('0');
                } else {
                    // possible to collapse it
                    i++;
                    if (segments[i] == 0) {
                        // yup
                        b.append(':').append(':');
                        for (i++; i < 8; i++) {
                            if (segments[i] == 0xffff && b.length() == 2) {
                                b.append("ffff");
                                if (i == 5) {
                                    // it's an IPv4 compat address.
                                    b.append(':').append(bytes[12] & 0xff).append('.').append(bytes[13] & 0xff).append('.')
                                            .append(bytes[14] & 0xff).append('.').append(bytes[15] & 0xff);
                                    i = 8;
                                } else if (i == 4 && segments[5] == 0) {
                                    // it's a SIIT address.
                                    b.append(":0:").append(bytes[12] & 0xff).append('.').append(bytes[13] & 0xff).append('.')
                                            .append(bytes[14] & 0xff).append('.').append(bytes[15] & 0xff);
                                    i = 8;
                                } else {
                                    // finally break and do the rest normally
                                    for (i++; i < 8; i++) {
                                        b.append(':').append(Integer.toHexString(segments[i]));
                                    }
                                }
                            } else if (segments[i] != 0) {
                                // finally break and do the rest normally
                                b.append(Integer.toHexString(segments[i]));
                                for (i++; i < 8; i++) {
                                    b.append(':').append(Integer.toHexString(segments[i]));
                                }
                            }
                        }
                    } else {
                        // no, just a single 0 in isolation doesn't get collapsed
                        if (i > 1)
                            b.append(':');
                        b.append('0').append(':').append(Integer.toHexString(segments[i]));
                    }
                }
            } else {
                if (i > 0)
                    b.append(':');
                b.append(Integer.toHexString(segments[i]));
            }
        }
        return b.toString();
    }
}
