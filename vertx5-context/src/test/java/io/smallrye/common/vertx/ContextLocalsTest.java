package io.smallrye.common.vertx;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class ContextLocalsTest {
    @Test
    public void assertAccessFromRootContext(Vertx vertx, VertxTestContext tc) throws InterruptedException {
        Checkpoint checkpoint = tc.checkpoint(3);

        vertx.runOnContext(x -> {
            Assertions.assertThatThrownBy(() -> ContextLocals.get("foo"))
                    .isInstanceOf(UnsupportedOperationException.class);
            checkpoint.flag();
        });

        vertx.runOnContext(x -> {
            Assertions.assertThatThrownBy(() -> ContextLocals.put("foo", "bar"))
                    .isInstanceOf(UnsupportedOperationException.class);
            checkpoint.flag();
        });

        vertx.runOnContext(x -> {
            Assertions.assertThatThrownBy(() -> ContextLocals.remove("foo"))
                    .isInstanceOf(UnsupportedOperationException.class);
            checkpoint.flag();
        });
    }

    @Test
    public void assertAccessFromNoContext() {
        Assertions.assertThatThrownBy(() -> {
            ContextLocals.get("foo");
        }).isInstanceOf(UnsupportedOperationException.class);
        Assertions.assertThatThrownBy(() -> {
            ContextLocals.put("foo", "bar");
        }).isInstanceOf(UnsupportedOperationException.class);
        Assertions.assertThatThrownBy(() -> {
            ContextLocals.remove("foo");
        }).isInstanceOf(UnsupportedOperationException.class);

    }

    @Test
    public void assertAccessFromDuplicatedContext(Vertx vertx, VertxTestContext tc) {
        Checkpoint checkpoint = tc.checkpoint(2);

        // In Vert.x 5, getOrCreateContext() from a non-Vert.x thread creates a new context each time
        // So we need to get the context once and reuse it
        Context root = vertx.getOrCreateContext();
        Context dup1 = VertxContext.getOrCreateDuplicatedContext(root);
        Context dup2 = VertxContext.getOrCreateDuplicatedContext(root);
        Assertions.assertThat(((ContextInternal) dup1).unwrap())
                .isSameAs(((ContextInternal) dup2).unwrap());

        dup1.runOnContext(x -> {
            Assertions.assertThat(ContextLocals.get("foo")).isEmpty();
            ContextLocals.put("foo", "bar");

            dup1.runOnContext(z -> {
                Assertions.assertThat(ContextLocals.get("foo")).contains("bar");
                Assertions.assertThat(ContextLocals.get("null")).isEmpty();
                ContextLocals.remove("foo");

                dup1.runOnContext(w -> {
                    Assertions.assertThat(ContextLocals.get("foo")).isEmpty();
                    checkpoint.flag();
                });
            });
        });

        dup2.runOnContext(x -> {
            Assertions.assertThat(ContextLocals.get("foo")).isEmpty();
            ContextLocals.put("foo", "baz");

            Vertx.currentContext().runOnContext(z -> {
                Assertions.assertThat(ContextLocals.get("foo", "nope")).isEqualTo("baz");
                Assertions.assertThat(ContextLocals.get("missing", 42)).isEqualTo(42);
                Assertions.assertThat(ContextLocals.remove("foo")).isTrue();
                Assertions.assertThat(ContextLocals.remove("missing")).isFalse();

                Vertx.currentContext().runOnContext(w -> {
                    Assertions.assertThat(ContextLocals.get("foo")).isEmpty();
                    checkpoint.flag();
                });
            });
        });
    }

    @Test
    public void assertGetWithExplicitContext(Vertx vertx, VertxTestContext tc) {
        Checkpoint checkpoint = tc.checkpoint(1);

        Context root = vertx.getOrCreateContext();
        Context dup = VertxContext.getOrCreateDuplicatedContext(root);

        dup.runOnContext(x -> {
            Assertions.assertThat(ContextLocals.get(dup, "foo")).isEmpty();
            ContextLocals.put("foo", "bar");
            Assertions.assertThat(ContextLocals.get(dup, "foo")).contains("bar");
            Assertions.assertThat(ContextLocals.get(dup, "missing")).isEmpty();
            Assertions.assertThat(ContextLocals.get(dup, "foo", "default")).isEqualTo("bar");
            Assertions.assertThat(ContextLocals.get(dup, "missing", 42)).isEqualTo(42);
            checkpoint.flag();
        });
    }

    @Test
    public void assertGetWithExplicitContextRejectsRootContext(Vertx vertx, VertxTestContext tc) {
        Checkpoint checkpoint = tc.checkpoint(1);

        vertx.runOnContext(x -> {
            Context root = Vertx.currentContext();
            Assertions.assertThatThrownBy(() -> ContextLocals.get(root, "foo"))
                    .isInstanceOf(UnsupportedOperationException.class);
            Assertions.assertThatThrownBy(() -> ContextLocals.get(root, "foo", "def"))
                    .isInstanceOf(UnsupportedOperationException.class);
            checkpoint.flag();
        });
    }

    @Test
    public void assertGetWithNullContextFails() {
        Assertions.assertThatThrownBy(() -> ContextLocals.get((Context) null, "foo"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void assertPutAndRemoveWithExplicitContext(Vertx vertx, VertxTestContext tc) {
        Checkpoint checkpoint = tc.checkpoint(1);

        Context root = vertx.getOrCreateContext();
        Context dup = VertxContext.getOrCreateDuplicatedContext(root);

        dup.runOnContext(x -> {
            ContextLocals.put(dup, "foo", "bar");
            Assertions.assertThat(ContextLocals.get(dup, "foo")).contains("bar");

            Assertions.assertThat(ContextLocals.remove(dup, "foo")).isTrue();
            Assertions.assertThat(ContextLocals.get(dup, "foo")).isEmpty();
            Assertions.assertThat(ContextLocals.remove(dup, "foo")).isFalse();
            checkpoint.flag();
        });
    }

    @Test
    public void assertPutWithExplicitContextRejectsRootContext(Vertx vertx, VertxTestContext tc) {
        Checkpoint checkpoint = tc.checkpoint(1);

        vertx.runOnContext(x -> {
            Context root = Vertx.currentContext();
            Assertions.assertThatThrownBy(() -> ContextLocals.put(root, "foo", "bar"))
                    .isInstanceOf(UnsupportedOperationException.class);
            Assertions.assertThatThrownBy(() -> ContextLocals.remove(root, "foo"))
                    .isInstanceOf(UnsupportedOperationException.class);
            checkpoint.flag();
        });
    }
}
