package io.smallrye.common.net;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.UnsignedWord;
import org.graalvm.word.WordFactory;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

final class Substitutions {
    @TargetClass(className = "io.smallrye.common.net.GetHostInfoAction")
    static final class Target_io_smallrye_common_net_GetHostInfoAction {
        @Substitute
        public String[] run() {
            // still allow host name to be overridden
            String qualifiedHostName = System.getProperty("jboss.qualified.host.name");
            String providedHostName = System.getProperty("jboss.host.name");
            String providedNodeName = System.getProperty("jboss.node.name");
            if (qualifiedHostName == null) {
                // if host name is specified, don't pick a qualified host name that isn't related to it
                qualifiedHostName = providedHostName;
                if (qualifiedHostName == null) {
                    // query the operating system
                    CCharPointer nameBuf = StackValue.get(ProcessSubstitutions.SIZE); // should be more than enough
                    int res = NativeInfo.gethostname(nameBuf, WordFactory.unsigned(ProcessSubstitutions.SIZE));
                    if (res != -1 && res > 0) {
                        if (res == ProcessSubstitutions.SIZE) {
                            // null-terminate a really long name
                            nameBuf.write(ProcessSubstitutions.SIZE - 1, (byte) 0);
                        }
                        qualifiedHostName = CTypeConversion.toJavaString(nameBuf);
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
                        && (Inet.isInet4Address(qualifiedHostName) || Inet.isInet6Address(qualifiedHostName))) {
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

    @CContext(NativeInfoDirectives.class)
    static final class NativeInfo {
        @CFunction
        static native int gethostname(CCharPointer nameBuf, UnsignedWord /* size_t */ len);
    }

    static final class NativeInfoDirectives implements CContext.Directives {
        public List<String> getHeaderFiles() {
            return Collections.singletonList("<unistd.h>");
        }
    }

    static final class ProcessSubstitutions {
        static final int SIZE = 512;
    }
}
