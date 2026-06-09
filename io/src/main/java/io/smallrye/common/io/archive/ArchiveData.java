package io.smallrye.common.io.archive;

import static io.smallrye.common.io.archive.Constants.*;

import java.io.IOException;
import java.nio.ByteBuffer;

abstract class ArchiveData {
    ArchiveData() {
    }

    // low level accessors

    // Local File Header

    int lfhSignature(long lfhOffset) {
        return s32le(lfhOffset + LH_SIGNATURE);
    }

    int lfhVersionNeeded(long lfhOffset) {
        return u16le(lfhOffset + LH_MIN_VERSION);
    }

    int lfhGeneralBits(long lfhOffset) {
        return u16le(lfhOffset + LH_GP_BITS);
    }

    int lfhMethod(long lfhOffset) {
        return u16le(lfhOffset + LH_COMP_METHOD);
    }

    int lfhModTime(long lfhOffset) {
        return u16le(lfhOffset + LH_MOD_TIME);
    }

    int lfhModDate(long lfhOffset) {
        return u16le(lfhOffset + LH_MOD_DATE);
    }

    int lfhCrc32(long lfhOffset) {
        return s32le(lfhOffset + LH_CRC_32);
    }

    long lfhCompressedSize(long lfhOffset) {
        return u32le(lfhOffset + LH_COMPRESSED_SIZE);
    }

    long lfhUncompressedSize(long lfhOffset) {
        return u32le(lfhOffset + LH_UNCOMPRESSED_SIZE);
    }

    int lfhFileNameLength(long lfhOffset) {
        return u16le(lfhOffset + LH_FILE_NAME_LENGTH);
    }

    int lfhExtraFieldLength(long lfhOffset) {
        return u16le(lfhOffset + LH_EXTRA_LENGTH);
    }

    int lfhEntrySize(long lfhOffset) {
        return LH_END + lfhFileNameLength(lfhOffset) + lfhExtraFieldLength(lfhOffset);
    }

    // Data descriptor (exists if bit 3 of GP bits is set)

    int ddCrc32(long ddOffset) {
        return s32le(ddOffset + DD_CRC_32);
    }

    long ddCompressedSize(long ddOffset) {
        return u32le(ddOffset + DD_COMPRESSED_SIZE);
    }

    long ddUncompressedSize(long ddOffset) {
        return u32le(ddOffset + DD_UNCOMPRESSED_SIZE);
    }

    // Central Directory Entries

    int cdeHeaderSignature(long cdeOffset) {
        return s32le(cdeOffset + CDE_SIGNATURE);
    }

    int cdeVersionMadeBy(long cdeOffset) {
        return u16le(cdeOffset + CDE_VERSION_MADE_BY);
    }

    int cdeVersionNeeded(long cdeOffset) {
        return u16le(cdeOffset + CDE_VERSION_NEEDED);
    }

    int cdeGeneralBits(long cdeOffset) {
        return u16le(cdeOffset + CDE_GP_BITS);
    }

    boolean cdeIsUtf8(long cdeOffset) {
        return (cdeGeneralBits(cdeOffset) & GP_UTF_8) != 0;
    }

    int cdeMethod(long cdeOffset) {
        return u16le(cdeOffset + CDE_COMP_METHOD);
    }

    int cdeModTime(long cdeOffset) {
        return u16le(cdeOffset + CDE_MOD_TIME);
    }

    int cdeModDate(long cdeOffset) {
        return u16le(cdeOffset + CDE_MOD_DATE);
    }

    int cdeCrc32(long cdeOffset) {
        return s32le(cdeOffset + CDE_CRC_32);
    }

    long cdeCompressedSize(long cdeOffset) {
        return u32le(cdeOffset + CDE_COMPRESSED_SIZE);
    }

    long cdeCompressedSize(long cdeOffset, long zip64) {
        long size = cdeCompressedSize(cdeOffset);
        return size == 0xffff_ffffL && zip64 != -1 ? zip64CompressedSize(zip64) : size;
    }

    long cdeUncompressedSize(long cdeOffset) {
        return u32le(cdeOffset + CDE_UNCOMPRESSED_SIZE);
    }

