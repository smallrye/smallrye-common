package io.smallrye.common.process;

import java.nio.file.Path;
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

        List<String> formatArguments(final Path command, final List<String> arguments) throws IllegalArgumentException {
            return Stream.concat(Stream.of(command.toString()), arguments.stream()).toList();
        }
    },
    BATCH {
        void checkArguments(final List<String> arguments) throws IllegalArgumentException {
            // for now: accept any "safe" string, excluding empty strings
            for (String argument : arguments) {
                int len = argument.length();
                int cp;
                for (int i = 0; i < len; i += Character.charCount(cp)) {
                    cp = argument.codePointAt(i);
                    switch (cp) {
                        case '&', '<', '>', '[', ']', '{', '}', '^',
                                '=', ';', '!', '\'', '+', ',', '`', '~' ->
                            throw invalidCharacter(argument, i);
                        default -> {
                            if (Character.isWhitespace(cp) || Character.isISOControl(cp)) {
                                throw invalidCharacter(argument, i);
                            }
                        }
                    }
                }
            }
        }

        List<String> formatArguments(final Path command, final List<String> arguments) throws IllegalArgumentException {
            // in the future, this will be replaced with a variation which has extra quoting capabilities
            return Stream.concat(Stream.of(command.toString()), arguments.stream()).toList();
        }
    },
    POWERSHELL {
        void checkArguments(final List<String> arguments) throws IllegalArgumentException {
            // OK
        }

        List<String> formatArguments(final Path command, final List<String> arguments) throws IllegalArgumentException {
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

    abstract List<String> formatArguments(Path command, List<String> arguments) throws IllegalArgumentException;
}
