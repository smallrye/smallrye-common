package io.smallrye.common.io.archive;

import static io.smallrye.common.io.archive.Constants.*;

/**
 * A fully sorted index of all the entries of an archive.
 * Indexes can be normal (up to 2B entries) or huge (more than 2B entries),
 * and the archive size can be small (64k or less), normal (4GB or less), or huge (more than 4GB).
 * Each table entry value is a central directory offset.
 */
abstract class Index {
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
        Index index;
        if (data.size() < 0x1_0000L) {
            index = new NormalSmallIndex(data, cd, count);
        } else if (data.size() < 0x1_0000_0000L) {
            index = new NormalNormalIndex(data, cd, count);
        } else {
            index = new NormalHugeIndex(data, cd, count);
        }
        return index;
    }

    static final class NormalSmallIndex extends Index {
        private final short[] table;

        NormalSmallIndex(ArchiveData data, long cd, long count) {
            short[] table = new short[(int) count];
            long pos = cd;
            for (int i = 0; i < count; i++) {
                if (data.cdeHeaderSignature(pos) != SIG_CDE) {
                    throw new IllegalArgumentException(
                            "Invalid archive (bad central directory entry signature at offset " + pos + ")");
                }
                table[i] = (short) pos;
                pos += data.cdeEntrySize(pos);
            }
            this.table = mergeSort(data, table, new short[(int) count]);
        }

        private static short[] mergeSort(ArchiveData data, short[] table, short[] temp) {
            for (int width = 1; width < table.length; width <<= 1) {
                for (int i = 0; i < table.length; i += width << 1) {
                    bottomUpMerge(data, table, i, Math.min(i + width, table.length), Math.min(i + (width << 1), table.length),
                            temp);
                }
                // swap table and temp
                short[] t = table;
                table = temp;
                temp = t;
            }
            return table;
        }

        private static void bottomUpMerge(ArchiveData data, short[] table, int left, int right, int end, short[] temp) {
            int i = left, j = right;
            for (int k = left; k < end; k++) {
                if (i < right && (j >= end
                        || compareName(data, Short.toUnsignedLong(table[i]), Short.toUnsignedLong(table[j])) <= 0)) {
                    temp[k] = table[i];
                    i++;
                } else {
                    temp[k] = table[j];
                    j++;
                }
            }
        }

        long entries() {
            return table.length;
        }

        long cdeOffset(final long index) {
            return Short.toUnsignedLong(table[(int) index]);
        }
    }

    static final class NormalNormalIndex extends Index {
        private final int[] table;

        NormalNormalIndex(ArchiveData data, long cd, long count) {
            int[] table = new int[(int) count];
            long pos = cd;
            for (int i = 0; i < count; i++) {
                if (data.cdeHeaderSignature(pos) != SIG_CDE) {
                    throw new IllegalArgumentException(
                            "Invalid archive (bad central directory entry signature at offset " + pos + ")");
                }
                table[i] = (int) pos;
                pos += data.cdeEntrySize(pos);
            }
            this.table = mergeSort(data, table, new int[(int) count]);
        }

        private static int[] mergeSort(ArchiveData data, int[] table, int[] temp) {
            for (int width = 1; width < table.length; width <<= 1) {
                for (int i = 0; i < table.length; i += width << 1) {
                    bottomUpMerge(data, table, i, Math.min(i + width, table.length), Math.min(i + (width << 1), table.length),
                            temp);
                }
                // swap table and temp
                int[] t = table;
                table = temp;
                temp = t;
            }
            return table;
        }

        private static void bottomUpMerge(ArchiveData data, int[] table, int left, int right, int end, int[] temp) {
            int i = left, j = right;
            for (int k = left; k < end; k++) {
                if (i < right && (j >= end
                        || compareName(data, Integer.toUnsignedLong(table[i]), Integer.toUnsignedLong(table[j])) <= 0)) {
                    temp[k] = table[i];
                    i++;
                } else {
                    temp[k] = table[j];
                    j++;
                }
            }
        }

        long entries() {
            return table.length;
        }

        long cdeOffset(final long index) {
            return Integer.toUnsignedLong(table[(int) index]);
        }
    }

    static final class NormalHugeIndex extends Index {
        private final long[] table;

        NormalHugeIndex(ArchiveData data, long cd, long count) {
            long[] table = new long[(int) count];
            long pos = cd;
            for (int i = 0; i < count; i++) {
                if (data.cdeHeaderSignature(pos) != SIG_CDE) {
                    throw new IllegalArgumentException(
                            "Invalid archive (bad central directory entry signature at offset " + pos + ")");
                }
                table[i] = (int) pos;
                pos += data.cdeEntrySize(pos);
            }
            this.table = mergeSort(data, table, new long[(int) count]);
        }

        private static long[] mergeSort(ArchiveData data, long[] table, long[] temp) {
            for (int width = 1; width < table.length; width <<= 1) {
                for (int i = 0; i < table.length; i += width << 1) {
                    bottomUpMerge(data, table, i, Math.min(i + width, table.length), Math.min(i + (width << 1), table.length),
                            temp);
                }
                // swap table and temp
                long[] t = table;
                table = temp;
                temp = t;
            }
            return table;
        }

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

        long entries() {
            return table.length;
        }

        long cdeOffset(final long index) {
            return table[(int) index];
        }
    }
}
