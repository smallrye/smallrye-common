package io.smallrye.common.process;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.LockSupport;

/**
 * Methods for managing "critical sections" of code.
 * A "critical section" is defined as any program subunit within a thread which could have
 * a deleterious impact on the state of the running system if
 * interrupted by process exit partway through execution.
 * <p>
 * This is intended to be used with a {@code try}-with-resources construct
 * in order to ensure that the critical section is safely concluded.
 * If the returned handle is not properly used to {@code close()} the critical section,
 * the process may never exit.
 */
public final class CriticalSection {
    private static final CriticalSection INSTANCE = new CriticalSection();
    private static final Thread shutdownThread = new Thread(CriticalSection::run, "Critical section exit thread");
    private static final VarHandle stateHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "state",
            VarHandle.class, CriticalSection.class, long.class);
    private static final CopyOnWriteArraySet<Thread> interruptibleThreads = new CopyOnWriteArraySet<>();

    private static final long ST_EXITING = 1L << 63;
    private static final long CNT_MASK = -1L & ~ST_EXITING;

    static {
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    private CriticalSection() {
    }

    @SuppressWarnings("unused")
    private long state;

    static void run() {
        long oldVal = (long) stateHandle.getAndBitwiseOr(ST_EXITING) & CNT_MASK;
        while (oldVal > 0) {
            interruptibleThreads.forEach(Thread::interrupt);
            // clear interrupted flag
            Thread.interrupted();
            LockSupport.park(INSTANCE);
            oldVal = (long) stateHandle.getVolatile() & CNT_MASK;
        }
        // done
    }

    /**
     * Attempt to enter a critical section.
     * If the process is shutting down, and no other critical sections are entered,
     * this method does not return.
     *
     * @return a handle to use to exit the critical section (not {@code null})
     */
    public static Closer enter() {
        tryEnter();
        return new Closer(false);
    }

    /**
     * Attempt to enter a critical section.
     * If the process is shutting down, and no other critical sections are entered,
     * this method does not return.
     * Otherwise, this thread will be interrupted no less than one time when the
     * process is exiting.
     *
     * @return a handle to use to exit the critical section (not {@code null})
     */
    public static Closer enterInterruptibly() {
        long oldVal = tryEnter();
        if ((oldVal & ST_EXITING) != 0) {
            Thread.currentThread().interrupt();
        }
        // if this thread is already in the set, then an outer block carries the interruptible status
        return new Closer(interruptibleThreads.add(Thread.currentThread()));
    }

    private static long tryEnter() {
        long oldVal = (long) stateHandle.getVolatile();
        for (;;) {
            if (oldVal == ST_EXITING) {
                // no critical sections and exit was requested, so shutdown will commence soon
                for (;;) {
                    Thread.interrupted();
                    LockSupport.park(INSTANCE);
                }
            } else {
                long witness = (long) stateHandle.compareAndExchange(oldVal, oldVal + 1);
                if (witness == oldVal) {
                    break;
                }
                oldVal = witness;
            }
        }
        return oldVal;
    }

    /**
     * The close handle for the critical section.
     */
    public static final class Closer implements AutoCloseable {
        private static final VarHandle closedHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "closed",
                VarHandle.class, Closer.class, boolean.class);

        private final boolean interruptible;

        @SuppressWarnings("unused")
        private boolean closed;

        private Closer(final boolean interruptible) {
            this.interruptible = interruptible;
        }

        /**
         * Finish the critical section (idempotent).
         */
        public void close() {
            if (closedHandle.compareAndSet(this, false, true)) {
                long oldVal = (long) stateHandle.getAndAdd(-1L);
                if (oldVal == ST_EXITING + 1) {
                    // we were the last one, so wake up the runner
                    LockSupport.unpark(shutdownThread);
                }
                if (interruptible) {
                    interruptibleThreads.remove(Thread.currentThread());
                }
            }
        }
    }
}
