package io.smallrye.common.vertx;

import org.assertj.core.api.Assertions;
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
        Assertions.assertThat(VertxContext.isOnDuplicatedContext()).isFalse();

        Context context = vertx.getOrCreateContext();
        context.runOnContext(x -> {
            Assertions.assertThat(VertxContext.isOnDuplicatedContext()).isFalse();
            Assertions.assertThat(VertxContext.getRootContext(Vertx.currentContext())).isSameAs(Vertx.currentContext());
            Assertions.assertThat(VertxContext.getRootContext(Vertx.currentContext())).isSameAs(context);

            Context dup = VertxContext.getOrCreateDuplicatedContext(Vertx.currentContext());
            dup.runOnContext(z -> {
                Assertions.assertThat(VertxContext.isOnDuplicatedContext()).isTrue();
                Assertions.assertThat(VertxContext.getRootContext(Vertx.currentContext())).isSameAs(context);
                tc.completeNow();
            });
        });
    }

    @Test
    public void createDuplicatedContextFromVertxInstance(Vertx vertx, VertxTestContext tc) {
        Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
        Assertions.assertThat(VertxContext.isDuplicatedContext(context)).isTrue();

        context.runOnContext(x -> {
            Assertions.assertThat(VertxContext.isOnDuplicatedContext()).isTrue();

            context.runOnContext(z -> {
                Assertions.assertThat(VertxContext.isOnDuplicatedContext()).isTrue();
                Assertions.assertThat(Vertx.currentContext()).isSameAs(context);

                Assertions.assertThat(VertxContext.getOrCreateDuplicatedContext(Vertx.currentContext()))
                        .isSameAs(Vertx.currentContext());

                Assertions.assertThat(VertxContext.getOrCreateDuplicatedContext(vertx))
                        .isSameAs(Vertx.currentContext());
                tc.completeNow();
            });

        });
    }

    @Test
    public void createDuplicatedContextFromCurrentContext(Vertx vertx, VertxTestContext tc) {
        // We are not on a Vert.x thread -> null
        Assertions.assertThat(VertxContext.getOrCreateDuplicatedContext()).isNull();

        Context context = vertx.getOrCreateContext();
        Assertions.assertThat(VertxContext.isDuplicatedContext(context)).isFalse();

        context.runOnContext(x -> {
            Assertions.assertThat(VertxContext.isOnDuplicatedContext()).isFalse();
            Context dup = VertxContext.getOrCreateDuplicatedContext();
            Assertions.assertThat(dup).isNotNull();
            Assertions.assertThat(VertxContext.isDuplicatedContext(dup)).isTrue();
            Assertions.assertThat(VertxContext.getRootContext(dup)).isSameAs(context);

            dup.runOnContext(z -> {
                Assertions.assertThat(VertxContext.isOnDuplicatedContext()).isTrue();
                Assertions.assertThat(VertxContext.getRootContext(Vertx.currentContext())).isSameAs(context);

                Assertions.assertThat(VertxContext.getOrCreateDuplicatedContext())
                        .isSameAs(Vertx.currentContext());
                tc.completeNow();
            });

        });
    }

    @Test
    public void createDuplicatedContextFromContext(Vertx vertx, VertxTestContext tc) {
        vertx.runOnContext(x -> {
            Context root = Vertx.currentContext();
            Assertions.assertThat(VertxContext.isDuplicatedContext(Vertx.currentContext())).isFalse();
            Context context = VertxContext.getOrCreateDuplicatedContext(Vertx.currentContext());
            Assertions.assertThat(VertxContext.isDuplicatedContext(context)).isTrue();
            context.runOnContext(z -> {
                Assertions.assertThat(VertxContext.isOnDuplicatedContext()).isTrue();
                Assertions.assertThat(Vertx.currentContext()).isSameAs(context);
                Assertions.assertThat(root).isSameAs(((ContextInternal) Vertx.currentContext()).unwrap());

                Assertions.assertThat(VertxContext.getOrCreateDuplicatedContext(Vertx.currentContext()))
                        .isSameAs(Vertx.currentContext());

                tc.completeNow();
            });
        });
    }

    @Test
    public void createNewDuplicatedContext(Vertx vertx, VertxTestContext tc) {
        Assertions.assertThat(VertxContext.createNewDuplicatedContext()).isNull();
        vertx.runOnContext(x -> {
            Context actual1 = VertxContext.createNewDuplicatedContext();
            Assertions.assertThat(actual1).isNotNull();
            Assertions.assertThat(VertxContext.isDuplicatedContext(actual1)).isTrue();

            Context actual2 = VertxContext.createNewDuplicatedContext();
            Assertions.assertThat(actual2).isNotNull();
            Assertions.assertThat(actual2).isNotSameAs(actual1);
            Assertions.assertThat(VertxContext.isDuplicatedContext(actual2)).isTrue();

            actual2.runOnContext(z -> {
                Context actual3 = VertxContext.createNewDuplicatedContext();
                Assertions.assertThat(actual3).isNotSameAs(actual2);
                Assertions.assertThat(VertxContext.isDuplicatedContext(actual3)).isTrue();

                tc.completeNow();
            });
        });
    }

    @Test
    public void createNewDuplicatedContextFromContext(Vertx vertx, VertxTestContext tc) {
        Assertions.assertThat(VertxContext.createNewDuplicatedContext(null)).isNull();

        Context context = vertx.getOrCreateContext();
        Context dc1 = VertxContext.createNewDuplicatedContext(context);
        Assertions.assertThat(dc1).isNotNull();
        Assertions.assertThat(VertxContext.isDuplicatedContext(dc1)).isTrue();

        Context dc2 = VertxContext.createNewDuplicatedContext(context);
        Assertions.assertThat(dc2).isNotNull();
        Assertions.assertThat(VertxContext.isDuplicatedContext(dc2)).isTrue();
        Assertions.assertThat(dc1).isNotSameAs(dc2);

        vertx.runOnContext(x -> {
            Context actual1 = VertxContext.createNewDuplicatedContext(Vertx.currentContext());
            Context actual2 = VertxContext.createNewDuplicatedContext();
            Assertions.assertThat(actual1).isNotNull();
            Assertions.assertThat(VertxContext.isDuplicatedContext(actual1)).isTrue();
            Assertions.assertThat(actual2).isNotNull();
            Assertions.assertThat(actual2).isNotSameAs(actual1);
            Assertions.assertThat(VertxContext.isDuplicatedContext(actual2)).isTrue();

            tc.completeNow();
        });
    }

}
