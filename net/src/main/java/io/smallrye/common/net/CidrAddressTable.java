package io.smallrye.common.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.common.constraint.Assert;

/**
 * A table for mapping IP addresses to objects using {@link CidrAddress} instances for matching.
 *
 * @param <T> the value type
 */
public final class CidrAddressTable<T> implements Iterable<CidrAddressTable.Mapping<T>> {

    @SuppressWarnings("rawtypes")
    private static final Mapping[] NO_MAPPINGS = new Mapping[0];

    private final AtomicReference<Mapping<T>[]> mappingsRef;

    /**
     * Construct a new instance.
     */
    public CidrAddressTable() {
        mappingsRef = new AtomicReference<>(empty());
    }

    private CidrAddressTable(Mapping<T>[] mappings) {
        mappingsRef = new AtomicReference<>(mappings);
    }

    /**
     * {@return the value which best matches the given address, or {@code defVal} if none match}
     *
     * @param address the address to match (must not be {@code null})
     * @param defVal the default value to return
     */
    public T getOrDefault(InetAddress address, T defVal) {
        Assert.checkNotNullParam("address", address);
        final Mapping<T> mapping = doGet(mappingsRef.get(), address.getAddress(), address instanceof Inet4Address ? 32 : 128,
                Inet.getScopeId(address));
        return mapping == null ? defVal : mapping.value;
    }

    /**
     * {@return the value which best matches the given address, or {@code null} if none match}
     *
     * @param address the address to match (must not be {@code null})
     */
    public T get(InetAddress address) {
        return getOrDefault(address, null);
    }

    /**
     * Add a mapping for the given block.
     *
     * @param block the address block (must not be {@code null})
     * @param value the value for the mapping (must not be {@code null})
     * @return the previous mapping, if any, or else {@code null}
     */
    public T put(CidrAddress block, T value) {
        Assert.checkNotNullParam("block", block);
        Assert.checkNotNullParam("value", value);
        return doPut(block, null, value, true, true);
    }

    /**
     * Add a mapping for the given block if one is not already present.
     *
     * @param block the address block (must not be {@code null})
     * @param value the value for the mapping (must not be {@code null})
     * @return the existing mapping, if any, or else {@code null} if the addition succeeded
     */
    public T putIfAbsent(CidrAddress block, T value) {
        Assert.checkNotNullParam("block", block);
        Assert.checkNotNullParam("value", value);
        return doPut(block, null, value, true, false);
    }

    /**
     * Replace a mapping for the given block if one is already present.
     *
     * @param block the address block (must not be {@code null})
     * @param value the new value for the mapping (must not be {@code null})
     * @return the previous mapping, if any, or else {@code null} if the value was not replaced
     */
    public T replaceExact(CidrAddress block, T value) {
        Assert.checkNotNullParam("block", block);
        Assert.checkNotNullParam("value", value);
        return doPut(block, null, value, false, true);
    }

    /**
     * Replace a mapping for the given block if one is already present with the given expected value.
     *
     * @param block the address block (must not be {@code null})
     * @param expect the expected value for the mapping (must not be {@code null})
     * @param update the new value for the mapping (must not be {@code null})
     * @return {@code true} if the mapping was replaced, or {@code false} if it was not replaced
     */
    public boolean replaceExact(CidrAddress block, T expect, T update) {
        Assert.checkNotNullParam("block", block);
        Assert.checkNotNullParam("expect", expect);
        Assert.checkNotNullParam("update", update);
        return doPut(block, expect, update, false, true) == expect;
    }

    /**
     * Remove a mapping for a specific address block.
     *
     * @param block the address block (must not be {@code null})
     * @return the removed mapping value, if any, or else {@code null} if the value was not removed
     */
    public T removeExact(CidrAddress block) {
        Assert.checkNotNullParam("block", block);
        return doPut(block, null, null, false, true);
    }

    /**
     * Remove a mapping for a specific address block if its value is the expected value.
     *
     * @param block the address block (must not be {@code null})
     * @param expect the expected value
     * @return {@code true} if the mapping was removed, or {@code false} if it was not removed
     */
    public boolean removeExact(CidrAddress block, T expect) {
        Assert.checkNotNullParam("block", block);
        return doPut(block, expect, null, false, true) == expect;
    }

