package io.smallrye.common.net;

import java.net.UnknownHostException;
import java.security.PrivilegedAction;
import java.util.regex.Pattern;

final class GetHostInfoAction implements PrivilegedAction<String[]> {
    GetHostInfoAction() {
    }

    public String[] run() {
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
