package io.smallrye.common.io;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;

import io.smallrye.common.constraint.Assert;

/**
 * A utility class for conveniently producing {@link FileAttribute} implementations.
 */
public final class FileAttributes {
    private FileAttributes() {
    }

    /**
     * Produce a file attribute containing a Windows-style ACL entry set.
     *
     * @param entry the ACL entry (must not be {@code null})
     * @return the file attribute (not {@code null})
     */
    public static FileAttribute<List<AclEntry>> acl(AclEntry entry) {
        return acl(List.of(entry));
    }

    /**
     * Produce a file attribute containing a Windows-style ACL entry set.
     *
     * @param entries the ACL entries (must not be {@code null})
     * @return the file attribute (not {@code null})
     */
    public static FileAttribute<List<AclEntry>> acl(AclEntry... entries) {
        return acl(List.of(entries));
    }

    /**
     * Produce a file attribute containing a Windows-style ACL entry set.
     *
     * @param entries the ACL entries (must not be {@code null})
     * @return the file attribute (not {@code null})
     */
    public static FileAttribute<List<AclEntry>> acl(List<AclEntry> entries) {
        return new FileAttributeImpl<>("acl:acl", List.copyOf(entries));
    }

    /**
     * Produce a file attribute containing a creation timestamp.
     * <p>
     * <b>Note:</b> this attribute is only supported when creating new archive
     * entries and is not recognized by most {@code FileSystem} providers,
     * which require the attribute to be set later.
     *
     * @param time the creation time (must not be {@code null})
     * @return the file attribute (not {@code null})
     * @see Files#setAttribute(Path, String, Object, LinkOption...)
     */
    public static FileAttribute<FileTime> creationTime(FileTime time) {
        Assert.checkNotNullParam("time", time);
        return new FileAttributeImpl<>("basic:creationTime", time);
    }

    /**
     * Produce a file attribute containing a creation timestamp.
     * <p>
     * <b>Note:</b> this attribute is only supported when creating new archive
     * entries and is not recognized by most {@code FileSystem} providers,
     * which require the attribute to be set later.
     *
     * @param time the creation time (must not be {@code null})
     * @return the file attribute (not {@code null})
     * @see Files#setAttribute(Path, String, Object, LinkOption...)
     */
    public static FileAttribute<FileTime> creationTime(Instant time) {
        Assert.checkNotNullParam("time", time);
        return creationTime(FileTime.from(time));
    }

    /**
     * Produce a file attribute containing a last-modified timestamp.
     * <p>
     * <b>Note:</b> this attribute is only supported when creating new archive
     * entries and is not recognized by most {@code FileSystem} providers,
     * which require the attribute to be set later.
     *
     * @param time the creation time (must not be {@code null})
     * @return the file attribute (not {@code null})
     * @see Files#setAttribute(Path, String, Object, LinkOption...)
     */
    public static FileAttribute<FileTime> lastModifiedTime(FileTime time) {
        Assert.checkNotNullParam("time", time);
        return new FileAttributeImpl<>("basic:lastModifiedTime", time);
    }

    /**
     * Produce a file attribute containing a last-modified timestamp.
     * <p>
     * <b>Note:</b> this attribute is only supported when creating new archive
     * entries and is not recognized by most {@code FileSystem} providers,
     * which require the attribute to be set later.
     *
     * @param time the creation time (must not be {@code null})
     * @return the file attribute (not {@code null})
     * @see Files#setAttribute(Path, String, Object, LinkOption...)
     */
    public static FileAttribute<FileTime> lastModifiedTime(Instant time) {
        Assert.checkNotNullParam("time", time);
        return lastModifiedTime(FileTime.from(time));
    }

    /**
     * Produce a file attribute containing a last-access timestamp.
     * <p>
     * <b>Note:</b> this attribute is only supported when creating new archive
     * entries and is not recognized by most {@code FileSystem} providers,
     * which require the attribute to be set later.
     *
     * @param time the creation time (must not be {@code null})
     * @return the file attribute (not {@code null})
     * @see Files#setAttribute(Path, String, Object, LinkOption...)
     */
    public static FileAttribute<FileTime> lastAccessTime(FileTime time) {
        Assert.checkNotNullParam("time", time);
        return new FileAttributeImpl<>("basic:lastAccessTime", time);
    }

    /**
     * Produce a file attribute containing a last-access timestamp.
     * <p>
     * <b>Note:</b> this attribute is only supported when creating new archive
     * entries and is not recognized by most {@code FileSystem} providers,
     * which require the attribute to be set later.
     *
     * @param time the creation time (must not be {@code null})
     * @return the file attribute (not {@code null})
     * @see Files#setAttribute(Path, String, Object, LinkOption...)
     */
    public static FileAttribute<FileTime> lastAccessTime(Instant time) {
        Assert.checkNotNullParam("time", time);
        return lastAccessTime(FileTime.from(time));
    }

