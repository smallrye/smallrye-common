/**
 * Common annotations for SmallRye projects and their consumers.
 */
module io.smallrye.common.annotation {
    requires static jakarta.cdi;
    requires static jakarta.inject;

    exports io.smallrye.common.annotation;
}