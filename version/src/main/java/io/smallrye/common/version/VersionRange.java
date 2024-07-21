package io.smallrye.common.version;

import static io.smallrye.common.version.Messages.msg;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A {@link VersionRange} is a predicate that tests if a version string is within a specified range.
 */
public class VersionRange implements Predicate<String> {

    private final List<VersionRestriction> restrictions;

    VersionRange(List<VersionRestriction> restrictions) {
        this.restrictions = restrictions;
    }

    @Override
    public boolean test(String s) {
        for (VersionRestriction restriction : restrictions) {
            if (restriction.test(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * Create a version range from a string representation
     * </p>
     * Some spec examples are:
     * <ul>
     * <li><code>1.0</code> Version 1.0 as a recommended version</li>
     * <li><code>[1.0]</code> Version 1.0 explicitly only</li>
     * <li><code>[1.0,2.0)</code> Versions 1.0 (included) to 2.0 (not included)</li>
     * <li><code>[1.0,2.0]</code> Versions 1.0 to 2.0 (both included)</li>
     * <li><code>[1.5,)</code> Versions 1.5 and higher</li>
     * <li><code>(,1.0],[1.2,)</code> Versions up to 1.0 (included) and 1.2 or higher</li>
     * </ul>
     *
     * @param spec string representation of a version or version range
     * @return a new {@link VersionRange} object that represents the spec
     * @return null if the spec is null
     */
    public static VersionRange createFromVersionSpec(VersionScheme scheme, String spec) {
        if (spec == null) {
            return null;
        }

        List<VersionRestriction> restrictions = new ArrayList<>();
        String process = spec;
        String upperBound = null;
        String lowerBound = null;

        while (process.startsWith("[") || process.startsWith("(")) {
            int index1 = process.indexOf(')');
            int index2 = process.indexOf(']');

            int index = index2;
            if (index2 < 0 || index1 < index2) {
                if (index1 >= 0) {
                    index = index1;
                }
            }

            if (index < 0) {
                throw msg.unboundedRange(spec);
            }

            VersionRestriction restriction = VersionRestriction.parse(scheme, process.substring(0, index + 1));
            if (lowerBound == null) {
                lowerBound = restriction.getLowerBound();
            }
            if (upperBound != null) {
                if (restriction.getLowerBound() == null
                        || scheme.lt(restriction.getLowerBound(), upperBound)) {
                    throw msg.rangesOverlap(spec);
                }
            }
            restrictions.add(restriction);
            upperBound = restriction.getUpperBound();

            process = process.substring(index + 1).trim();

            if (process.startsWith(",")) {
                process = process.substring(1).trim();
            }
        }

        if (!process.isEmpty()) {
            if (!restrictions.isEmpty()) {
                throw msg.onlyFullyQualifiedSetsAllowed(spec);
            } else {
                restrictions.add(VersionRestriction.EVERYTHING);
            }
        }

        return new VersionRange(restrictions);
    }
}