    long cdeUncompressedSize(long cdeOffset, long zip64) {
        long size = cdeUncompressedSize(cdeOffset);
        return size == 0xffff_ffffL && zip64 != -1 ? zip64UncompressedSize(zip64) : size;
    }

    int cdeFileNameLength(long cdeOffset) {
        return u16le(cdeOffset + CDE_FILE_NAME_LENGTH);
    }

    int cdeExtraFieldLength(long cdeOffset) {
        return u16le(cdeOffset + CDE_EXTRA_LENGTH);
    }

    int cdeFileCommentLength(long cdeOffset) {
        return u16le(cdeOffset + CDE_COMMENT_LENGTH);
    }

    int cdeDiskNumberStart(long cdeOffset) {
        return u16le(cdeOffset + CDE_FIRST_DISK_NUMBER);
    }

    int cdeInternalAttributes(long cdeOffset) {
        return u16le(cdeOffset + CDE_INTERNAL_ATTRIBUTES);
    }

    int cdeExternalAttributes(long cdeOffset) {
        return s32le(cdeOffset + CDE_EXTERNAL_ATTRIBUTES);
    }

    long cdeLocalHeaderRelativeOffset(long cdeOffset) {
        return u32le(cdeOffset + CDE_LOCAL_HEADER_OFFSET);
    }

    long cdeLocalHeaderRelativeOffset(long cdeOffset, long zip64) {
        long offs = cdeLocalHeaderRelativeOffset(cdeOffset);
        return offs == 0xFFFF_FFFFL && zip64 != -1 ? zip64LocalRelativeHeaderOffset(zip64) : offs;
    }

    int cdeEntrySize(long cdeOffset) {
        return CDE_END + cdeFileNameLength(cdeOffset) + cdeExtraFieldLength(cdeOffset) + cdeFileCommentLength(cdeOffset);
    }

    long cdeZip64(long cdeOffset) {
        long pos = cdeExtended(cdeOffset, EXT_ID_ZIP64);
        if (pos != -1 && u16le(pos + 2) >= 8) {
            return pos;
        }
        // not found or too small
        return -1;
    }

    long cdeExtended(long cdeOffset, int id) {
        long pos = cdeOffset + CDE_END + cdeFileNameLength(cdeOffset);
        long end = pos + cdeExtraFieldLength(cdeOffset);
        // test this location
        while (pos < end - 4) {
            if (id == u16le(pos)) {
                return pos;
            }
            pos += u16le(pos + 2) + 4;
        }
        return -1;
    }

    // end of central directory record

    int eocdSignature(long eocdOffset) {
        return s32le(eocdOffset + EOCD_SIGNATURE);
    }

    int eocdDiskNumber(long eocdOffset) {
        return u16le(eocdOffset + EOCD_DISK_NUMBER);
    }

    int eocdCdDiskNumber(long eocdOffset) {
        return u16le(eocdOffset + EOCD_CD_FIRST_DISK_NUMBER);
    }

    int eocdThisDiskEntryCount(long eocdOffset) {
        return u16le(eocdOffset + EOCD_CDE_COUNT_THIS_DISK);
    }

    int eocdTotalEntryCount(long eocdOffset) {
        return u16le(eocdOffset + EOCD_CDE_COUNT_ALL);
    }

    long eocdDirectorySize(long eocdOffset) {
        return u32le(eocdOffset + EOCD_CD_SIZE);
    }

    long eocdDirectoryRelativeOffset(long eocdOffset) {
        return u32le(eocdOffset + EOCD_CD_START_OFFSET);
    }

    int eocdCommentLength(long eocdOffset) {
        return u16le(eocdOffset + EOCD_COMMENT_LENGTH);
    }

    // zip64 end of central directory locator

    int zip64eocdlSignature(long eocdlOffset) {
        return s32le(eocdlOffset + EOCDL_ZIP64_SIGNATURE);
    }

    int zip64eocdlCdDiskNumber(long eocdlOffset) {
        return s32le(eocdlOffset + EOCDL_ZIP64_EOCD_DISK_NUMBER);
    }

    long zip64eocdlEocdRelativeOffset(long eocdlOffset) {
        return s64le(eocdlOffset + EOCDL_ZIP64_EOCD_OFFSET);
    }

