package io.smallrye.common.ref;

import java.lang.ref.ReferenceQueue;

/**
 * A reapable soft reference with an attachment. If a {@link Reaper} is given, then it will be used to asynchronously
 * clean up the referent.
 *
 * @param <T> the reference value type
 * @param <A> the attachment type
 *
 * @see java.lang.ref.SoftReference
 */
public class SoftReference<T, A> extends java.lang.ref.SoftReference<T> implements Reference<T, A>, Reapable<T, A> {
    private final A attachment;
    private final Reaper<T, A> reaper;

    /**
     * Construct a new instance.
     *
     * @param referent the referent
     */
    public SoftReference(final T referent) {
        this(referent, null, (ReferenceQueue<T>) null);
    }

    /**
     * Construct a new instance.
     *
     * @param referent the referent
     * @param attachment the attachment
     */
    public SoftReference(final T referent, final A attachment) {
        this(referent, attachment, (ReferenceQueue<T>) null);
    }

    /**
     * Construct a new instance with an explicit reference queue.
     *
     * @param referent the referent
     * @param attachment the attachment
     * @param q the reference queue to use
     */
    public SoftReference(final T referent, final A attachment, final ReferenceQueue<? super T> q) {
        super(referent, q);
        reaper = null;
        this.attachment = attachment;
    }

    /**
     * Construct a new instance with a reaper.
     *
     * @param referent the referent
     * @param attachment the attachment
     * @param reaper the reaper to use
     */
    public SoftReference(final T referent, final A attachment, final Reaper<T, A> reaper) {
        super(referent, References.ReaperThread.REAPER_QUEUE);
        this.reaper = reaper;
        this.attachment = attachment;
    }

    public Reaper<T, A> getReaper() {
        return reaper;
    }

    public A getAttachment() {
        return attachment;
    }

    public Type getType() {
        return Type.SOFT;
    }

    public String toString() {
        return "soft reference to " + get();
    }
}
