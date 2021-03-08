/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smallrye.common.os;

import static java.security.AccessController.doPrivileged;

import java.util.List;

/**
 * Utilities for getting information about the current process.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Process {
    private static final ProcessInfo currentProcess;

    static {
        currentProcess = doPrivileged(new GetProcessInfoAction());
    }

    private Process() {
    }

    /**
     * Get the name of this process. If the process name is not known, then "&lt;unknown&gt;" is returned.
     *
     * @return the process name (not {@code null})
     */
    public static String getProcessName() {
        return currentProcess.getCommand();
    }

    /**
     * Get the ID of this process. This is the operating system specific PID. If the PID cannot be determined,
     * -1 is returned.
     *
     * @return the ID of this process, or -1 if it cannot be determined
     */
    public static long getProcessId() {
        return currentProcess.getId();
    }

    /**
     * Returns information about the current process
     *
     * @return the current process
     */
    public static ProcessInfo getCurrentProcess() {
        return currentProcess;
    }

    /**
     * Returns all the running processes.
     *
     * @return a list of all the running processes. May throw an exception if running on an unsupported JDK
     * @throws UnsupportedOperationException if running on JDK 8
     */
    public static List<ProcessInfo> getAllProcesses() {
        return doPrivileged(new GetAllProcessesInfoAction());
    }
}
