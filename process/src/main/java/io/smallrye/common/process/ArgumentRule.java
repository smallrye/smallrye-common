package io.smallrye.common.process;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The rule for argument validation.
 */
enum ArgumentRule {
    DEFAULT {
        void checkArguments(final List<String> arguments) throws IllegalArgumentException {
            // no operation (accepts any)
        }

        List<String> formatArguments(final Path command, final List<String> arguments, final boolean specialQuoting)
                throws IllegalArgumentException {
            return Stream.concat(Stream.of(command.toString()), arguments.stream()).toList();
        }
    },
    BATCH {
        void checkArguments(final List<String> arguments) throws IllegalArgumentException {
            for (String argument : arguments) {
                int len = argument.length();
                if (len == 0) {
                    throw invalidCharacter(argument, 0);
                }
                boolean qs = argument.startsWith("\"");
                boolean qe = argument.endsWith("\"");
                if (qs && !qe) {
                    throw invalidCharacter(argument, 0);
                } else if (qe && !qs) {
                    throw invalidCharacter(argument, argument.length() - 1);
                }
                int cp;
                for (int i = 0; i < len; i += Character.charCount(cp)) {
                    cp = argument.codePointAt(i);
                    if (Character.isISOControl(cp) || cp == '%') {
                        throw invalidCharacter(argument, i);
                    }
                }
            }
        }

        List<String> formatArguments(final Path command, final List<String> arguments, final boolean specialQuoting)
                throws IllegalArgumentException {
            // in the future, this will be replaced with a variation which has extra quoting capabilities
            ArrayList<String> list = new ArrayList<>(arguments.size() + 5);
            if (specialQuoting) {
                StringBuilder sb = new StringBuilder();
                list.add(quote(command.toString(), sb));
                for (final String argument : arguments) {
                    list.add(quote(argument, sb));
                }
            } else {
                list.add(command.toString());
                list.addAll(arguments);
            }
            return list;
        }

        private String quote(String str, StringBuilder sb) {
            if (str.startsWith("\"") && str.endsWith("\"")) {
                str = str.substring(1, str.length() - 1);
            }
            boolean quoting = false;
            int start = sb.length();
            int cp;
            for (int i = 0; i < str.length(); i += Character.charCount(cp)) {
                cp = str.codePointAt(i);
                switch (cp) {
                    case '&', '<', '>', '[', ']', '{', '}', '^', '"',
                            '=', ';', '!', '\'', '+', ',', '`', '~' -> {
                        sb.append('^').appendCodePoint(cp);
                        if (!quoting) {
                            quoting = true;
                            sb.insert(start, '"');
                        }
                    }
                    default -> sb.appendCodePoint(cp);
                }
            }
            if (quoting) {
                sb.append('"');
            }
            try {
                return sb.toString();
            } finally {
                sb.setLength(0);
            }
        }
    },
    POWERSHELL {
        void checkArguments(final List<String> arguments) throws IllegalArgumentException {
            // OK
        }

        List<String> formatArguments(final Path command, final List<String> arguments, final boolean specialQuoting)
                throws IllegalArgumentException {
            return Stream.concat(
                    Stream.of("powershell.exe", "-ExecutionPolicy", "Bypass", "-File", command.toString()),
                    arguments.stream()).toList();
        }
    };

    IllegalArgumentException invalidCharacter(final String argument, final int idx) {
        return new IllegalArgumentException("Argument \"%s\" has an invalid character at index %d for argument rule %s"
                .formatted(argument, idx, this));
    }

    abstract void checkArguments(List<String> arguments) throws IllegalArgumentException;

    abstract List<String> formatArguments(Path command, List<String> arguments, boolean specialQuoting)
            throws IllegalArgumentException;
}
