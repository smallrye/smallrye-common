package io.smallrye.common.vertx;

import static io.smallrye.common.constraint.Assert.checkNotNullParam;

import io.smallrye.common.constraint.Nullable;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

/**
 * Utility classes allowing to create duplicated contexts.
 * <p>
 * The rationale for this class is to avoid end-users to use the Vert.x internal API, and avoids common mistakes when
 * dealing with Contexts and duplicated Contexts.
 */
public class VertxContext {

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
        checkNotNullParam("vertx", vertx);
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
        // This creates a duplicated context from the root context of the current duplicated context (if that's one)
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
        ContextInternal actual = checkNotNullParam("context", (ContextInternal) context);
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
     * Derive a duplicated context from a parent context.
     * <p>
     * When the parent is a root context, it is simply duplicated and starts with a fresh set of locat data entries.
     * When the parent is a duplicated context, the local data from the parent are copied to the returned context.
     *
     * @param context the parent context, must not be {@code null}
     * @return a duplicated context
     * @throws IllegalArgumentException when {@code context} is {@code null}
     */
    public static Context duplicate(Context context) {
        checkNotNullParam("context", context);
        ContextInternal actual = (ContextInternal) context;
        if (!actual.isDuplicate()) {
            return actual.duplicate();
        }
        ContextInternal result = (ContextInternal) createNewDuplicatedContext(actual);
        result.contextData().putAll(actual.contextData());
        return result;
    }

    /**
     * Derive a duplicated context from the current context.
     *
     * @return a duplicated context
     * @see #duplicate(Context)
     * @see Vertx#currentContext()
     */
    public static Context duplicate() {
        return duplicate(Vertx.currentContext());
    }
}
