package io.smallrye.common.vertx;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class VertxContextTest {

    @Test
    public void isOnDuplicatedContext(Vertx vertx, VertxTestContext tc) {
        assertThat(VertxContext.isOnDuplicatedContext()).isFalse();

        Context context = vertx.getOrCreateContext();
        context.runOnContext(x -> {
            assertThat(VertxContext.isOnDuplicatedContext()).isFalse();
            assertThat(VertxContext.getRootContext(Vertx.currentContext())).isSameAs(Vertx.currentContext());
            assertThat(VertxContext.getRootContext(Vertx.currentContext())).isSameAs(context);

            Context dup = VertxContext.getOrCreateDuplicatedContext(Vertx.currentContext());
            dup.runOnContext(z -> {
                assertThat(VertxContext.isOnDuplicatedContext()).isTrue();
                assertThat(VertxContext.getRootContext(Vertx.currentContext())).isSameAs(context);
                tc.completeNow();
            });
        });
    }

    @Test
    public void createDuplicatedContextFromVertxInstance(Vertx vertx, VertxTestContext tc) {
        Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
        assertThat(VertxContext.isDuplicatedContext(context)).isTrue();

        context.runOnContext(x -> {
            assertThat(VertxContext.isOnDuplicatedContext()).isTrue();

            context.runOnContext(z -> {
                assertThat(VertxContext.isOnDuplicatedContext()).isTrue();
                assertThat(Vertx.currentContext()).isSameAs(context);

                assertThat(VertxContext.getOrCreateDuplicatedContext(Vertx.currentContext()))
                        .isSameAs(Vertx.currentContext());

                assertThat(VertxContext.getOrCreateDuplicatedContext(vertx))
                        .isSameAs(Vertx.currentContext());
                tc.completeNow();
            });

        });
    }

    @Test
    public void createDuplicatedContextFromCurrentContext(Vertx vertx, VertxTestContext tc) {
        // We are not on a Vert.x thread -> null
        assertThat(VertxContext.getOrCreateDuplicatedContext()).isNull();

        Context context = vertx.getOrCreateContext();
        assertThat(VertxContext.isDuplicatedContext(context)).isFalse();

        context.runOnContext(x -> {
            assertThat(VertxContext.isOnDuplicatedContext()).isFalse();
            Context dup = VertxContext.getOrCreateDuplicatedContext();
            assertThat(dup).isNotNull();
            assertThat(VertxContext.isDuplicatedContext(dup)).isTrue();
            assertThat(VertxContext.getRootContext(dup)).isSameAs(context);

            dup.runOnContext(z -> {
                assertThat(VertxContext.isOnDuplicatedContext()).isTrue();
                assertThat(VertxContext.getRootContext(Vertx.currentContext())).isSameAs(context);

                assertThat(VertxContext.getOrCreateDuplicatedContext())
                        .isSameAs(Vertx.currentContext());
                tc.completeNow();
            });

        });
    }

    @Test
    public void createDuplicatedContextFromContext(Vertx vertx, VertxTestContext tc) {
        vertx.runOnContext(x -> {
            Context root = Vertx.currentContext();
            assertThat(VertxContext.isDuplicatedContext(Vertx.currentContext())).isFalse();
            Context context = VertxContext.getOrCreateDuplicatedContext(Vertx.currentContext());
            assertThat(VertxContext.isDuplicatedContext(context)).isTrue();
            context.runOnContext(z -> {
                assertThat(VertxContext.isOnDuplicatedContext()).isTrue();
                assertThat(Vertx.currentContext()).isSameAs(context);
                assertThat(root).isSameAs(((ContextInternal) Vertx.currentContext()).unwrap());

                assertThat(VertxContext.getOrCreateDuplicatedContext(Vertx.currentContext()))
                        .isSameAs(Vertx.currentContext());

                tc.completeNow();
            });
        });
    }

    @Test
    public void createNewDuplicatedContext(Vertx vertx, VertxTestContext tc) {
        assertThat(VertxContext.createNewDuplicatedContext()).isNull();
        vertx.runOnContext(x -> {
            Context actual1 = VertxContext.createNewDuplicatedContext();
            assertThat(actual1).isNotNull();
            assertThat(VertxContext.isDuplicatedContext(actual1)).isTrue();

            Context actual2 = VertxContext.createNewDuplicatedContext();
            assertThat(actual2).isNotNull();
            assertThat(actual2).isNotSameAs(actual1);
            assertThat(VertxContext.isDuplicatedContext(actual2)).isTrue();

            actual2.runOnContext(z -> {
                Context actual3 = VertxContext.createNewDuplicatedContext();
                assertThat(actual3).isNotSameAs(actual2);
                assertThat(VertxContext.isDuplicatedContext(actual3)).isTrue();

                tc.completeNow();
            });
        });
    }

    @Test
    public void createNewDuplicatedContextFromContext(Vertx vertx, VertxTestContext tc) {
        assertThat(VertxContext.createNewDuplicatedContext(null)).isNull();

        Context context = vertx.getOrCreateContext();
        Context dc1 = VertxContext.createNewDuplicatedContext(context);
        assertThat(dc1).isNotNull();
        assertThat(VertxContext.isDuplicatedContext(dc1)).isTrue();

        Context dc2 = VertxContext.createNewDuplicatedContext(context);
        assertThat(dc2).isNotNull();
        assertThat(VertxContext.isDuplicatedContext(dc2)).isTrue();
        assertThat(dc1).isNotSameAs(dc2);

        vertx.runOnContext(x -> {
            Context actual1 = VertxContext.createNewDuplicatedContext(Vertx.currentContext());
            Context actual2 = VertxContext.createNewDuplicatedContext();
            assertThat(actual1).isNotNull();
            assertThat(VertxContext.isDuplicatedContext(actual1)).isTrue();
            assertThat(actual2).isNotNull();
            assertThat(actual2).isNotSameAs(actual1);
            assertThat(VertxContext.isDuplicatedContext(actual2)).isTrue();

            tc.completeNow();
        });
    }

    @Test
    void testDuplicationOfDuplicate(Vertx vertx, VertxTestContext tc) {
        var root = vertx.getOrCreateContext();
        assertThat(VertxContext.isDuplicatedContext(root)).isFalse();
        root.runOnContext(ignored1 -> {
            assertThat(VertxContext.getRootContext(root)).isSameAs(root);
            Context duplicated1 = VertxContext.createNewDuplicatedContext(root);
            duplicated1.runOnContext(ignored2 -> {
                assertThat(VertxContext.isDuplicatedContext(duplicated1)).isTrue();
                assertThat(VertxContext.getRootContext(duplicated1)).isSameAs(root);

                VertxContext.localContextData(duplicated1).put("duplicated1-data", "duplicated1-value");

                var duplicated2 = VertxContext.createNewDuplicatedContext(duplicated1);
                duplicated2.runOnContext(ignored3 -> {
                    assertThat(VertxContext.isDuplicatedContext(duplicated2)).isTrue();
                    assertThat(VertxContext.getRootContext(duplicated2)).isSameAs(root);
                    assertThat((String) VertxContext.localContextData(duplicated2).get("duplicated1-data")).isNull();
                    assertThat(VertxContext.getRootContext(duplicated2)).isSameAs(root);
                    tc.completeNow();
                });
            });
        });

    }

    @Test
    void testDuplicationOfDuplicateUsingVertxAPI(Vertx vertx, VertxTestContext tc) {
        var root = vertx.getOrCreateContext();
        assertThat(VertxContext.isDuplicatedContext(root)).isFalse();
        root.runOnContext(ignored1 -> {
            assertThat(VertxContext.getRootContext(root)).isSameAs(root);
            var duplicated1 = ((ContextInternal) root).duplicate();
            duplicated1.runOnContext(ignored2 -> {
                assertThat(VertxContext.isDuplicatedContext(duplicated1)).isTrue();
                assertThat(VertxContext.getRootContext(duplicated1)).isSameAs(root);

                VertxContext.localContextData(duplicated1).put("duplicated1-data", "duplicated1-value");

                var duplicated2 = ((ContextInternal) root).duplicate();
                duplicated2.runOnContext(ignored3 -> {
                    assertThat(VertxContext.isDuplicatedContext(duplicated2)).isTrue();
                    assertThat(VertxContext.getRootContext(duplicated2)).isSameAs(root);
                    assertThat((String) VertxContext.localContextData(duplicated2).get("duplicated1-data")).isNull();
                    assertThat(VertxContext.getRootContext(duplicated2)).isSameAs(root);
                    tc.completeNow();
                });
            });
        });

    }

}
