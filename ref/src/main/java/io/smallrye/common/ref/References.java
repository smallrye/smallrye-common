package io.smallrye.common.ref;

import static java.security.AccessController.doPrivileged;

import java.lang.ref.ReferenceQueue;
import java.security.PrivilegedAction;

import org.graalvm.nativeimage.ImageInfo;

import io.smallrye.common.constraint.Assert;

/**
 * A set of utility methods for reference types.
 */
public final class References {
    private References() {
    }

    private static final Reference<?, ?> NULL = new StrongReference<>(null);

    static final class BuildTimeHolder {
        static final ReferenceQueue<Object> REAPER_QUEUE = new ReferenceQueue<Object>();
    }

    static final class ReaperThread extends Thread {
        static ReferenceQueue<Object> getReaperQueue() {
            return BuildTimeHolder.REAPER_QUEUE;
        }

        static {
            if (isBuildTime()) {
                // do nothing (class should be reinitialized)
            } else {
                doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        return startThreadAction(1);
                    }
                });
            }
        }

        private static boolean isBuildTime() {
            try {
                return ImageInfo.inImageBuildtimeCode();
            } catch (Throwable ignored) {
                return false;
            }
        }

        private static Void startThreadAction(int id) {
            final ReaperThread thr = new ReaperThread();
            // avoid + because of startup cost of StringConcatFactory
            thr.setName("Reference Reaper #".concat(String.valueOf(id)));
            thr.setDaemon(true);
            thr.start();
            return null;
        }

        public void run() {
            for (;;)
                try {
                    final java.lang.ref.Reference<?> ref = ReaperThread.getReaperQueue().remove();
                    if (ref instanceof CleanerReference) {
                        ((CleanerReference<?, ?>) ref).clean();
                    }
                    if (ref instanceof Reapable) {
                        reap((Reapable<?, ?>) ref);
                    }
                } catch (InterruptedException ignored) {
                    // we consume interrupts.
                } catch (Throwable cause) {
                    // ignore failures.
                }
        }

        @SuppressWarnings({ "unchecked" })
        private static <T, A> void reap(final Reapable<T, A> reapable) {
            reapable.getReaper().reap((Reference<T, A>) reapable);
        }
    }

    /**
     * Create a reference of a given type with the provided value and attachment. If the reference type is
     * {@link Reference.Type#STRONG} or {@link Reference.Type#NULL} then the reaper argument is ignored. If
     * the reference type is {@link Reference.Type#NULL} then the value and attachment arguments are ignored.
     *
     * @param type the reference type
     * @param value the reference value
     * @param attachment the attachment value
     * @param reaper the reaper to use, if any
     * @param <T> the reference value type
     * @param <A> the reference attachment type
     * @return the reference
     */
    public static <T, A> Reference<T, A> create(Reference.Type type, T value, A attachment, Reaper<T, A> reaper) {
        Assert.checkNotNullParam("type", type);
        if (value == null) {
            type = Reference.Type.NULL;
        }
        switch (type) {
            case STRONG:
                return new StrongReference<T, A>(value, attachment);
            case WEAK:
                return new WeakReference<T, A>(value, attachment, reaper);
            case PHANTOM:
                return new PhantomReference<T, A>(value, attachment, reaper);
            case SOFT:
                return new SoftReference<T, A>(value, attachment, reaper);
            case NULL:
                return attachment == null ? getNullReference() : new StrongReference<>(null, attachment);
            default:
                throw Assert.impossibleSwitchCase(type);
        }
    }

    /**
     * Create a reference of a given type with the provided value and attachment. If the reference type is
     * {@link Reference.Type#STRONG} or {@link Reference.Type#NULL} then the reference queue argument is ignored. If
     * the reference type is {@link Reference.Type#NULL} then the value and attachment arguments are ignored.
     *
     * @param type the reference type
     * @param value the reference value
     * @param attachment the attachment value
     * @param referenceQueue the reference queue to use, if any
     * @param <T> the reference value type
     * @param <A> the reference attachment type
     * @return the reference
     */
    public static <T, A> Reference<T, A> create(Reference.Type type, T value, A attachment,
            ReferenceQueue<? super T> referenceQueue) {
        Assert.checkNotNullParam("type", type);
        if (referenceQueue == null)
            return create(type, value, attachment);
        if (value == null) {
            type = Reference.Type.NULL;
        }
        switch (type) {
            case STRONG:
                return new StrongReference<T, A>(value, attachment);
            case WEAK:
                return new WeakReference<T, A>(value, attachment, referenceQueue);
            case PHANTOM:
                return new PhantomReference<T, A>(value, attachment, referenceQueue);
            case SOFT:
                return new SoftReference<T, A>(value, attachment, referenceQueue);
            case NULL:
                return attachment == null ? getNullReference() : new StrongReference<>(null, attachment);
            default:
                throw Assert.impossibleSwitchCase(type);
        }
    }

    /**
     * Create a reference of a given type with the provided value and attachment. If the reference type is
     * {@link Reference.Type#PHANTOM} then this method will return a {@code null} reference because
     * such references are not constructable without a queue or reaper. If the reference type is
     * {@link Reference.Type#NULL} then the value and attachment arguments are ignored.
     *
     * @param type the reference type
     * @param value the reference value
     * @param attachment the attachment value
     * @param <T> the reference value type
     * @param <A> the reference attachment type
     * @return the reference
     */
    public static <T, A> Reference<T, A> create(Reference.Type type, T value, A attachment) {
        Assert.checkNotNullParam("type", type);
        if (value == null) {
            type = Reference.Type.NULL;
        }
        switch (type) {
            case STRONG:
                return new StrongReference<T, A>(value, attachment);
            case WEAK:
                return new WeakReference<T, A>(value, attachment);
            case SOFT:
                return new SoftReference<T, A>(value, attachment);
            case PHANTOM:
            case NULL:
                return attachment == null ? getNullReference() : new StrongReference<>(null, attachment);
            default:
                throw Assert.impossibleSwitchCase(type);
        }
    }

    /**
     * Get a {@code null} reference. This reference type is always cleared and does not retain an attachment; as such
     * there is only one single instance of it.
     *
     * @param <T> the reference value type
     * @param <A> the attachment value type
     * @return the {@code null} reference
     */
    @SuppressWarnings({ "unchecked" })
    public static <T, A> Reference<T, A> getNullReference() {
        return (Reference<T, A>) NULL;
    }
}
