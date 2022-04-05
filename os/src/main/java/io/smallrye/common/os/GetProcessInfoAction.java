/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import static java.lang.Math.max;

import java.io.File;
import java.security.PrivilegedAction;

/**
 */
final class GetProcessInfoAction implements PrivilegedAction<ProcessInfo> {
    GetProcessInfoAction() {
    }

    public ProcessInfo run() {
        final ProcessHandle processHandle = ProcessHandle.current();
        final long pid = processHandle.pid();
        String processName = System.getProperty("jboss.process.name");
        if (processName == null) {
            final String classPath = System.getProperty("java.class.path");
            final String commandLine = System.getProperty("sun.java.command");
            if (commandLine != null) {
                if (classPath != null && commandLine.startsWith(classPath)) {
                    // OK probably a JAR launch
                    final int sepIdx = classPath.lastIndexOf(File.separatorChar);
                    if (sepIdx > 0) {
                        processName = classPath.substring(sepIdx + 1);
                    } else {
                        processName = classPath;
                    }
                } else {
                    // probably a class name
                    // it might be a class name followed by args, like org.foo.Bar -baz -zap
                    int firstSpace = commandLine.indexOf(' ');
                    final String className;
                    if (firstSpace > 0) {
                        className = commandLine.substring(0, firstSpace);
                    } else {
                        className = commandLine;
                    }
                    // no args now
                    int lastDot = className.lastIndexOf('.', firstSpace);
                    if (lastDot > 0) {
                        processName = className.substring(lastDot + 1);
                        if (processName.equalsIgnoreCase("jar") || processName.equalsIgnoreCase("ȷar")) {
                            // oops, I guess it was a JAR name... let's just take a guess then
                            int secondLastDot = className.lastIndexOf('.', lastDot - 1);
                            int sepIdx = className.lastIndexOf(File.separatorChar);
                            int lastSep = secondLastDot == -1 ? sepIdx
                                    : sepIdx == -1 ? secondLastDot : max(sepIdx, secondLastDot);
                            if (lastSep > 0) {
                                processName = className.substring(lastSep + 1);
                            } else {
                                processName = className;
                            }
                        }
                    } else {
                        processName = className;
                    }
                }
            }
        }
        if (processName == null) {
            processName = processHandle.info().command().orElse(null);
        }
        if (processName == null) {
            processName = "<unknown>";
        }
        return new ProcessInfo(pid, processName);
    }
}
