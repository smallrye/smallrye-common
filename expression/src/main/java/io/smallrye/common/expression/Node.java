package io.smallrye.common.expression;

import java.util.HashSet;
import java.util.List;

import io.smallrye.common.function.ExceptionBiConsumer;

abstract class Node {

    static final Node[] NO_NODES = new Node[0];

    Node() {
    }

    static Node fromList(List<Node> list) {
        if (list == null || list.isEmpty()) {
            return NULL;
        } else if (list.size() == 1) {
            return list.get(0);
        } else {
            return new CompositeNode(list);
        }
    }

    static final Node NULL = new Node() {
        <E extends Exception> void emit(final ResolveContext<E> context,
                final ExceptionBiConsumer<ResolveContext<E>, StringBuilder, E> resolveFunction) throws E {
        }

        void catalog(final HashSet<String> strings) {
        }

        public String toString() {
            return "<<null>>";
        }
    };

    abstract <E extends Exception> void emit(final ResolveContext<E> context,
            final ExceptionBiConsumer<ResolveContext<E>, StringBuilder, E> resolveFunction) throws E;

    abstract void catalog(final HashSet<String> strings);
}
