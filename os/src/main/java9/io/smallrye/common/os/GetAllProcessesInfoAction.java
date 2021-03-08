package io.smallrye.common.os;

import java.security.PrivilegedAction;
import java.util.List;
import java.util.stream.Collectors;

final class GetAllProcessesInfoAction implements PrivilegedAction<List<ProcessInfo>> {

    @Override
    public List<ProcessInfo> run() {
        return ProcessHandle.allProcesses()
                .map(processHandle -> new ProcessInfo(processHandle.pid(), processHandle.info().command().orElse(null)))
                .collect(Collectors.toList());
    }
}
