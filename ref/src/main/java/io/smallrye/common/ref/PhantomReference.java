package io.smallrye.common.ref;

import java.lang.ref.ReferenceQueue;

/**
 * A reapable phantom reference with an attachment. If a {@link Reaper} is given, then it will be used to asynchronously
 * clean up the referent.
 *
 * @param <T> the reference value type
 * @param <A> the attachment type
 *
 * @see java.lang.ref.PhantomReference
 */
public class PhantomReference<T, A> extends java.lang.ref.PhantomReference<T> implements Reference<T, A>, Reapable<T, A> {
    private final A attachment;
    private final Reaper<T, A> reaper;

    /**
     * Construct a new instance with an explicit reference queue.
     *
     * @param referent the referent
     * @param attachment the attachment
     * @param q the reference queue to use
     */
    public PhantomReference(final T referent, final A attachment, final ReferenceQueue<? super T> q) {
        super(referent, q);
        this.attachment = attachment;
        reaper = null;
    }

    /**
     * Construct a new instance with a reaper.
     *
     * @param referent the referent
     * @param attachment the attachment
     * @param reaper the reaper to use
     */
    public PhantomReference(final T referent, final A attachment, final Reaper<T, A> reaper) {
        super(referent, References.ReaperThread.REAPER_QUEUE);
        this.reaper = reaper;
        this.attachment = attachment;
    }

    public A getAttachment() {
        return attachment;
    }

    public Type getType() {
        return Type.PHANTOM;
    }

    public Reaper<T, A> getReaper() {
        return reaper;
    }

    public String toString() {
        return "phantom reference";
    }
}
