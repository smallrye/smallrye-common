package io.smallrye.common.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import io.smallrye.common.constraint.Assert;

/**
 * Methods for getting the system host name. The host name is detected from the environment, but may be overridden by
 * use of the {@code jboss.host.name} and/or {@code jboss.qualified.host.name} system properties.
 */
@SuppressWarnings("removal")
public final class HostName {

    private static final Object lock = new Object();
    private static volatile String hostName;
    private static volatile String qualifiedHostName;
    private static volatile String nodeName;

    static {
        String[] names = resolveHosts();
        hostName = names[0];
        qualifiedHostName = names[1];
        nodeName = names[2];
    }

    private HostName() {
    }

    static InetAddress getLocalHost() throws UnknownHostException {
        InetAddress addr;
        try {
            addr = InetAddress.getLocalHost();
        } catch (ArrayIndexOutOfBoundsException e) { //this is workaround for mac osx bug see AS7-3223 and JGRP-1404
            addr = InetAddress.getByName(null);
        }
        return addr;
    }

    /**
     * Get the detected host name.
     *
     * @return the detected host name
     */
    public static String getHostName() {
        return hostName;
    }

    /**
     * Get the detected qualified host name.
     *
     * @return the detected qualified host name
     */
    public static String getQualifiedHostName() {
        return qualifiedHostName;
    }

    /**
     * Get the node name.
     *
     * @return the node name
     */
    public static String getNodeName() {
        return nodeName;
    }

    /**
     * Set the host name. The qualified host name is set directly from the given value; the unqualified host name
     * is then re-derived from that value. The node name is not changed by this method.
     *
     * @param qualifiedHostName the host name
     */
    public static void setQualifiedHostName(final String qualifiedHostName) {
        Assert.checkNotNullParam("qualifiedHostName", qualifiedHostName);
        synchronized (lock) {
            HostName.qualifiedHostName = qualifiedHostName;
            // Use the host part of the qualified host name
            final int idx = qualifiedHostName.indexOf('.');
            HostName.hostName = idx == -1 ? qualifiedHostName : qualifiedHostName.substring(0, idx);
        }
    }

    /**
     * Set the node name.
     *
     * @param nodeName the node name
     */
    public static void setNodeName(final String nodeName) {
        Assert.checkNotNullParam("nodeName", nodeName);
        HostName.nodeName = nodeName;
    }

    private static String[] resolveHosts() {
        // allow host name to be overridden
        String qualifiedHostName = System.getProperty("jboss.qualified.host.name");
        String providedHostName = System.getProperty("jboss.host.name");
        String providedNodeName = System.getProperty("jboss.node.name");
        if (qualifiedHostName == null) {
            // if host name is specified, don't pick a qualified host name that isn't related to it
            qualifiedHostName = providedHostName;
            if (qualifiedHostName == null) {
                // POSIX-like OSes including Mac should have this set
                qualifiedHostName = System.getenv("HOSTNAME");
            }
            if (qualifiedHostName == null) {
                // Certain versions of Windows
                qualifiedHostName = System.getenv("COMPUTERNAME");
            }
            if (qualifiedHostName == null) {
                try {
                    qualifiedHostName = HostName.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    qualifiedHostName = null;
                }
            }
            if (qualifiedHostName != null
                    && Pattern.compile("^\\d+\\.\\d+\\.\\d+\\.\\d+$|:").matcher(qualifiedHostName).find()) {
                // IP address is not acceptable
                qualifiedHostName = null;
            }
            if (qualifiedHostName == null) {
                // Give up
                qualifiedHostName = "unknown-host.unknown-domain";
            } else {
                qualifiedHostName = qualifiedHostName.trim().toLowerCase();
            }
        }
        if (providedHostName == null) {
            // Use the host part of the qualified host name
            final int idx = qualifiedHostName.indexOf('.');
            providedHostName = idx == -1 ? qualifiedHostName : qualifiedHostName.substring(0, idx);
        }
        if (providedNodeName == null) {
            providedNodeName = providedHostName;
        }
        return new String[] {
                providedHostName,
                qualifiedHostName,
                providedNodeName
        };
    }

}