    int zip64eocdlDiskCount(long eocdlOffset) {
        return s32le(eocdlOffset + EOCDL_ZIP64_DISK_COUNT);
    }

    // zip64 end of central directory

    int zip64eocdSignature(long eocdOffset) {
        return s32le(eocdOffset + EOCD_ZIP64_SIGNATURE);
    }

    /**
     * {@return the size of the zip64 eocd record, including the signature and size fields}
     */
    long zip64eocdRecordSize(long eocdOffset) {
        return s64le(eocdOffset + EOCD_ZIP64_SIZE) + 12;
    }

    int zip64eocdVersionMadeBy(long eocdOffset) {
        return u16le(eocdOffset + EOCD_ZIP64_VERSION_MADE_BY);
    }

    int zip64eocdVersionNeeded(long eocdOffset) {
        return u16le(eocdOffset + EOCD_ZIP64_VERSION_NEEDED);
    }

    int zip64eocdDiskNumber(long eocdOffset) {
        return s32le(eocdOffset + EOCD_ZIP64_DISK_NUMBER);
    }

    int zip64eocdCdDiskNumber(long eocdOffset) {
        return s32le(eocdOffset + EOCD_ZIP64_CD_FIRST_DISK_NUMBER);
    }

    long zip64eocdCdThisDiskEntryCount(long eocdOffset) {
        return s64le(eocdOffset + EOCD_ZIP64_CDE_COUNT_THIS_DISK);
    }

    long zip64eocdCdTotalEntryCount(long eocdOffset) {
        return s64le(eocdOffset + EOCD_ZIP64_CDE_COUNT_ALL);
    }

    long zip64eocdDirectorySize(long eocdOffset) {
        return s64le(eocdOffset + EOCD_ZIP64_CD_SIZE);
    }

    long zip64eocdDirectoryRelativeOffset(long eocdOffset) {
        return s64le(eocdOffset + EOCD_ZIP64_CD_START_OFFSET);
    }

    // zip64

    long zip64UncompressedSize(long zip64) {
        return s64le(zip64 + ZIP64_UNCOMPRESSED_SIZE);
    }

    long zip64CompressedSize(long zip64) {
        return s64le(zip64 + ZIP64_COMPRESSED_SIZE);
    }

    long zip64LocalRelativeHeaderOffset(long zip64) {
        return s64le(zip64 + ZIP64_LOCAL_HEADER_OFFSET);
    }

    // backend-specific memory-access methods

    protected abstract byte s8(long offset);

    protected int u8(long offset) {
        return Byte.toUnsignedInt(s8(offset));
    }

    protected abstract short s16le(long offset);

    protected short s16be(final long offset) {
        return Short.reverseBytes(s16le(offset));
    }

    protected int u16le(long offset) {
        return Short.toUnsignedInt(s16le(offset));
    }

    protected int u16be(long offset) {
        return Short.toUnsignedInt(s16be(offset));
    }

    protected abstract int s32le(long offset);

    protected int s32be(final long offset) {
        return Integer.reverseBytes(s32le(offset));
    }

    protected long u32le(long offset) {
        return Integer.toUnsignedLong(s32le(offset));
    }

    protected long u32be(long offset) {
        return Integer.toUnsignedLong(s32be(offset));
    }

    protected abstract long s64le(long offset);

    protected long s64be(final long offset) {
        return Long.reverseBytes(s64le(offset));
    }

    protected abstract ByteBuffer buffer(long offset, int size);

    protected abstract long size();

    long findRecord(final int signature, final long start, final long end) {
        for (long pos = end - 4; pos >= start; pos--) {
            if (s32le(pos) == signature) {
                return pos;
            }
        }
        return -1;
    }

