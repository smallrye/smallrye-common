package io.smallrye.common.vertx;

import java.util.concurrent.ConcurrentMap;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.constraint.Nullable;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

/**
 * Utility classes allowing to create duplicated and nested contexts.
 * <p>
 * The rationale for this class is to avoid end-users to use the Vert.x internal API, and avoids common mistakes when
 * dealing with Contexts and duplicated Contexts.
 * </p>
 * <p>
 * With this class, Vert.x contexts can be categorized into three types:
 * </p>
 * <h2>1. Root Context</h2>
 * The original context associated with a Vert.x thread (usually an event loop context).
 * This context is long-lived and shared across multiple asynchronous operations.
 * Because the data stored in this context is shared across operations, it is not safe for storing transient or request-scoped
 * data.
 *
 * <pre>
 * +---------------------------+
 * |       Event Loop          |
 * |       (Root Context)      |
 * +---------------------------+
 * </pre>
 *
 * <h2>2. Duplicated Context</h2>
 * A lightweight, short-lived copy of a root context used for isolating state.
 * <p>
 * Duplicated contexts provide a safe space for request or task-scoped data.
 * </p>
 *
 * <pre>
 * +---------------------------+
 * |       Event Loop          |
 * |       (Root Context)      |
 * +---------------------------+
 *            |
 *            v
 * +---------------------------+
 * |    Duplicated Context     |
 * |   (copy of root context)  |
 * +---------------------------+
 * </pre>
 *
 * <h2>3. Nested Context</h2>
 * A duplicated context that is derived from another duplicated context, forming a parent-child relationship.
 * <p>
 * Nested contexts copy the local state of their parent on creation but maintain isolation afterward.
 * A reference to the parent context is retained, enabling access to shared data when necessary.
 * </p>
 *
 * <pre>
 *  +---------------------------+
 *  |       Event Loop          |
 *  |       (Root Context)      |
 *  +---------------------------+
 *          |
 *          V
 * +---------------------------+
 * |    Duplicated Context     |  --- parent
 * |    (e.g., request scope)  |
 * +---------------------------+
 *            |
 *            v
 * +---------------------------+
 * |     Nested Context        |
 * |  (e.g., REST call scope)  |
 * +---------------------------+
 * </pre>
 *
 * <h2>Summary of Behavior:</h2>
 * <ul>
 * <li>Root to Duplicated: isolates state, safe for short-lived operations</li>
 * <li>Duplicated to️ Nested: adds parent-awareness and controlled inheritance</li>
 * <li>Root Shared Locals: discouraged due to data leakage risk</li>
 * </ul>
 */
public class VertxContext {

    /**
     * The key used to store the parent context in a nested context.
     */
    public static final String PARENT_CONTEXT = "__PARENT_CONTEXT__";

    private VertxContext() {
        // Avoid direct instantiation
    }

    /**
     * Gets or creates a duplicated context.
     * If the given context is already a duplicated context, it returns this duplicated context.
     * Otherwise, it creates a new duplicated context, using the given context as <em>root</em> context.
     *
     * @param context the context, must not be {@code null}
     * @return the given context if it's a duplicated context, or a new duplicated context using the given one as
     *         <em>root</em>.
     */
    public static Context getOrCreateDuplicatedContext(Context context) {
        if (isDuplicatedContext(context)) { // Also checks that the context is not null
            return context;
        } else {
            return ((ContextInternal) context).duplicate();
        }
    }

    /**
     * Gets or creates a duplicated context.
     * <p>
     * If this method is called from a non-Vert.x thread (so, there is no current context), it creates a new (event loop)
     * context and a duplicated context (and returns the duplicated context).
     * <p>
     * If this method is called from a Vert.x thread (so, there is a current context), and if the current context is
     * already a duplicated context, it returns the current duplicated context.
     * <p>
     * It this method is called from a Vert.x thread (so, there is a current context), and if the current context is not
     * a duplicated context, it creates a new duplicated context, using the current context as <em>root</em> context.
     *
     * @param vertx the Vert.x instance to use to create the context if needed. Must not be {@code null}
     * @return the current context if it's a duplicated context, or a new duplicated context.
     * @see #getOrCreateDuplicatedContext(Context)
     */
    public static Context getOrCreateDuplicatedContext(Vertx vertx) {
        //Intentionally avoid to reassign the return from checkNotNullParam
        //as it will trigger JDK-8180450
        Assert.checkNotNullParam("vertx", vertx);
        Context context = vertx.getOrCreateContext(); // Creates an event loop context if none
        if (isDuplicatedContext(context)) { // Also checks that the context is not null
            return context;
        } else {
            return ((ContextInternal) context).duplicate();
        }
    }

