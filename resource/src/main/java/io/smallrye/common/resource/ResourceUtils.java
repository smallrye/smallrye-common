package io.smallrye.common.resource;

/**
 * Miscellaneous resource-related utilities.
 */
final class ResourceUtils {
    private ResourceUtils() {
    }

    private static final int ST_I_INIT = 0;
    private static final int ST_I_IGNORE = 1;
    private static final int ST_I_D1 = 3;
    private static final int ST_I_D2 = 4;

    private static final int ST_T_INIT = 5;
    private static final int ST_T_SLASH = 6;
    private static final int ST_T_D1 = 7;
    private static final int ST_T_D2 = 8;
    private static final int ST_T_CONTENT = 9;

    private static final int ST_M_INIT = 10;
    private static final int ST_M_D1 = 11;
    private static final int ST_M_D2 = 12;
    private static final int ST_M_IGNORE = 13;

    private static final int ST_C_INIT = 0;
    private static final int ST_C_CONTENT = 1;
    private static final int ST_C_D1 = 2;
    private static final int ST_C_D2 = 3;

    private static final int EOF = -1;

    /**
     * An efficient relative path string canonicalizer which avoids copying and allocation to the maximum possible extent.
     * The canonical path has no {@code .} or {@code ..} segments,
     * does not contain consecutive {@code /},
     * and does not begin or end with a {@code /}.
     *
     * @param path the path name (must not be {@code null})
     * @return the canonical equivalent path (not {@code null})
     */
    static String canonicalizeRelativePath(String path) {
        final int length = path.length();
        if (length == 0) {
            return path;
        }
        int idx = length;
        int state = ST_I_INIT;
        // state-specific values
        int t_end = -1, t_start = -1;
        int skip = 0;
        // current character
        int c;
        // main state machine -- iterate over string *in reverse*
        for (;;) {
            c = idx == 0 ? EOF : path.charAt(idx - 1);

            // on entry:
            //   idx is the index of the character after c (so it can be used as exclusive-end)
            //   t_end is the end index (exclusive) of the trailing substring to potentially return
            //   t_start is the start index (inclusive) of the middle substring to potentially return
            //   buf is the copied name buffer; the name always ends at the end of the buffer array
            //   buf_idx is the index (inclusive) of the first character in the copied name buffer
            //   skip is the number of outstanding .. segments to skip
            switch (state) {

                // ST_I_*: initial states where we are not actually capturing anything yet

                case ST_I_INIT -> {
                    switch (c) {
                        case '.' -> state = ST_I_D1;
                        case '/' -> {
                        }
                        case EOF -> {
                            return "";
                        }
                        default -> {
                            if (skip > 0) {
                                // ignore the rest
                                skip--;
                                state = ST_I_IGNORE;
                            } else {
                                // start capturing, possibly
                                t_end = idx;
                                state = ST_T_INIT;
                            }
                        }
                    }
                }
                case ST_I_IGNORE -> {
                    switch (c) {
                        case '/' -> state = ST_I_INIT;
                        default -> {
                        }
                    }
                }
                case ST_I_D1 -> {
                    switch (c) {
                        case '/' -> state = ST_I_INIT;
                        case '.' -> state = ST_I_D2;
                        case EOF -> {
                            return "";
                        }
                        default -> {
                            if (skip > 0) {
                                // ignore the rest
                                skip--;
                                state = ST_I_IGNORE;
                            } else {
                                // start capturing, possibly
                                t_end = idx + 1;
                                state = ST_T_INIT;
                            }
                        }
                    }
                }
                case ST_I_D2 -> {
                    switch (c) {
                        case EOF -> {
                            return "";
                        }
                        case '/' -> {
                            skip++;
                            state = ST_I_INIT;
                        }
                        default -> {
                            if (skip > 0) {
                                // ignore the rest
                                skip--;
                                state = ST_I_IGNORE;
                            } else {
                                // start capturing, possibly
                                t_end = idx + 2;
                                state = ST_T_INIT;
                            }
                        }
                    }
                }

                // ST_T_*: path ends at t_end; at least one captured character

                case ST_T_INIT -> {
                    switch (c) {
                        case EOF -> {
                            // this returns `path` if t_end == length
                            return path.substring(0, t_end);
                        }
                        case '/' -> state = ST_T_SLASH;
                        case '.' -> state = ST_T_D1;
                        default -> state = ST_T_CONTENT;
                    }
                }
                case ST_T_SLASH -> {
                    switch (c) {
                        case EOF -> {
                            // path starts with single /
                            return path.substring(1, t_end);
                        }
                        case '.' -> state = ST_T_D1;
                        case '/' -> {
                            // string is `path[0,idx) + "//" + valid_path + path[t_end,length)
                            t_start = idx + 1; // idx + 1 is the character after "//"
                            state = ST_M_INIT;
                        }
                        default -> state = ST_T_INIT;
                    }
                }
                case ST_T_D1 -> {
                    switch (c) {
                        case EOF -> {
                            // path starts with ./
                            return path.substring(2, t_end);
                        }
                        case '.' -> state = ST_T_D2;
                        case '/' -> {
                            // string is `path[0,idx) + "/./" + valid_path + path[t_end,length)
                            t_start = idx + 2; // idx + 2 is the character after "/./"
                            state = ST_M_INIT;
                        }
                        default -> state = ST_T_CONTENT;
                    }
                }
                case ST_T_D2 -> {
                    switch (c) {
                        case EOF -> {
                            // path starts with ../
                            return path.substring(3, t_end);
                        }
                        case '/' -> {
                            // string is `path[0,idx) + "/../" + valid_path + path[t_end,length)
                            skip++;
                            t_start = idx + 3; // idx + 3 is the character after "/../"
                            state = ST_M_INIT;
                        }
                        default -> state = ST_T_CONTENT;
                    }
                }
                case ST_T_CONTENT -> {
                    switch (c) {
                        case EOF -> {
                            // this returns `path` if t_end == length
                            return path.substring(0, t_end);
                        }
                        case '/' -> state = ST_T_SLASH;
                        default -> {
                        }
                    }
                }

                // ST_M_*: path starts at t_start and ends at t_end; there might be more valid path

                case ST_M_INIT -> {
                    switch (c) {
                        case EOF -> {
                            return path.substring(t_start, t_end);
                        }
                        case '.' -> state = ST_M_D1;
                        case '/' -> {
                        }
                        default -> {
                            if (skip > 0) {
                                skip--;
                                state = ST_M_IGNORE;
                            } else {
                                // multiple, separated path segments; go to slow path
                                return canonicalizeRelativePathWithCopy(path, t_start, t_end, idx);
                            }
                        }
                    }
                }
                case ST_M_D1 -> {
                    switch (c) {
                        case EOF -> {
                            // path starts with ./
                            return path.substring(t_start, t_end);
                        }
                        case '.' -> state = ST_M_D2;
                        case '/' -> state = ST_M_INIT;
                        default -> {
                            if (skip > 0) {
                                skip--;
                                state = ST_M_IGNORE;
                            } else {
                                // multiple, separated path segments; go to slow path
                                return canonicalizeRelativePathWithCopy(path, t_start, t_end, idx + 1);
                            }
                        }
                    }
                }
                case ST_M_D2 -> {
                    switch (c) {
                        case EOF -> {
                            // path starts with ../
                            return path.substring(t_start, t_end);
                        }
                        case '/' -> {
                            // string is `path[0,idx) + "/../" + path[idx+4,t_start) + valid_path + path[t_end,length)
                            skip++;
                            state = ST_M_INIT;
                        }
                        default -> {
                            if (skip > 0) {
                                skip--;
                                state = ST_M_IGNORE;
                            } else {
                                // multiple, separated path segments; go to slow path
                                return canonicalizeRelativePathWithCopy(path, t_start, t_end, idx + 2);
                            }
                        }
                    }
                }
                case ST_M_IGNORE -> {
                    switch (c) {
                        case EOF -> {
                            // path starts with some unclosed ..
                            return path.substring(t_start, t_end);
                        }
                        case '/' -> state = ST_M_INIT;
                        default -> {
                        }
                    }
                }
                default -> throw new IllegalStateException();
            }
            if (idx > 0) {
                idx--;
            }
        }
    }

