package io.smallrye.common.vertx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
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
    public void createParentChild(Vertx vertx, VertxTestContext tc) {
        vertx.runOnContext(v -> {
            try {
                Context parent = vertx.getOrCreateContext();
                parent.putLocal("foo", "bar");

                Context level_1 = VertxContext.duplicate(parent);
                level_1.putLocal("abc", "123");
                assertThat(level_1.<String> getLocal("foo")).isNull();
                level_1.putLocal("foo", "bar");
                assertThat(level_1.<String> getLocal("foo")).isEqualTo("bar");
                assertThat(level_1.<String> getLocal("abc")).isEqualTo("123");

                Context level_2_1 = VertxContext.duplicate(level_1);
                assertThat(level_2_1.<String> getLocal("foo")).isEqualTo("bar");
                assertThat(level_2_1.<String> getLocal("abc")).isEqualTo("123");

                Context level_2_2 = VertxContext.duplicate(level_1);
                level_2_2.putLocal("abc", "456");
                assertThat(level_2_2.<String> getLocal("foo")).isEqualTo("bar");
                assertThat(level_2_2.<String> getLocal("abc")).isEqualTo("456");
                assertThat(level_1.<String> getLocal("abc")).isEqualTo("123");

                Context level_3a_1 = VertxContext.duplicate(level_2_2);
                assertThat(level_3a_1.<String> getLocal("foo")).isEqualTo("bar");
                assertThat(level_3a_1.<String> getLocal("abc")).isEqualTo("456");

                Context level_3b_1 = VertxContext.duplicate(level_2_1);
                assertThat(level_3b_1.<String> getLocal("foo")).isEqualTo("bar");
                assertThat(level_3b_1.<String> getLocal("abc")).isEqualTo("123");

                assertThat(parent.<Object> getLocal(VertxContext.PARENT_KEY)).isNull();
                assertThat(level_1.<Object> getLocal(VertxContext.PARENT_KEY)).isNull();
                assertThat(level_2_1.<Object> getLocal(VertxContext.PARENT_KEY)).isSameAs(level_1);
                assertThat(level_2_2.<Object> getLocal(VertxContext.PARENT_KEY)).isSameAs(level_1);
                assertThat(level_3a_1.<Object> getLocal(VertxContext.PARENT_KEY)).isSameAs(level_2_2);
                assertThat(level_3b_1.<Object> getLocal(VertxContext.PARENT_KEY)).isSameAs(level_2_1);
            } catch (Throwable t) {
                tc.failNow(t);
            }
            tc.completeNow();
        });
    }

    @Test
    public void rejectNullContextForDuplication() {
        assertThatThrownBy(() -> VertxContext.duplicate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'context' may not be null");

        assertThatThrownBy(VertxContext::duplicate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parameter 'context' may not be null");
    }
}