    /**
     * Gets or creates a duplicated context.
     * <p>
     * If the method is not called from a Vert.x thread, it returns {@code null}.
     * If the caller context is already a duplicated context, it returns this duplicated context.
     * Otherwise, it creates a new duplicated context, using current context as <em>root</em> context.
     *
     * @return the current context if it's a duplicated context, a new duplicated context using the given one
     *         as <em>root</em>, {@code null} if not called from a Vert.x thread.
     * @see #getOrCreateDuplicatedContext(Context)
     */
    public static @Nullable Context getOrCreateDuplicatedContext() {
        Context context = Vertx.currentContext();
        if (context == null) {
            return null;
        }
        return getOrCreateDuplicatedContext(context);
    }

    /**
     * Creates a new duplicated context, even if the current one is already a duplicated context.
     * If the method is not called from a Vert.x thread, it returns {@code null}.
     *
     * @return a new duplicated context if called from a Vert.x thread, {@code null} otherwise.
     */
    public static @Nullable Context createNewDuplicatedContext() {
        return createNewDuplicatedContext(Vertx.currentContext());
    }

    /**
     * Creates a new duplicated context, even if the passed one is already a duplicated context.
     * If the passed context is {@code null}, it returns {@code null}
     *
     * @param context the context
     * @return a new duplicated context created from the given context, {@code null} is the passed context is {@code null}
     */
    public static @Nullable Context createNewDuplicatedContext(Context context) {
        if (context == null) {
            return null;
        }
        // If we are on a duplicated context, we just duplicate the root context
        if (isDuplicatedContext(context)) {
            // We are on a duplicated context, so we duplicate the root context
            return ((ContextInternal) context).unwrap().duplicate();
        }

        return ((ContextInternal) context).duplicate();
    }

    /**
     * Checks if the given context is a duplicated context.
     *
     * @param context the context, must not be {@code null}
     * @return {@code true} if the given context is a duplicated context, {@code false} otherwise.
     */
    public static boolean isDuplicatedContext(Context context) {
        //Do not use Assert.checkNotNullParam with type io.vertx.core.Context as it is likely
        //to trigger a performance issue via JDK-8180450.
        //Identified via https://github.com/franz1981/type-pollution-agent
        //So we cast to ContextInternal first:
        ContextInternal actual = Assert.checkNotNullParam("context", (ContextInternal) context);
        return actual.isDuplicate();
    }

    /**
     * Checks if the current context is a duplicated context.
     * If the method is called from a Vert.x thread, it retrieves the current context and checks if it's a duplicated
     * context. Otherwise, it returns false.
     *
     * @return {@code true} if the method is called from a duplicated context, {@code false} otherwise.
     */
    public static boolean isOnDuplicatedContext() {
        Context context = Vertx.currentContext();
        return context != null && isDuplicatedContext(context);
    }

    /**
     * Returns the parent context from a given Vert.x context.
     * <p>
     * A duplicate context returns the wrapped context otherwise the given context is returned.
     *
     * @param context the context, must not be {@code null}
     * @return the <em>root</em> context if the given context is a duplicated context, returns the given context otherwise.
     */
    public static Context getRootContext(Context context) {
        return isDuplicatedContext(context) ? ((ContextInternal) context).unwrap() : context;
    }

    /**
     * Creates a new <em>nested</em> context from the given context.
     * <p>
     * A nested context is a duplicated context that has a reference to the parent context.
     * It's an artificial way to create a hierarchy of contexts.
     * </p>
     * <p>
     * If the given context is a root context, it creates a new (regulaR) duplicated context.
     * If the given context is a duplicated context, it creates a new duplicated context and copies the context locals.
     * <p>
     * When creating a nested context, the locals from the parent context are copied to the nested context.
     * In this way, the nested context can access the parent context locals, but writes are not propagated to the parent
     * (unless made explicitly).
     * <p>
     * Nested contexts keep a reference to the parent context, so, it's possible to navigate the context hierarchy.
     *
     * @param context the context, must not be {@code null}
     * @return a new nested context from the given context.
     */
    @SuppressWarnings("deprecation")
    public static Context newNestedContext(Context context) {
        if (context == null) {
            throw new IllegalStateException("Cannot create a nested context from a `null` context");
        }
        if (!isDuplicatedContext(context)) {
            // We are on the root context, so, just do regular duplication
            var nested = ((ContextInternal) context).duplicate();
            nested.putLocal(PARENT_CONTEXT, context);
            return nested;
        }
        // We are on a duplicated context, so, we create a new nested context.
        // Step 1 - create a new duplicated context from the root context
        // Step 2 - copy the context locals
        // Step 3 - add links to the current context (the duplicated one)
        var nested = ((ContextInternal) context).duplicate();
        ConcurrentMap<Object, Object> map = ((ContextInternal) context).localContextData();
        nested.localContextData().putAll(map);
        nested.putLocal(PARENT_CONTEXT, context);
        return nested;
    }

    /**
     * Creates a new <em>nested</em> context from the current context.
     *
     * @return a new nested context from the current context.
     * @see #newNestedContext(Context)
     */
    public static Context newNestedContext() {
        return newNestedContext(Vertx.currentContext());
    }

}
