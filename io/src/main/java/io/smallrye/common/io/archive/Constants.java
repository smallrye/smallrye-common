package io.smallrye.common.io.archive;

final class Constants {
    private Constants() {
    }
    /*
     *
     * Directory layout:
     *
     * [ ] -> number of sorted entries
     *
     * File index node layout:
     *
     * [ ] -> CDE index
     * [ ] -> offset of name segment in CDE
     * [ ] -> 0, or offset of directory contents
     *
     *
     */

    static final int GP_ENCRYPTED = 1 << 0;

    // only if method == implode
    static final int GP_IMPLODE_8K_DICTIONARY = 1 << 1;
    static final int GP_IMPLODE_3_TREES = 1 << 2;

    // only if method == deflate
    static final int GP_DEFLATE_COMP_OPT_MASK = 0b11 << 1;

    static final int GP_DEFLATE_COMP_OPT_NORMAL = 0b00 << 1;
    static final int GP_DEFLATE_COMP_OPT_MAXIMUM = 0b01 << 1;
    static final int GP_DEFLATE_COMP_OPT_FAST = 0b10 << 1;
    static final int GP_DEFLATE_COMP_OPT_SUPER_FAST = 0b11 << 1;

    // only if method == lzma
    static final int GP_LZMA_EOS_USED = 1 << 1;
    // reserved 1 << 2
    static final int GP_LATE_SIZES = 1 << 3;
    // reserved 1 << 4
    static final int GP_COMPRESSED_PATCHED = 1 << 5;
    static final int GP_STRONG_ENCRYPTION = 1 << 6;
    // reserved 1 << 7
    // reserved 1 << 8
    // reserved 1 << 9
    // reserved 1 << 10
    static final int GP_UTF_8 = 1 << 11;
    // reserved 1 << 12
    static final int GP_CD_MASKED = 1 << 13;
    // reserved 1 << 14
    // reserved 1 << 15

    static final int METHOD_STORED = 0;
    static final int METHOD_SHRINK = 1;
    static final int METHOD_REDUCE_1 = 2;
    static final int METHOD_REDUCE_2 = 3;
    static final int METHOD_REDUCE_3 = 4;
    static final int METHOD_REDUCE_4 = 5;
    static final int METHOD_IMPLODE = 6;
    static final int METHOD_DEFLATE = 8;
    static final int METHOD_DEFLATE64 = 9;
    static final int METHOD_BZIP2 = 12;
    static final int METHOD_LZMA = 14;

    static final int MADE_BY_MS_DOS = 0;
    static final int MADE_BY_UNIX = 3;
    static final int MADE_BY_NTFS = 10;
    static final int MADE_BY_OS_X = 19;

    static final int SIG_LH = 0x04034b50;

    static final int LH_SIGNATURE = 0;
    static final int LH_MIN_VERSION = 4;
    static final int LH_GP_BITS = 6;
    static final int LH_COMP_METHOD = 8;
    static final int LH_MOD_TIME = 10;
    static final int LH_MOD_DATE = 12;
    static final int LH_CRC_32 = 14;
    static final int LH_COMPRESSED_SIZE = 18;
    static final int LH_UNCOMPRESSED_SIZE = 22;
    static final int LH_FILE_NAME_LENGTH = 26;
    static final int LH_EXTRA_LENGTH = 28;
    static final int LH_END = 30;

    static final int SIG_DD = 0x08074b50;

    static final int DD_SIGNATURE = 0;
    static final int DD_CRC_32 = 4;
    static final int DD_COMPRESSED_SIZE = 8;
    static final int DD_UNCOMPRESSED_SIZE = 12;
    static final int DD_END = 16;
    static final int DD_ZIP64_COMPRESSED_SIZE = 8;
    static final int DD_ZIP64_UNCOMPRESSED_SIZE = 16;
    static final int DD_ZIP64_END = 24;

    static final int SIG_CDE = 0x02014b50;

    static final int CDE_SIGNATURE = 0;
    static final int CDE_VERSION_MADE_BY = 4;
    static final int CDE_VERSION_NEEDED = 6;
    static final int CDE_GP_BITS = 8;
    static final int CDE_COMP_METHOD = 10;
    static final int CDE_MOD_TIME = 12;
    static final int CDE_MOD_DATE = 14;
    static final int CDE_CRC_32 = 16;
    static final int CDE_COMPRESSED_SIZE = 20;
    static final int CDE_UNCOMPRESSED_SIZE = 24;
    static final int CDE_FILE_NAME_LENGTH = 28;
    static final int CDE_EXTRA_LENGTH = 30;
    static final int CDE_COMMENT_LENGTH = 32;
    static final int CDE_FIRST_DISK_NUMBER = 34;
    static final int CDE_INTERNAL_ATTRIBUTES = 36;
    static final int CDE_EXTERNAL_ATTRIBUTES = 38;
    static final int CDE_LOCAL_HEADER_OFFSET = 42; // relative to the start of the above first disk number
    static final int CDE_END = 46;