    int compareUtf8ToUtf8(long off1, int len1, long off2, int len2) {
        for (;;) {
            if (len1 == 0) {
                if (len2 == 0) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (len2 == 0) {
                return 1;
            }
            int cp1 = utf8(off1);
            int cp2 = utf8(off2);
            int cmp = Integer.compare(cp1, cp2);
            if (cmp != 0) {
                return cmp;
            }
            int sz1 = utf8Size(off1);
            off1 += sz1;
            len1 -= sz1;
            int sz2 = utf8Size(off2);
            off2 += sz2;
            len2 -= sz2;
        }
    }

    int compareUtf8ToString(long off1, int len1, String str2, int skip) {
        int off2 = 0;
        int len2 = str2.length();
        for (;;) {
            if (len1 == 0) {
                if (len2 == 0) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (len2 == 0) {
                return 1;
            }
            if (skip > 0) {
                int cp1 = utf8(off1);
                int sz1 = utf8Size(off1);
                off1 += sz1;
                len1 -= sz1;
                skip -= Character.charCount(cp1);
            } else {
                int cp1 = utf8(off1);
                int cp2 = str2.codePointAt(off2);
                int cmp = Integer.compare(cp1, cp2);
                if (cmp != 0) {
                    return cmp;
                }
                int sz1 = utf8Size(off1);
                off1 += sz1;
                len1 -= sz1;
                int sz2 = Character.charCount(cp2);
                off2 += sz2;
                len2 -= sz2;
            }
        }
    }

    int compareUtf8ToCp437(long off1, int len1, long off2, int len2) {
        for (;;) {
            if (len1 == 0) {
                if (len2 == 0) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (len2 == 0) {
                return 1;
            }
            int cp1 = utf8(off1);
            int cp2 = cp437(off2);
            int cmp = Integer.compare(cp1, cp2);
            if (cmp != 0) {
                return cmp;
            }
            int sz1 = utf8Size(off1);
            off1 += sz1;
            len1 -= sz1;
            off2++;
            len2--;
        }
    }

    int compareCp437ToUtf8(long off1, int len1, long off2, int len2) {
        return -compareUtf8ToCp437(off2, len2, off1, len1);
    }

    int compareCp437ToString(long off1, int len1, String str2) {
        int off2 = 0;
        int len2 = str2.length();
        for (;;) {
            if (len1 == 0) {
                if (len2 == 0) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (len2 == 0) {
                return 1;
            }
            int cp1 = cp437(off1);
            int cp2 = str2.codePointAt(off2);
            int cmp = Integer.compare(cp1, cp2);
            if (cmp != 0) {
                return cmp;
            }
            off1++;
            len1--;
            int sz2 = Character.charCount(cp2);
            off2 += sz2;
            len2 -= sz2;
        }
    }

    int compareCp437ToCp437(long off1, int len1, long off2, int len2) {
        for (;;) {
            if (len1 == 0) {
                if (len2 == 0) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (len2 == 0) {
                return 1;
            }
            int cp1 = cp437(off1);
            int cp2 = cp437(off2);
            int cmp = Integer.compare(cp1, cp2);
            if (cmp != 0) {
                return cmp;
            }
            off1++;
            len1--;
            off2++;
            len2--;
        }
    }

    boolean utf8StartsWithUtf8(long off, int len, long prefixOff, int prefixLen) {
        for (;;) {
            if (prefixLen == 0) {
                return true;
            } else if (len == 0) {
                return false;
            }
            int cp1 = utf8(off);
            int cp2 = utf8(prefixOff);
            if (cp1 != cp2) {
                return false;
            }
            int sz1 = utf8Size(off);
            off += sz1;
            len -= sz1;
            int sz2 = utf8Size(prefixOff);
            prefixOff += sz2;
            prefixLen -= sz2;
        }
    }

    boolean utf8StartsWithCp437(long off, int len, long prefixOff, int prefixLen) {
        for (;;) {
            if (prefixLen == 0) {
                return true;
            } else if (len == 0) {
                return false;
            }
            int cp1 = utf8(off);
            int cp2 = cp437(prefixOff);
            if (cp1 != cp2) {
                return false;
            }
            int sz1 = utf8Size(off);
            off += sz1;
            len -= sz1;
            prefixOff++;
            prefixLen--;
        }
    }

    boolean utf8StartsWithString(long off, int len, String prefix) {
        int prefixOff = 0;
        int prefixLen = prefix.length();
        for (;;) {
            if (prefixLen == 0) {
                return true;
            } else if (len == 0) {
                return false;
            }
            int cp1 = utf8(off);
            int cp2 = prefix.codePointAt(prefixOff);
            if (cp1 != cp2) {
                return false;
            }
            int sz1 = utf8Size(off);
            off += sz1;
            len -= sz1;
            int sz2 = Character.charCount(cp2);
            prefixOff += sz2;
            prefixLen -= sz2;
        }
    }

    boolean cp437StartsWithUtf8(long off, int len, long prefixOff, int prefixLen) {
        for (;;) {
            if (prefixLen == 0) {
                return true;
            } else if (len == 0) {
                return false;
            }
            int cp1 = cp437(off);
            int cp2 = utf8(prefixOff);
            if (cp1 != cp2) {
                return false;
            }
            off++;
            len--;
            int sz2 = utf8Size(prefixOff);
            prefixOff += sz2;
            prefixLen -= sz2;
        }
    }

    boolean cp437StartsWithCp437(long off, int len, long prefixOff, int prefixLen) {
        for (;;) {
            if (prefixLen == 0) {
                return true;
            } else if (len == 0) {
                return false;
            }
            int cp1 = cp437(off);
            int cp2 = cp437(prefixOff);
            if (cp1 != cp2) {
                return false;
            }
            off++;
            len--;
            prefixOff++;
            prefixLen--;
        }
    }

    boolean cp437StartsWithString(long off, int len, String prefix) {
        int prefixOff = 0;
        int prefixLen = prefix.length();
        for (;;) {
            if (prefixLen == 0) {
                return true;
            } else if (len == 0) {
                return false;
            }
            int cp1 = cp437(off);
            int cp2 = prefix.codePointAt(prefixOff);
            if (cp1 != cp2) {
                return false;
            }
            off++;
            len--;
            int sz2 = Character.charCount(cp2);
            prefixOff += sz2;
            prefixLen -= sz2;
        }
    }

    String utf8ToString(long off, int len) {
        if (len == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(len);
        do {
            int sz = utf8Size(off);
            b.appendCodePoint(utf8(off));
            off += sz;
            len -= sz;
        } while (len > 0);
        return b.toString();
    }

    String cp437ToString(long off, int len) {
        if (len == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(len);
        while (len > 0) {
            b.appendCodePoint(cp437(off));
            off++;
            len--;
        }
        return b.toString();
    }

    abstract void get(final long base, final byte[] dest, final int off, final int len);

    private int cp437(final long off) {
        // Use a string constant for fast loading
        return "\0☺☻♥♦♣♠•◘○◙♂♀♪♫☼►◄↕‼¶§▬↨↑↓→←∟↔▲▼ !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~⌂ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒáíóúñÑªº¿⌐¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αßΓπΣσµτΦΘΩδ∞φε∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■\u00A0"
                .charAt(u8(off));
    }

    private int utf8(final long off) {
        int a = u8(off);
        if (a < 0x80) {
            return a;
        }
        if (a < 0xc0 || 0xf8 <= a) {
            // invalid
            return '�';
        }
        // at least 2 bytes
        int b = u8(off + 1);
        if (b < 0x80 || 0xc0 <= b) {
            // invalid
            return '�';
        }
        if (a < 0xe0) {
            // exactly 2 bytes
            return (a & 0x1f) << 6 | b & 0x3f;
        }
        // at least 3 bytes
        int c = u8(off + 2);
        if (c < 0x80 || 0xc0 <= c) {
            // invalid
            return '�';
        }
        if (a < 0xf0) {
            // exactly 3 bytes
            return (a & 0xf) << 12 | (b & 0x3f) << 6 | c & 0x3f;
        }
        // 4 bytes
        int d = u8(off + 3);
        if (d < 0x80 || 0xc0 <= d) {
            // invalid
            return '�';
        }
        return (a & 0x7) << 18 | (b & 0x3f) << 12 | (c & 0x3f) << 6 | d & 0x3f;
    }

    private int utf8Size(final long off) {
        int a = u8(off);
        if (a < 0xc0) {
            return 1;
        }
        if (a < 0xe0) {
            return 2;
        }
        if (a < 0xf0) {
            return 3;
        }
        if (a < 0xf8) {
            return 4;
        }
        // invalid
        return 1;
    }

    abstract void release();

    void close() throws IOException {
    }
}