    private static String canonicalizeRelativePathWithCopy(final String path, final int t_start, final int t_end,
            final int init_idx) {
        // this is the slow path where we must copy.
        char[] buf = new char[init_idx + 1 + t_end - t_start];
        int idx = init_idx;
        // this is the next available index in buf (counting from end, exclusive)
        int c_idx = buf.length - (t_end - t_start);
        path.getChars(t_start, t_end, buf, c_idx);
        // the end of the current captured content
        int s_end = -1;
        int skip = 0;
        int state = ST_C_INIT;
        // current character
        int c;
        // secondary state machine -- iterate over string *in reverse*
        for (;;) {
            c = idx == 0 ? EOF : path.charAt(idx - 1);
            switch (state) {
                case ST_C_INIT -> {
                    switch (c) {
                        case EOF -> {
                            // return buffer as-is
                            return new String(buf, idx, buf.length - c_idx);
                        }
                        case '.' -> state = ST_C_D1;
                        case '/' -> {
                        }
                        default -> {
                            s_end = idx;
                            state = ST_C_CONTENT;
                        }
                    }
                }
                case ST_C_D1 -> {
                    switch (c) {
                        case EOF -> {
                            return new String(buf, c_idx, buf.length - c_idx);
                        }
                        case '.' -> state = ST_C_D2;
                        case '/' -> state = ST_C_INIT;
                        default -> {
                            s_end = idx + 1;
                            state = ST_C_CONTENT;
                        }
                    }
                }
                case ST_C_D2 -> {
                    switch (c) {
                        case EOF -> {
                            return new String(buf, c_idx, buf.length - c_idx);
                        }
                        case '/' -> {
                            skip++;
                            state = ST_C_INIT;
                        }
                        default -> {
                            s_end = idx + 2;
                            state = ST_C_CONTENT;
                        }
                    }
                }
                case ST_C_CONTENT -> {
                    switch (c) {
                        case EOF -> {
                            if (skip == 0) {
                                // append segment
                                buf[--c_idx] = '/';
                                path.getChars(0, s_end, buf, c_idx - s_end);
                            }
                            return new String(buf, c_idx - s_end, buf.length - (c_idx - s_end));
                        }
                        case '/' -> {
                            if (skip == 0) {
                                // append segment
                                buf[--c_idx] = '/';
                                path.getChars(idx, s_end, buf, c_idx - (s_end - idx));
                                c_idx -= s_end - idx;
                            } else {
                                skip--;
                            }
                            state = ST_C_INIT;
                        }
                        default -> {
                        }
                    }
                }
            }
            if (idx > 0) {
                idx--;
            }
        }
    }
}