    /**
     * Produce a file attribute containing a POSIX file permission set.
     * Unlike {@link PosixFilePermissions#asFileAttribute(Set)},
     * this method uses compact sets which are cached and reused.
     *
     * @param perms the permissions (must not be {@code null})
     * @return the file attribute (not {@code null})
     */
    public static FileAttribute<Set<PosixFilePermission>> posixPermissions(Set<PosixFilePermission> perms) {
        int mask = 0;
        for (PosixFilePermission item : perms) {
            mask |= 1 << item.ordinal();
        }
        return permissionAttr(mask);
    }

    /**
     * Produce a file attribute containing a POSIX file permission set.
     * Unlike {@link PosixFilePermissions#asFileAttribute(Set)},
     * this method uses compact sets which are cached and reused.
     *
     * @param perms the permissions (must not be {@code null})
     * @return the file attribute (not {@code null})
     */
    public static FileAttribute<Set<PosixFilePermission>> posixPermissions(PosixFilePermission... perms) {
        int mask = 0;
        for (PosixFilePermission item : perms) {
            mask |= 1 << item.ordinal();
        }
        return permissionAttr(mask);
    }

    /**
     * Produce a file attribute containing a POSIX file permission set.
     * Unlike {@link PosixFilePermissions#asFileAttribute(Set)},
     * this method uses compact sets which are cached and reused.
     *
     * @param perm1 the permission (must not be {@code null})
     * @return the file attribute (not {@code null})
     */
    public static FileAttribute<Set<PosixFilePermission>> posixPermissions(PosixFilePermission perm1) {
        return permissionAttr(1 << perm1.ordinal());
    }

    /**
     * Produce a file attribute containing a POSIX file permission set.
     * Unlike {@link PosixFilePermissions#asFileAttribute(Set)},
     * this method uses compact sets which are cached and reused.
     *
     * @param perm1 the first permission (must not be {@code null})
     * @param perm2 the second permission (must not be {@code null})
     * @return the file attribute (not {@code null})
     */
    public static FileAttribute<Set<PosixFilePermission>> posixPermissions(PosixFilePermission perm1,
            PosixFilePermission perm2) {
        return permissionAttr(1 << perm1.ordinal() | 1 << perm2.ordinal());
    }

    /**
     * Produce a file attribute containing a POSIX file permission set.
     * Unlike {@link PosixFilePermissions#asFileAttribute(Set)},
     * this method uses compact sets which are cached and reused.
     *
     * @param perm1 the first permission (must not be {@code null})
     * @param perm2 the second permission (must not be {@code null})
     * @param perm3 the third permission (must not be {@code null})
     * @return the file attribute (not {@code null})
     */
    public static FileAttribute<Set<PosixFilePermission>> posixPermissions(PosixFilePermission perm1, PosixFilePermission perm2,
            PosixFilePermission perm3) {
        return permissionAttr(1 << perm1.ordinal() | 1 << perm2.ordinal() | 1 << perm3.ordinal());
    }

    /**
     * Produce a file attribute containing a POSIX file permission set.
     * Unlike {@link PosixFilePermissions#asFileAttribute(Set)},
     * this method uses compact sets which are cached and reused.
     * Only the bottom 9 bits of the mask are recognized.
     *
     * @param mask the bit mask
     * @return the file attribute (not {@code null})
     */
    public static FileAttribute<Set<PosixFilePermission>> posixPermissions(int mask) {
        return permissionAttr(mask & 0x1ff);
    }

    /**
     * Our basic file attribute implementation.
     *
     * @param name the attribute name (must not be {@code null})
     * @param value the attribute value
     * @param <T> the attribute type
     */
    record FileAttributeImpl<T>(String name, T value) implements FileAttribute<T> {
    }

    /**
     * All of the {@code PosixFilePermission} values in an immutable {@code List}.
     */
    private static final List<PosixFilePermission> perms = List.of(PosixFilePermission.values());

    /**
     * Cache for POSIX permissions sets (normally we're only making one or two of them).
     */
    private static final AtomicReferenceArray<FileAttribute<Set<PosixFilePermission>>> attrs = new AtomicReferenceArray<>(
            1 << perms.size());

    /**
     * Get a permission set from the cache, possibly creating it.
     *
     * @param mask the bit mask
     * @return the corresponding file attribute (not {@code null})
     */
    private static FileAttribute<Set<PosixFilePermission>> permissionAttr(final int mask) {
        FileAttribute<Set<PosixFilePermission>> attr = attrs.get(mask);
        if (attr == null) {
            EnumSet<PosixFilePermission> set = EnumSet.noneOf(PosixFilePermission.class);
            int v = mask;
            while (v != 0) {
                int lob = Integer.lowestOneBit(v);
                set.add(perms.get(Integer.numberOfTrailingZeros(lob)));
                v &= ~lob;
            }
            attr = new FileAttributeImpl<>("posix:permissions", Collections.unmodifiableSet(set));
            FileAttribute<Set<PosixFilePermission>> appearing = attrs.compareAndExchange(mask, null, attr);
            if (appearing != null) {
                attr = appearing;
            }
        }
        return attr;
    }
}
