package io.smallrye.common.io;

import io.smallrye.common.constraint.Assert;

/**
 * Statistics for a quiet file deletion operation.
 */
public record DeleteStats(long directoriesFound, long directoriesRemoved, long filesFound, long filesRemoved) {
    public DeleteStats {
        Assert.checkMinimumParameter("directoriesFound", 0, directoriesFound);
        Assert.checkMinimumParameter("directoriesRemoved", 0, directoriesRemoved);
        Assert.checkMaximumParameter("directoriesRemoved", directoriesFound, directoriesRemoved);
        Assert.checkMinimumParameter("filesFound", 0, filesFound);
        Assert.checkMinimumParameter("filesRemoved", 0, filesRemoved);
        Assert.checkMaximumParameter("filesRemoved", filesFound, filesRemoved);
    }

    DeleteStats(long[] array) {
        this(array[DIR_FOUND], array[DIR_REMOVED], array[FILE_FOUND], array[FILE_REMOVED]);
    }

    static final int STATS_SIZE = 4;

    static final int DIR_FOUND = 0;
    static final int DIR_REMOVED = 1;
    static final int FILE_FOUND = 2;
    static final int FILE_REMOVED = 3;
}
