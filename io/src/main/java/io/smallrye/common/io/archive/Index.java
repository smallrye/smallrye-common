package io.smallrye.common.io.archive;

import static io.smallrye.common.io.archive.Constants.*;

/**
 * A fully sorted index of all the entries of an archive.
 * Indexes are used to perform fast binary search lookups on entry names.
 */
abstract class Index {
    /**
     * Constructor for use by subclasses.
     */
    Index() {
    }

    /**
     * {@return the number of entries}
     */
    abstract long entries();

    /**
     * {@return the central directory entry offset for a given entry}
     *
     * @param index the file index
     */
    abstract long cdeOffset(long index);

    /**
     * Compare the names of two central directory entries.
     *
     * @param data the archive data
     * @param e0 the offset of the first central directory entry
     * @param e1 the offset of the second central directory entry
     * @return a negative integer, zero, or a positive integer as the first entry name is lexicographically
     *         less than, equal to, or greater than the second entry name
     */
    private static int compareName(final ArchiveData data, final long e0, final long e1) {
        long fn0 = data.cdeFileNameStart(e0);
        int len0 = data.cdeFileNameLength(e0);
        long fn1 = data.cdeFileNameStart(e1);
        int len1 = data.cdeFileNameLength(e1);
        if (data.cdeIsUtf8(e0)) {
            if (data.cdeIsUtf8(e1)) {
                return data.compareUtf8ToUtf8(fn0, len0, fn1, len1);
            } else {
                return data.compareUtf8ToCp437(fn0, len0, fn1, len1);
            }
        } else {
            if (data.cdeIsUtf8(e1)) {
                return data.compareCp437ToUtf8(fn0, len0, fn1, len1);
            } else {
                return data.compareCp437ToCp437(fn0, len0, fn1, len1);
            }
        }
    }

    /**
     * Create an index of the given archive data.
     *
     * @param data the archive data
     * @param eocd the end-of-central-directory offset
     * @param zip64eocd the ZIP64 end-of-central-directory offset, or -1 if none
     * @param cd the start offset of the central directory
     * @return a fully sorted index of the archive
     */
    static Index of(ArchiveData data, long eocd, long zip64eocd, long cd) {
        long count = data.eocdTotalEntryCount(eocd);
        if (count == 0xffff && zip64eocd != -1) {
            count = data.zip64eocdCdTotalEntryCount(zip64eocd);
        }
        if (count > Integer.MAX_VALUE - 8) {
            throw new UnsupportedOperationException("Huge indexes not yet supported");
        }
        long cdSize = data.eocdDirectorySize(eocd);
        if (cdSize == 0xffff_ffffL && zip64eocd != -1) {
            cdSize = data.zip64eocdDirectorySize(zip64eocd);
        }
        return new NormalIndex(data, cd, count);
    }

    /**
     * An index for archives with 2.14 billion or fewer entries.
     */
    static final class NormalIndex extends Index {
        /**
         * The sorted array of central directory entry offsets.
         */
        private final long[] table;

        /**
         * Construct a new NormalIndex.
         *
         * @param data the archive data containing the files
         * @param cd the start offset of the central directory
         * @param count the total number of entries
         */
        NormalIndex(ArchiveData data, long cd, long count) {
            long[] table = new long[(int) count];
            long pos = cd;
            for (int i = 0; i < count; i++) {
                if (data.cdeHeaderSignature(pos) != SIG_CDE) {
                    throw new IllegalArgumentException(
                            "Invalid archive (bad central directory entry signature at offset " + pos + ")");
                }
                table[i] = pos;
                pos += data.cdeEntrySize(pos);
            }
            this.table = mergeSort(data, table, new long[(int) count]);
        }

        /**
         * Sort the index table using an adaptive bottom-up merge sort.
         * <p>
         * Theory of operation: Performs a standard non-recursive bottom-up merge sort.
         * Optimizes for mostly-sorted or fully-sorted JARs by checking if the adjacent left
         * and right runs are already in sorted order, copying them directly to the scratch array
         * via {@link System#arraycopy} if so, bypassing element-by-element comparisons.
         *
         * @param data the archive data containing the files
         * @param table the index table to sort
         * @param temp the scratch/temporary working table
         * @return the sorted index table (which is either table or temp)
         */
        private static long[] mergeSort(ArchiveData data, long[] table, long[] temp) {
            for (int width = 1; width < table.length; width <<= 1) {
                for (int i = 0; i < table.length; i += width << 1) {
                    int left = i;
                    int right = Math.min(i + width, table.length);
                    int end = Math.min(i + (width << 1), table.length);
                    if (right >= end || compareName(data, table[right - 1], table[right]) <= 0) {
                        System.arraycopy(table, left, temp, left, end - left);
                    } else {
                        bottomUpMerge(data, table, left, right, end, temp);
                    }
                }
                // swap table and temp
                long[] t = table;
                table = temp;
                temp = t;
            }
            return table;
        }

        /**
         * Merge two adjacent sorted runs in the index table.
         *
         * @param data the archive data containing the files
         * @param table the index table being sorted
         * @param left the left start index of the left run
         * @param right the right start index of the right run
         * @param end the end index of the merge region
         * @param temp the scratch/temporary working table
         */
        private static void bottomUpMerge(ArchiveData data, long[] table, int left, int right, int end, long[] temp) {
            int i = left, j = right;
            for (int k = left; k < end; k++) {
                if (i < right && (j >= end || compareName(data, table[i], table[j]) <= 0)) {
                    temp[k] = table[i];
                    i++;
                } else {
                    temp[k] = table[j];
                    j++;
                }
            }
        }

        /**
         * {@return the number of entries}
         */
        long entries() {
            return table.length;
        }

        /**
         * {@return the central directory entry offset for a given entry}
         *
         * @param index the file index
         */
        long cdeOffset(final long index) {
            return table[(int) index];
        }
    }
}
