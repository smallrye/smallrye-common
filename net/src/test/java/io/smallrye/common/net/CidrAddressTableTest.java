package io.smallrye.common.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CidrAddressTableTest {
    @Test
    public void testEmpty() {
        CidrAddressTable<Void> table = new CidrAddressTable<>();
        assertTrue(table.isEmpty());
    }

    @Test
    public void testSingle4() {
        CidrAddressTable<String> table = new CidrAddressTable<>();
        table.put(CidrAddress.create(Inet.parseInet4Address("10.4.5.9"), 16), "meow");
        assertEquals("meow", table.get(Inet.parseInet4Address("10.4.5.9")));
        assertEquals("woof", table.getOrDefault(Inet.parseInet4Address("11.4.5.9"), "woof"));
    }

    @Test
    public void testDouble4() {
        CidrAddressTable<String> table = new CidrAddressTable<>();
        table.put(CidrAddress.create(Inet.parseInet4Address("10.4.5.9"), 16), "meow");
        table.put(CidrAddress.create(Inet.parseInet4Address("11.4.5.9"), 16), "nyan");
        assertEquals("meow", table.get(Inet.parseInet4Address("10.4.5.9")));
        assertEquals("nyan", table.get(Inet.parseInet4Address("11.4.5.9")));
        assertEquals("woof", table.getOrDefault(Inet.parseInet4Address("12.4.5.9"), "woof"));
    }

    @Test
    public void testBig4() {
        CidrAddressTable<String> table = new CidrAddressTable<>();
        table.put(CidrAddress.create(Inet.parseInet4Address("0.0.0.0"), 0), "the big block");
        table.put(CidrAddress.create(Inet.parseInet4Address("8.8.8.8"), 32), "one DNS server");
        table.put(CidrAddress.create(Inet.parseInet4Address("12.4.0.0"), 16), "twelve-four");
        table.put(CidrAddress.create(Inet.parseInet4Address("127.0.0.0"), 8), "local stuff");
        table.put(CidrAddress.create(Inet.parseInet4Address("10.0.0.0"), 8), "big private");
        table.put(CidrAddress.create(Inet.parseInet4Address("10.0.0.0"), 12), "big private sub-block (1)");
        table.put(CidrAddress.create(Inet.parseInet4Address("10.16.0.0"), 12), "big private sub-block (2)");
        table.put(CidrAddress.create(Inet.parseInet4Address("192.168.1.0"), 24), "little private");
        assertEquals("big private sub-block (1)", table.get(Inet.parseInet4Address("10.4.5.9")));
        assertEquals("big private", table.get(Inet.parseInet4Address("10.33.5.9")));
        assertEquals("the big block", table.get(Inet.parseInet4Address("11.4.5.9")));
        assertEquals("the big block", table.get(Inet.parseInet4Address("8.8.8.9")));
        assertEquals("the big block", table.get(Inet.parseInet4Address("8.8.8.7")));
        assertEquals("one DNS server", table.get(Inet.parseInet4Address("8.8.8.8")));
        assertEquals("little private", table.get(Inet.parseInet4Address("192.168.1.34")));
        assertEquals("local stuff", table.get(Inet.parseInet4Address("127.0.0.1")));
        assertEquals(8, table.size());
    }

    @Test
    public void testVaryLengths4() {
        CidrAddressTable<String> table = new CidrAddressTable<>();
        table.put(CidrAddress.create(Inet.parseInet4Address("10.4.5.9"), 16), "meow");
        table.put(CidrAddress.create(Inet.parseInet4Address("10.4.5.9"), 17), "nyan");
        assertEquals("meow", table.get(Inet.parseInet4Address("10.4.255.9")));
        assertEquals("nyan", table.get(Inet.parseInet4Address("10.4.5.9")));
        assertEquals("woof", table.getOrDefault(Inet.parseInet4Address("12.4.5.9"), "woof"));
    }

    @Test
    public void testRemove4() {
        CidrAddressTable<String> table = new CidrAddressTable<>();
        table.put(CidrAddress.create(Inet.parseInet4Address("10.4.5.9"), 16), "meow");
        table.put(CidrAddress.create(Inet.parseInet4Address("11.4.5.9"), 16), "nyan");
        assertEquals("meow", table.removeExact(CidrAddress.create(Inet.parseInet4Address("10.4.0.0"), 16)));
        assertEquals(1, table.size());
        assertNull(table.removeExact(CidrAddress.create(Inet.parseInet4Address("10.4.0.0"), 16)));
        assertNull(table.removeExact(CidrAddress.create(Inet.parseInet4Address("11.5.4.0"), 16)));
        assertFalse(table.removeExact(CidrAddress.create(Inet.parseInet4Address("11.4.0.0"), 16), "meow"));
        assertEquals(1, table.size());
        assertTrue(table.removeExact(CidrAddress.create(Inet.parseInet4Address("11.4.0.0"), 16), "nyan"));
        assertTrue(table.isEmpty());
    }

    @Test
    public void testReplace4() {
        CidrAddressTable<String> table = new CidrAddressTable<>();
        table.put(CidrAddress.create(Inet.parseInet4Address("10.4.5.9"), 16), "meow");
        table.put(CidrAddress.create(Inet.parseInet4Address("11.4.5.9"), 16), "nyan");
        assertEquals("meow", table.replaceExact(CidrAddress.create(Inet.parseInet4Address("10.4.0.0"), 16), "purr"));
        assertEquals("purr", table.get(Inet.parseInet4Address("10.4.99.255")));
        assertEquals(2, table.size());
        assertFalse(table.replaceExact(CidrAddress.create(Inet.parseInet4Address("11.4.0.0"), 16), "meow", "purr"));
        assertTrue(table.replaceExact(CidrAddress.create(Inet.parseInet4Address("11.4.0.0"), 16), "nyan", "hiss"));
        assertEquals("hiss", table.get(Inet.parseInet4Address("11.4.100.200")));
    }

    @Test
    public void testSingle6() {
        CidrAddressTable<String> table = new CidrAddressTable<>();
        table.put(CidrAddress.create(Inet.parseInet6Address("1000::"), 16), "meow");
        assertEquals("meow", table.get(Inet.parseInet6Address("1000:2000:3000::")));
        assertEquals("woof", table.getOrDefault(Inet.parseInet6Address("1001:2000:3000::"), "woof"));
    }

    @Test
    public void testDouble6() {
        CidrAddressTable<String> table = new CidrAddressTable<>();
        table.put(CidrAddress.create(Inet.parseInet6Address("1000::4.5.9.4"), 16), "meow");
        table.put(CidrAddress.create(Inet.parseInet6Address("1100::3.4.5.9"), 16), "nyan");
        assertEquals("meow", table.get(Inet.parseInet6Address("1000::ffff:44.55.69.99")));
        assertEquals("nyan", table.get(Inet.parseInet6Address("1100:1::1:1100")));
        assertEquals("woof", table.getOrDefault(Inet.parseInet6Address("1111:1::4ae4"), "woof"));
    }

    @Test
    public void testBig6() {
        CidrAddressTable<String> table = new CidrAddressTable<>();
        table.put(CidrAddress.create(Inet.parseInet6Address("::"), 0), "the big block");
        table.put(CidrAddress.create(Inet.parseInet6Address("::ffff:8.8.8.8"), 128), "one DNS server");
        table.put(CidrAddress.create(Inet.parseInet6Address("12:4::"), 32), "twelve-four");
        table.put(CidrAddress.create(Inet.parseInet6Address("ff01::"), 32), "local stuff");
        table.put(CidrAddress.create(Inet.parseInet6Address("1010:1010:1010::"), 48), "big private sub-block (2)");
        table.put(CidrAddress.create(Inet.parseInet6Address("1010::"), 16), "big private");
        table.put(CidrAddress.create(Inet.parseInet6Address("1010:1010::"), 32), "big private sub-block (1)");
        table.put(CidrAddress.create(Inet.parseInet6Address("4592:f092:33f2:6655::"), 64), "little private");
        assertEquals("big private sub-block (1)", table.get(Inet.parseInet6Address("1010:1010:2993:9938:ff0f::")));
        assertEquals("big private", table.get(Inet.parseInet6Address("1010:1011:2993:9938:ff0f::")));
        assertEquals("the big block", table.get(Inet.parseInet6Address("9999::")));
        assertEquals("the big block", table.get(Inet.parseInet6Address("ff02:20ff::")));
        assertEquals("the big block", table.get(Inet.parseInet6Address("::ffff:8.8.8.7")));
        assertEquals("one DNS server", table.get(Inet.parseInet6Address("::ffff:8.8.8.8")));
        assertEquals("little private", table.get(Inet.parseInet6Address("4592:f092:33f2:6655:1234:4929:2929:1110")));
        assertEquals("local stuff", table.get(Inet.parseInet6Address("ff01::1")));
        assertEquals(8, table.size());
    }

    @Test
    public void testVaryLengths6() {
        CidrAddressTable<String> table = new CidrAddressTable<>();
        table.put(CidrAddress.create(Inet.parseInet6Address("fe3f:ff44::"), 32), "meow");
        table.put(CidrAddress.create(Inet.parseInet6Address("fe3f:ff44::"), 33), "nyan");
        assertEquals("meow", table.get(Inet.parseInet6Address("fe3f:ff44:8000::")));
        assertEquals("nyan", table.get(Inet.parseInet6Address("fe3f:ff44:0000::")));
        assertEquals("woof", table.getOrDefault(Inet.parseInet6Address("fe3f:ff45::"), "woof"));
    }

    @Test
    public void testRemove6() {
        CidrAddressTable<String> table = new CidrAddressTable<>();
        table.put(CidrAddress.create(Inet.parseInet6Address("ee55:7711:6543:ab3d::"), 32), "meow");
        table.put(CidrAddress.create(Inet.parseInet6Address("ee56:7722:73ec:01ee::"), 32), "nyan");
        assertEquals("meow", table.removeExact(CidrAddress.create(Inet.parseInet6Address("ee55:7711::"), 32)));
        assertEquals(1, table.size());
        assertNull(table.removeExact(CidrAddress.create(Inet.parseInet6Address("ee55:7711::"), 32)));
        assertNull(table.removeExact(CidrAddress.create(Inet.parseInet6Address("ee56::"), 16)));
        assertFalse(table.removeExact(CidrAddress.create(Inet.parseInet6Address("ee55:7711::"), 32), "meow"));
        assertEquals(1, table.size());
        assertTrue(table.removeExact(CidrAddress.create(Inet.parseInet6Address("ee56:7722::"), 32), "nyan"));
        assertTrue(table.isEmpty());
    }

    @Test
    public void testReplace6() {
        CidrAddressTable<String> table = new CidrAddressTable<>();
        table.put(CidrAddress.create(Inet.parseInet6Address("5e:4ffe::"), 32), "meow");
        table.put(CidrAddress.create(Inet.parseInet6Address("5e:4fff::"), 32), "nyan");
        assertEquals("meow", table.replaceExact(CidrAddress.create(Inet.parseInet6Address("5e:4ffe:f55f::"), 32), "purr"));
        assertEquals("purr", table.get(Inet.parseInet6Address("5e:4ffe:6011:222::44ee")));
        assertEquals(2, table.size());
        assertFalse(table.replaceExact(CidrAddress.create(Inet.parseInet6Address("5e:4ffe::"), 32), "meow", "purr"));
        assertTrue(table.replaceExact(CidrAddress.create(Inet.parseInet6Address("5e:4fff::"), 32), "nyan", "hiss"));
        assertEquals("hiss", table.get(Inet.parseInet6Address("5e:4fff:6:a::5:e")));
    }
}
