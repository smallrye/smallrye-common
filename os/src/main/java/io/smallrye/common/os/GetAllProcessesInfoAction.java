package io.smallrye.common.os;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.regex.Pattern;

final class GetAllProcessesInfoAction implements PrivilegedAction<List<ProcessInfo>> {

    private final static Predicate<String> IS_NUMBER = Pattern.compile("\\d+").asPredicate();

    @Override
    public List<ProcessInfo> run() {
        // The ProcessHandle API does not exist, so we have to rely on external processes to get this information
        switch (OS.current()) {
            case LINUX:
                return readLinuxProcesses();
            case MAC:
                return readMacProcesses();
            case WINDOWS:
                return readWindowsProcesses();
            default:
                throw new UnsupportedOperationException(
                        "Listing all processes is not supported in JDK 8 in " + OS.current().name());
        }
    }

    private List<ProcessInfo> readLinuxProcesses() {
        List<ProcessInfo> processes = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("/proc"))) {
            for (Path procPath : stream) {
                String name = procPath.getFileName().toString();
                if (IS_NUMBER.test(name)) {
                    long pid = Long.parseLong(name);
                    try (BufferedReader reader = Files
                            .newBufferedReader(procPath.resolve("cmdline"), StandardCharsets.UTF_8)) {
                        String line = reader.readLine();
                        if (line != null) {
                            int idx = line.indexOf(0);
                            String cmdLine = idx == -1 ? line : line.substring(0, idx);
                            processes.add(new ProcessInfo(pid, cmdLine));
                        }
                    } catch (IOException ignored) {
                        // ignore case where process exits right before we read cmdline
                    }
                }
            }
        } catch (IOException ignored) {
            // ignore
        }
        return processes;
    }

    private List<ProcessInfo> readMacProcesses() {
        List<ProcessInfo> processes = new ArrayList<>();
        java.lang.Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/usr/bin/ps", "-ax", "-o", "pid=,comm=");
            String thisCmd = String.join(" ", processBuilder.command());
            process = processBuilder.start();
            try (Scanner scanner = new Scanner(process.getInputStream())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    int separator = line.indexOf(" ");
                    String cmd = line.substring(separator + 1);
                    if (!thisCmd.equals(cmd)) {
                        long pid = Long.parseLong(line.substring(0, separator));
                        processes.add(new ProcessInfo(pid, cmd));
                    }
                }
            }
        } catch (IOException e) {
            // ignored
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return processes;
    }

    private List<ProcessInfo> readWindowsProcesses() {
        List<ProcessInfo> processes = new ArrayList<>();
        java.lang.Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("%WINDIR%\\system32\\tasklist.exe", "/fo", "csv", "/nh");
            String thisCmd = String.join(" ", processBuilder.command());
            process = processBuilder.start();
            try (Scanner sc = new Scanner(process.getInputStream())) {
                // Skip first line
                if (sc.hasNextLine()) {
                    sc.nextLine();
                }
                while (sc.hasNextLine()) {
                    String line = sc.nextLine().trim();
                    String[] parts = line.split(",");
                    String cmdLine = parts[0].substring(1).replaceFirst(".$", "");
                    if (!thisCmd.equals(cmdLine)) {
                        long pid = Long.parseLong(parts[1].substring(1).replaceFirst(".$", ""));
                        processes.add(new ProcessInfo(pid, cmdLine));
                    }
                }
            }
        } catch (IOException e) {
            // ignored
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return processes;
    }
}
