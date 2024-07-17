package io.smallrye.common.version;

import static io.smallrye.common.version.Messages.msg;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link VersionRange} is a predicate that tests if a version string is within a specified range.
 */
public class VersionRange implements Predicate<String> {

    /**
     * Range in format "[1.0,2.0)"
     */
    private static final Pattern RANGE_PATTERN = Pattern.compile("([\\[\\(])(.*),(.*)([\\]\\)])");

    private final VersionScheme versionScheme;
    private final Bound lowerBound;
    private final Bound upperBound;

    public VersionRange(VersionScheme versionScheme, String rangePattern) {
        this.versionScheme = versionScheme;
        // Range pattern is in format "[1.0,2.0)"
        Matcher matcher = RANGE_PATTERN.matcher(rangePattern);
        if (!matcher.matches()) {
            throw msg.invalidRangePattern(rangePattern);
        }
        if (matcher.group(2).isBlank()) {
            this.lowerBound = null;
        } else {
            this.lowerBound = new Bound(matcher.group(2), matcher.group(1).charAt(0) == '[');
        }
        if (matcher.group(3).isBlank()) {
            this.upperBound = null;
        } else {
            this.upperBound = new Bound(matcher.group(3), matcher.group(4).charAt(0) == ']');
        }
    }

    @Override
    public boolean test(String s) {
        if (lowerBound != null) {
            int comparison = versionScheme.compare(s, lowerBound.version);
            if (comparison < 0 || (!lowerBound.inclusive && comparison == 0)) {
                return false;
            }
        }
        if (upperBound != null) {
            int comparison = versionScheme.compare(s, upperBound.version);
            if (comparison > 0 || (!upperBound.inclusive && comparison == 0)) {
                return false;
            }
        }
        return true;
    }

    private class Bound {

        private final String version;
        private final boolean inclusive;

        public Bound(String version, boolean inclusive) {
            this.version = version;
            this.inclusive = inclusive;
        }
    }
}
