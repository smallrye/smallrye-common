package io.smallrye.common.process;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This class gathers some number of lines from the line queue,
 * saving the first and last {@code n} lines to be collected later.
 */
final class Gatherer {
    private final int headLines;
    private final int tailLines;
    private final ArrayList<String> head;
    private final ArrayDeque<String> tail;
    private int skipped;

    Gatherer(int headLines, int tailLines) {
        this.headLines = headLines;
        head = new ArrayList<>(headLines);
        this.tailLines = tailLines;
        tail = new ArrayDeque<>(tailLines);
    }

    public void run(final LineReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (head.size() < headLines) {
                head.add(line);
            } else if (tailLines > 0) {
                if (tail.size() == tailLines) {
                    tail.removeFirst();
                    skipped++;
                }
                tail.addLast(line);
            } else {
                skipped++;
            }
        }
    }

    public List<String> toList() {
        Optional<String> opt = skipped == 0 ? Optional.empty() : Optional.of("… (skipped " + skipped + " line(s)) …");
        return Stream.concat(Stream.concat(head.stream(), opt.stream()), tail.stream()).toList();
    }
}
