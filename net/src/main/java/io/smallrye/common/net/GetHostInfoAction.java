package io.smallrye.common.net;

import static io.smallrye.ffm.AsType.*;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedAction;
import java.util.regex.Pattern;

import io.smallrye.common.os.OS;
import io.smallrye.ffm.As;
import io.smallrye.ffm.Critical;
import io.smallrye.ffm.In;
import io.smallrye.ffm.Link;
import io.smallrye.ffm.Out;

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
            if (qualifiedHostName == null && Runtime.version().feature() >= 22) {
                switch (OS.current()) {
                    case MAC, LINUX, AIX, Z -> {
                        byte[] bytes = new byte[512];
                        int res = gethostname(bytes, bytes.length);
                        if (res == 0) {
                            for (int i = 0; i < bytes.length; i++) {
                                if (bytes[i] == 0) {
                                    qualifiedHostName = new String(bytes, 0, i, StandardCharsets.UTF_8);
                                    break;
                                }
                            }
                        }
                    }
                    case WINDOWS -> {
                        char[] chars = new char[512];
                        int[] lenBuf = new int[1];
                        lenBuf[0] = chars.length;
                        if (GetComputerNameW(chars, lenBuf)) {
                            qualifiedHostName = new String(chars, 0, lenBuf[0]);
                        }
                    }
                }
            }
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

    // POSIX
    @Link
    @As(stdc_int)
    @Critical(heap = true)
    private static native int gethostname(@Out byte[] buffer, @As(size_t) int bufLen);

    // Windows
    @Link(name = "_GetComputerNameW")
    @Critical(heap = true)
    private static native boolean GetComputerNameW(@Out char[] buffer, @In @Out int[] lenPtr);
}
