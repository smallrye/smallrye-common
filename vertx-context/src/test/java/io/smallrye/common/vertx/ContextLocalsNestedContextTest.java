
package io.smallrye.common.vertx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
class ContextLocalsNestedContextTest {

    @Test
    @Timeout(value = 2000)
    void testNestedContextIsolation(Vertx vertx, VertxTestContext testContext) {
        vertx.runOnContext(v -> {
            Context root = VertxContext.getOrCreateDuplicatedContext(vertx);
            root.runOnContext(r -> {
                ContextLocals.put("key", "rootValue");

                Context nested = VertxContext.newNestedContext(root);
                nested.runOnContext(n -> {
                    assertThat(ContextLocals.get("key")).hasValue("rootValue");
                    ContextLocals.put("key", "nestedValue");

                    assertThat(ContextLocals.get("key")).hasValue("nestedValue");
                    assertThat(ContextLocals.getFromParent("key")).hasValue("rootValue");

                    boolean putInParent = ContextLocals.putInParent("newKey", "inParent");
                    assertThat(putInParent).isTrue();
                    assertThat(ContextLocals.getFromParent("newKey")).hasValue("inParent");

                    // Trying to override should return false
                    boolean putAgain = ContextLocals.putInParent("newKey", "override");
                    assertThat(putAgain).isFalse();

                    root.runOnContext(r2 -> {
                        // The parent context should have the new key.
                        assertThat(ContextLocals.get("newKey")).hasValue("inParent");
                        testContext.completeNow();
                    });
                });
            });
        });
    }

    @Test
    @Timeout(value = 2000)
    void testNestedContextWithoutParentFails(Vertx vertx, VertxTestContext testContext) {
        vertx.runOnContext(v -> {
            Context root = VertxContext.getOrCreateDuplicatedContext(vertx);
            root.runOnContext(r -> {
                assertThatThrownBy(() -> ContextLocals.putInParent("key", "value"))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Parent context is not set");

                assertThat(ContextLocals.getFromParent("key")).isEmpty();
                testContext.completeNow();
            });
        });
    }

    @Test
    void testParentIsRootThrows(Vertx vertx, VertxTestContext testContext) {
        vertx.runOnContext(v -> {
            Context root = vertx.getOrCreateContext();
            Context nested = VertxContext.newNestedContext(root);
            nested.runOnContext(n -> {
                assertThatThrownBy(() -> ContextLocals.putInParent("key", "value"))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("The parent context is a root context");
                testContext.completeNow();
            });
        });
    }

    @Test
    void testParentRetrieval(Vertx vertx, VertxTestContext testContext) {
        vertx.runOnContext(v -> {
            Context base = VertxContext.getOrCreateDuplicatedContext(vertx);
            base.runOnContext(r -> {
                ContextLocals.put("level", "1");
                Context nested = VertxContext.newNestedContext(base);
                nested.runOnContext(n -> {
                    Context parent = ContextLocals.getParentContext();
                    assertThat(parent).isNotNull();
                    assertThat((String) parent.getLocal("level")).isEqualTo("1");
                    testContext.completeNow();
                });
            });
        });
    }

    @Test
    @Timeout(value = 2000)
    void testGetFromParentWithDefault(Vertx vertx, VertxTestContext testContext) {
        vertx.runOnContext(v -> {
            Context parent = VertxContext.getOrCreateDuplicatedContext(vertx);
            parent.runOnContext(p -> {
                ContextLocals.put("existingKey", "fromParent");

                Context nested = VertxContext.newNestedContext(parent);
                nested.runOnContext(n -> {
                    // Key exists in parent
                    String val1 = ContextLocals.getFromParent("existingKey", "defaultValue");
                    assertThat(val1).isEqualTo("fromParent");

                    // Key does not exist in parent
                    String val2 = ContextLocals.getFromParent("missingKey", "defaultValue");
                    assertThat(val2).isEqualTo("defaultValue");

                    testContext.completeNow();
                });
            });
        });
    }

    @Test
    void testMultipleNestedContextsShareParentData(Vertx vertx, VertxTestContext testContext) {
        vertx.runOnContext(v -> {
            Context parent = VertxContext.getOrCreateDuplicatedContext(vertx);
            parent.runOnContext(p -> {
                ContextLocals.put("sharedKey", "sharedValue");

                Context child1 = VertxContext.newNestedContext(parent);
                Context child2 = VertxContext.newNestedContext(parent);

                child1.runOnContext(c1 -> {
                    assertThat(ContextLocals.getFromParent("sharedKey")).hasValue("sharedValue");

                    boolean written = ContextLocals.putInParent("newSharedKey", "child1Value");
                    assertThat(written).isTrue();

                    child2.runOnContext(c2 -> {
                        assertThat(ContextLocals.getFromParent("newSharedKey")).hasValue("child1Value");

                        boolean secondWrite = ContextLocals.putInParent("newSharedKey", "child2Value");
                        assertThat(secondWrite).isFalse();

                        assertThat(ContextLocals.getFromParent("newSharedKey")).hasValue("child1Value");

                        testContext.completeNow();
                    });
                });
            });
        });
    }

    @Test
    void testNewNestedContextFromCurrentContext(Vertx vertx, VertxTestContext testContext) {
        vertx.runOnContext(v -> {
            // Create and switch to a duplicated context
            Context parent = VertxContext.getOrCreateDuplicatedContext(vertx);
            parent.runOnContext(p -> {
                ContextLocals.put("key", "parentValue");

                // Now we're in a duplicated context, so we can use newNestedContext() with no args
                Context nested = VertxContext.newNestedContext(); // <--- method under test
                nested.runOnContext(n -> {
                    // Confirm parent value is visible
                    assertThat(ContextLocals.get("key")).hasValue("parentValue");

                    // Confirm writing in child doesn't affect parent
                    ContextLocals.put("key", "childValue");
                    assertThat(ContextLocals.get("key")).hasValue("childValue");
                    assertThat(ContextLocals.getFromParent("key")).hasValue("parentValue");

                    testContext.completeNow();
                });
            });
        });
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testNewNestedContextWithNullContextThrows() {
        assertThatThrownBy(() -> VertxContext.newNestedContext(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot create a nested context from a `null` context");
    }

    @Test
    void testNewNestedContextUsingCurrentContextWithNoCurrentContextThrows() {
        assertThatThrownBy(VertxContext::newNestedContext)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot create a nested context from a `null` context");
    }

    @Test
    @Timeout(value = 2000)
    void testThatRegularDuplicationDoesNotAllowRetrievingDataFromContext(Vertx vertx, VertxTestContext testContext) {
        vertx.runOnContext(v -> {
            Context parent = VertxContext.getOrCreateDuplicatedContext(vertx);
            assertThatThrownBy(() -> ContextLocals.getFromParent("foo"))
                    .isInstanceOf(UnsupportedOperationException.class);
            parent.runOnContext(p -> {
                ContextLocals.put("key", "parentValue");

                Context dup = VertxContext.createNewDuplicatedContext();
                dup.runOnContext(n -> {
                    assertThat(ContextLocals.getFromParent("key", "bar")).isEqualTo("bar");
                    assertThat(ContextLocals.getFromParent("key")).isEmpty();
                    testContext.completeNow();
                });
            });
        });
    }
}