    static final int SIG_EOCD = 0x06054b50;

    static final int EOCD_SIGNATURE = 0;
    static final int EOCD_DISK_NUMBER = 4;
    static final int EOCD_CD_FIRST_DISK_NUMBER = 6;
    static final int EOCD_CDE_COUNT_THIS_DISK = 8;
    static final int EOCD_CDE_COUNT_ALL = 10;
    static final int EOCD_CD_SIZE = 12;
    static final int EOCD_CD_START_OFFSET = 16;
    static final int EOCD_COMMENT_LENGTH = 20;
    static final int EOCD_END = 22;

    static final int EXT_ID_ZIP64 = 0x0001;

    static final int ZIP64_UNCOMPRESSED_SIZE = 4;
    static final int ZIP64_COMPRESSED_SIZE = 12;
    static final int ZIP64_LOCAL_HEADER_OFFSET = 20;
    static final int ZIP64_FIRST_DISK_NUMBER = 28; // 4 bytes
    static final int ZIP64_END = 32;

    static final int EXT_ID_UNIX = 0x000d;

    static final int UNIX_ACCESS_TIME = 0;
    static final int UNIX_MODIFIED_TIME = 4;
    static final int UNIX_UID = 8;
    static final int UNIX_GID = 10;
    static final int UNIX_END = 12; // symlink target

    static final int UNIX_DEV_MAJOR = 12; // if it's a device node
    static final int UNIX_DEV_MINOR = 16;
    static final int UNIX_DEV_END = 20;

    static final int SIG_EOCD_ZIP64 = 0x06064b50;

    static final int EOCD_ZIP64_SIGNATURE = 0;
    static final int EOCD_ZIP64_SIZE = 4;
    static final int EOCD_ZIP64_VERSION_MADE_BY = 12;
    static final int EOCD_ZIP64_VERSION_NEEDED = 14;
    static final int EOCD_ZIP64_DISK_NUMBER = 16;
    static final int EOCD_ZIP64_CD_FIRST_DISK_NUMBER = 20;
    static final int EOCD_ZIP64_CDE_COUNT_THIS_DISK = 24; // 8 bytes
    static final int EOCD_ZIP64_CDE_COUNT_ALL = 32; // 8 bytes
    static final int EOCD_ZIP64_CD_SIZE = 40; // 8 bytes
    static final int EOCD_ZIP64_CD_START_OFFSET = 48; // 8 bytes
    static final int EOCD_ZIP64_END = 56;

    static final int SIG_EOCDL_ZIP64 = 0x07064b50;

    static final int EOCDL_ZIP64_SIGNATURE = 0;
    static final int EOCDL_ZIP64_EOCD_DISK_NUMBER = 4;
    static final int EOCDL_ZIP64_EOCD_OFFSET = 8;
    static final int EOCDL_ZIP64_DISK_COUNT = 16;
    static final int EOCDL_ZIP64_END = 20;

    static final int EX_ZIP64 = 0x0001;
    static final int EX_NTFS = 0x000a;
    static final int EX_UNIX = 0x000d;

    // NTFS extra field layout (subtag 0x0001 for timestamps)
    static final int NTFS_SUBTAG_TIME = 0x0001;
    static final int NTFS_SUBTAG_TIME_DATA_SIZE = 24; // 3 × 8 bytes (mtime, atime, ctime)
    // total data size for NTFS extra field: 4 (reserved) + 4 (subtag header) + 24 (time data)
    static final int NTFS_EXTRA_DATA_SIZE = 32;
    // total size including the extra field header (2-byte ID + 2-byte size)
    static final int NTFS_EXTRA_TOTAL_SIZE = 36;

    // ZIP64 extra field data size in LFH (uncompressed + compressed only; no offset/disk)
    static final int ZIP64_LFH_EXTRA_DATA_SIZE = 16;
    // total size including the extra field header
    static final int ZIP64_LFH_EXTRA_TOTAL_SIZE = 20;

    // ZIP64 extra field data size in CDE (uncompressed + compressed + offset; no disk number)
    static final int ZIP64_CDE_EXTRA_DATA_SIZE = 24;
    // total size including the extra field header
    static final int ZIP64_CDE_EXTRA_TOTAL_SIZE = 28;

    // Version values
    static final int VERSION_NEEDED_UTF8 = 63; // 6.3 for UTF-8 (GP bit 11)
    static final int VERSION_MADE_BY_UNIX_63 = (MADE_BY_UNIX << 8) | VERSION_NEEDED_UTF8;
}
