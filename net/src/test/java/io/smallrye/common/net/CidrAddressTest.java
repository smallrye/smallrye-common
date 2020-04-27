package io.smallrye.common.net;

import static org.junit.Assert.*;

import org.junit.Test;

public class CidrAddressTest {

    @Test
    public void testBasic4() {
        CidrAddress cidrAddress = CidrAddress.create(Inet.getInet4Address(127, 0, 0, 1), 32);
        assertEquals("127.0.0.1/32", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(127, 0, 0, 1), 8);
        assertEquals("127.0.0.0/8", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 0);
        assertEquals("0.0.0.0/0", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 1);
        assertEquals("128.0.0.0/1", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 2);
        assertEquals("192.0.0.0/2", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 3);
        assertEquals("224.0.0.0/3", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 4);
        assertEquals("240.0.0.0/4", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 5);
        assertEquals("248.0.0.0/5", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 6);
        assertEquals("252.0.0.0/6", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 7);
        assertEquals("254.0.0.0/7", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 8);
        assertEquals("255.0.0.0/8", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 9);
        assertEquals("255.128.0.0/9", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 10);
        assertEquals("255.192.0.0/10", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 11);
        assertEquals("255.224.0.0/11", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 12);
        assertEquals("255.240.0.0/12", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 13);
        assertEquals("255.248.0.0/13", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 14);
        assertEquals("255.252.0.0/14", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 15);
        assertEquals("255.254.0.0/15", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 16);
        assertEquals("255.255.0.0/16", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 17);
        assertEquals("255.255.128.0/17", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 18);
        assertEquals("255.255.192.0/18", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 19);
        assertEquals("255.255.224.0/19", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 20);
        assertEquals("255.255.240.0/20", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 21);
        assertEquals("255.255.248.0/21", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 22);
        assertEquals("255.255.252.0/22", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 23);
        assertEquals("255.255.254.0/23", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 24);
        assertEquals("255.255.255.0/24", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 25);
        assertEquals("255.255.255.128/25", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 26);
        assertEquals("255.255.255.192/26", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 27);
        assertEquals("255.255.255.224/27", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 28);
        assertEquals("255.255.255.240/28", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 29);
        assertEquals("255.255.255.248/29", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 30);
        assertEquals("255.255.255.252/30", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 31);
        assertEquals("255.255.255.254/31", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.getInet4Address(255, 255, 255, 255), 32);
        assertEquals("255.255.255.255/32", cidrAddress.toString());
    }

    @Test
    public void testBasic6() {
        CidrAddress cidrAddress = CidrAddress.create(Inet.parseInet6Address("::1"), 128);
        assertEquals("::1/128", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.parseInet6Address("::1"), 0);
        assertEquals("::/0", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.parseInet6Address("ffff:ee55::5:34de:ffff"), 32);
        assertEquals("ffff:ee55::/32", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.parseInet6Address("ffff:ee55::5:34de:ffff"), 30);
        assertEquals("ffff:ee54::/30", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.parseInet6Address("ffff:ee55::5:34de:ffff"), 20);
        assertEquals("ffff:e000::/20", cidrAddress.toString());
    }

    @Test
    public void testBasic6Scoped() {
        CidrAddress cidrAddress = CidrAddress.create(Inet.parseInet6Address("::1%1"), 128);
        assertEquals("::1%1/128", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.parseInet6Address("::1%1"), 0);
        assertEquals("::%1/0", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.parseInet6Address("ffff:ee55::5:34de:ffff%1"), 32);
        assertEquals("ffff:ee55::%1/32", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.parseInet6Address("ffff:ee55::5:34de:ffff%1"), 30);
        assertEquals("ffff:ee54::%1/30", cidrAddress.toString());
        cidrAddress = CidrAddress.create(Inet.parseInet6Address("ffff:ee55::5:34de:ffff%1"), 20);
        assertEquals("ffff:e000::%1/20", cidrAddress.toString());
    }

    @Test
    public void testMatch4() {
        CidrAddress cidrAddress = CidrAddress.create(Inet.parseInet4Address("10.10.20.20"), 16);
        assertEquals("10.10.0.0/16", cidrAddress.toString());
        assertTrue(cidrAddress.matches(Inet.parseInet4Address("10.10.0.0")));
        assertTrue(cidrAddress.matches(Inet.parseInet4Address("10.10.10.10")));
        assertTrue(cidrAddress.matches(Inet.parseInet4Address("10.10.255.255")));
        assertFalse(cidrAddress.matches(Inet.parseInet4Address("10.11.255.255")));
        assertFalse(cidrAddress.matches(Inet.parseInet4Address("0.0.20.20")));
    }

    @Test
    public void testMatch6() {
        CidrAddress cidrAddress = CidrAddress.create(Inet.parseInet6Address("fed:f00d::3456:feed"), 32);
        assertEquals("fed:f00d::/32", cidrAddress.toString());
        assertTrue(cidrAddress.matches(Inet.parseInet6Address("fed:f00d:0f:beef::")));
        assertTrue(cidrAddress.matches(Inet.parseInet6Address("fed:f00d:0f:beef::%1")));
        assertTrue(cidrAddress.matches(Inet.parseInet6Address("fed:f00d:c0ff:ee::")));
        assertTrue(cidrAddress.matches(Inet.parseInet6Address("fed:f00d::")));
        assertFalse(cidrAddress.matches(Inet.parseInet6Address("fed:beef:0f:f00d::")));
        assertFalse(cidrAddress.matches(Inet.parseInet6Address("fed:beef:0f:f00d::%1")));
        assertFalse(cidrAddress.matches(Inet.parseInet6Address("fed::")));
        assertFalse(cidrAddress.matches(Inet.parseInet6Address("fed::%1")));
    }

    @Test
    public void testMatch6Scoped() {
        CidrAddress cidrAddress = CidrAddress.create(Inet.parseInet6Address("fed:f00d::3456:feed%1"), 32);
        assertEquals("fed:f00d::%1/32", cidrAddress.toString());
        assertTrue(cidrAddress.matches(Inet.parseInet6Address("fed:f00d:0f:beef::%1")));
        assertTrue(cidrAddress.matches(Inet.parseInet6Address("fed:f00d:c0ff:ee::%1")));
        assertTrue(cidrAddress.matches(Inet.parseInet6Address("fed:f00d::%1")));
        assertFalse(cidrAddress.matches(Inet.parseInet6Address("fed:f00d:0f:beef::%2")));
        assertFalse(cidrAddress.matches(Inet.parseInet6Address("fed:f00d:c0ff:ee::%2")));
        assertFalse(cidrAddress.matches(Inet.parseInet6Address("fed:f00d::%2")));
        assertFalse(cidrAddress.matches(Inet.parseInet6Address("fed:f00d:0f:beef::")));
        assertFalse(cidrAddress.matches(Inet.parseInet6Address("fed:f00d:c0ff:ee::")));
        assertFalse(cidrAddress.matches(Inet.parseInet6Address("fed:f00d::")));
        assertFalse(cidrAddress.matches(Inet.parseInet6Address("fed:beef:0f:f00d::")));
        assertFalse(cidrAddress.matches(Inet.parseInet6Address("fed:beef:0f:f00d::%1")));
        assertFalse(cidrAddress.matches(Inet.parseInet6Address("fed::")));
        assertFalse(cidrAddress.matches(Inet.parseInet6Address("fed::%1")));
    }
}
