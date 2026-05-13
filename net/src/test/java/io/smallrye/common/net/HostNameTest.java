package io.smallrye.common.net;

import org.junit.jupiter.api.Test;

public final class HostNameTest {
    @Test
    public void testHostName() {
        System.out.println("Qualified name = " + HostName.getQualifiedHostName());
        System.out.println("Host name = " + HostName.getHostName());
        System.out.println("Node name = " + HostName.getNodeName());
    }
}