    private T doPut(final CidrAddress block, final T expect, final T update, final boolean putIfAbsent,
            final boolean putIfPresent) {
        assert putIfAbsent || putIfPresent;
        final AtomicReference<Mapping<T>[]> mappingsRef = this.mappingsRef;
        final byte[] bytes = block.getNetworkAddress().getAddress();
        Mapping<T>[] oldVal, newVal;
        int idx;
        T existing;
        boolean matchesExpected;
        do {
            oldVal = mappingsRef.get();
            idx = doFind(oldVal, bytes, block.getNetmaskBits(), block.getScopeId());
            if (idx < 0) {
                if (!putIfAbsent) {
                    return null;
                }
                existing = null;
            } else {
                existing = oldVal[idx].value;
            }
            if (expect != null) {
                matchesExpected = Objects.equals(expect, existing);
                if (!matchesExpected) {
                    return existing;
                }
            } else {
                matchesExpected = false;
            }
            if (idx >= 0 && !putIfPresent) {
                return existing;
            }
            // now construct the new mapping
            final int oldLen = oldVal.length;
            if (update == null) {
                assert idx >= 0;
                // removal
                if (oldLen == 1) {
                    newVal = empty();
                } else {
                    final Mapping<T> removing = oldVal[idx];
                    newVal = Arrays.copyOf(oldVal, oldLen - 1);
                    System.arraycopy(oldVal, idx + 1, newVal, idx, oldLen - idx - 1);
                    // now reparent any children that I was a parent of with my old parent
                    for (int i = 0; i < oldLen - 1; i++) {
                        if (newVal[i].parent == removing) {
                            newVal[i] = newVal[i].withNewParent(removing.parent);
                        }
                    }
                }
            } else if (idx >= 0) {
                // replace
                newVal = oldVal.clone();
                final Mapping<T> oldMapping = oldVal[idx];
                final Mapping<T> newMapping = new Mapping<>(block, update, oldVal[idx].parent);
                newVal[idx] = newMapping;
                // now reparent any child to me
                for (int i = 0; i < oldLen; i++) {
                    if (i != idx && newVal[i].parent == oldMapping) {
                        newVal[i] = newVal[i].withNewParent(newMapping);
                    }
                }
            } else {
                // add
                newVal = Arrays.copyOf(oldVal, oldLen + 1);
                final Mapping<T> newMappingParent = doGet(oldVal, bytes, block.getNetmaskBits(), block.getScopeId());
                final Mapping<T> newMapping = new Mapping<>(block, update, newMappingParent);
                newVal[-idx - 1] = newMapping;
                System.arraycopy(oldVal, -idx - 1, newVal, -idx, oldLen + idx + 1);
                // now reparent any children who have a parent of my (possibly null) parent but match me
                for (int i = 0; i <= oldLen; i++) {
                    if (newVal[i] != newMapping && newVal[i].parent == newMappingParent && block.matches(newVal[i].range)) {
                        newVal[i] = newVal[i].withNewParent(newMapping);
                    }
                }
            }
        } while (!mappingsRef.compareAndSet(oldVal, newVal));
        return matchesExpected ? expect : existing;
    }

    @SuppressWarnings("unchecked")
    private static <T> Mapping<T>[] empty() {
        return NO_MAPPINGS;
    }

    /**
     * Remove all mappings from this table.
     */
    public void clear() {
        mappingsRef.set(empty());
    }

    /**
     * {@return the number of mappings in this table}
     */
    public int size() {
        return mappingsRef.get().length;
    }

    /**
     * {@return {@code true} if the table is empty, or {@code false} otherwise}
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * {@return an atomic clone of this table}
     */
    public CidrAddressTable<T> clone() {
        return new CidrAddressTable<>(mappingsRef.get());
    }

    public Iterator<Mapping<T>> iterator() {
        final Mapping<T>[] mappings = mappingsRef.get();
        return new Iterator<Mapping<T>>() {
            int idx;

            public boolean hasNext() {
                return idx < mappings.length;
            }

            public Mapping<T> next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return mappings[idx++];
            }
        };
    }

    public Spliterator<Mapping<T>> spliterator() {
        final Mapping<T>[] mappings = mappingsRef.get();
        return Spliterators.spliterator(mappings, Spliterator.IMMUTABLE | Spliterator.ORDERED);
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        final Mapping<T>[] mappings = mappingsRef.get();
        b.append(mappings.length).append(" mappings");
        for (final Mapping<T> mapping : mappings) {
            b.append(System.lineSeparator()).append('\t').append(mapping.range);
            if (mapping.parent != null) {
                b.append(" (parent ").append(mapping.parent.range).append(')');
            }
            b.append(" -> ").append(mapping.value);
        }
        return b.toString();
    }

    private int doFind(Mapping<T>[] table, byte[] bytes, int maskBits, final int scopeId) {
        int low = 0;
        int high = table.length - 1;

        while (low <= high) {
            // bisect the range
            int mid = low + high >>> 1;

            // compare the mapping at this location
            Mapping<T> mapping = table[mid];
            int cmp = mapping.range.compareAddressBytesTo(bytes, maskBits, scopeId);

            if (cmp < 0) {
                // move to the latter half
                low = mid + 1;
            } else if (cmp > 0) {
                // move to the former half
                high = mid - 1;
            } else {
                // exact match is the best case
                return mid;
            }
        }
        // return the point we would insert at (plus one, negated)
        return -(low + 1);
    }

    private Mapping<T> doGet(Mapping<T>[] table, byte[] bytes, final int netmaskBits, final int scopeId) {
        int idx = doFind(table, bytes, netmaskBits, scopeId);
        if (idx >= 0) {
            // exact match
            assert table[idx].range.matches(bytes, scopeId);
            return table[idx];
        }
        // check immediate predecessor if there is one
        int pre = -idx - 2;
        if (pre >= 0) {
            if (table[pre].range.matches(bytes, scopeId)) {
                return table[pre];
            }
            // try parent
            Mapping<T> parent = table[pre].parent;
            while (parent != null) {
                if (parent.range.matches(bytes, scopeId)) {
                    return parent;
                }
                parent = parent.parent;
            }
        }
        return null;
    }

    /**
     * A single mapping in the table.
     *
     * @param <T> the value type
     */
    public static final class Mapping<T> {
        final CidrAddress range;
        final T value;
        final Mapping<T> parent;

        Mapping(final CidrAddress range, final T value, final Mapping<T> parent) {
            this.range = range;
            this.value = value;
            this.parent = parent;
        }

        Mapping<T> withNewParent(Mapping<T> newParent) {
            return new Mapping<T>(range, value, newParent);
        }

        /**
         * Get the address range of this entry.
         *
         * @return the address range of this entry (not {@code null})
         */
        public CidrAddress getRange() {
            return range;
        }

        /**
         * Get the stored value of this entry.
         *
         * @return the stored value of this entry
         */
        public T getValue() {
            return value;
        }

        /**
         * Get the parent of this entry, if any.
         *
         * @return the parent of this entry, or {@code null} if there is no parent
         */
        public Mapping<T> getParent() {
            return parent;
        }
    }
}
