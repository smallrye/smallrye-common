package io.smallrye.common.vertx;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
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

        Context dup1 = VertxContext.getOrCreateDuplicatedContext(vertx);
        Context dup2 = VertxContext.getOrCreateDuplicatedContext(vertx);
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
}
