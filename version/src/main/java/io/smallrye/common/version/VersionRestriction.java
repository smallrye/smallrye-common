package io.smallrye.common.version;

import static io.smallrye.common.version.Messages.msg;

import java.util.function.Predicate;

class VersionRestriction implements Predicate<String> {

    public static final VersionRestriction EVERYTHING = new VersionRestriction(null, null, false, null, false);

    private final VersionScheme versionScheme;
    private final String lowerBound;
    private final boolean lowerVersionInclusive;
    private final String upperBound;
    private final boolean upperBoundInclusive;

    VersionRestriction(VersionScheme versionScheme, String lowerBound, boolean lowerVersionInclusive, String upperBound,
            boolean upperBoundInclusive) {
        this.versionScheme = versionScheme;
        this.lowerBound = lowerBound;
        this.lowerVersionInclusive = lowerVersionInclusive;
        this.upperBound = upperBound;
        this.upperBoundInclusive = upperBoundInclusive;
    }

    public String getLowerBound() {
        return lowerBound;
    }

    public String getUpperBound() {
        return upperBound;
    }

    @Override
    public boolean test(String s) {
        if (lowerBound != null) {
            int comparison = versionScheme.compare(s, lowerBound);
            if (comparison < 0 || (!lowerVersionInclusive && comparison == 0)) {
                return false;
            }
        }
        if (upperBound != null) {
            int comparison = versionScheme.compare(s, upperBound);
            if (comparison > 0 || (!upperBoundInclusive && comparison == 0)) {
                return false;
            }
        }
        return true;
    }

    static VersionRestriction parse(VersionScheme versionScheme, String spec) {
        boolean lowerBoundInclusive = spec.startsWith("[");
        boolean upperBoundInclusive = spec.endsWith("]");

        String process = spec.substring(1, spec.length() - 1).trim();

        final VersionRestriction restriction;

        int index = process.indexOf(',');

        if (index < 0) {
            if (!lowerBoundInclusive || !upperBoundInclusive) {
                throw msg.singleVersionMustBeSurroundedByBrackets(spec);
            }
            restriction = new VersionRestriction(versionScheme, process, true, process, true);
        } else {
            String lowerBound = process.substring(0, index).trim();
            String upperBound = process.substring(index + 1).trim();

            String lowerVersion = null;
            String upperVersion = null;

            if (!lowerBound.isEmpty()) {
                lowerVersion = lowerBound;
            }
            if (!upperBound.isEmpty()) {
                upperVersion = upperBound;
            }

            if (upperVersion != null && lowerVersion != null) {
                int result = versionScheme.compare(upperVersion, lowerVersion);
                if (result < 0 || (result == 0 && (!lowerBoundInclusive || !upperBoundInclusive))) {
                    throw msg.rangeDefiesVersionOrdering(spec);
                }
            }
            restriction = new VersionRestriction(versionScheme, lowerVersion, lowerBoundInclusive, upperVersion,
                    upperBoundInclusive);
        }
        return restriction;
    }
}
