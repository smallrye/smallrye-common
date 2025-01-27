package io.smallrye.common.cpu;

import java.nio.ByteOrder;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Enumerated type for CPU types.
 */
// We should try to support (at least) all of the CPU types defined in jdk.internal.util.Architecture
public enum CPU {

    // IMPORTANT: Only add new CPU types to the end of the list to preserve ordinal sequence.
    /**
     * An unknown 32-bit little-endian CPU.
     */
    unknown32(4, ByteOrder.LITTLE_ENDIAN, Set.of(), true),
    /**
     * An unknown 32-bit big-endian CPU.
     */
    unknown32be(4, ByteOrder.BIG_ENDIAN, Set.of(), true),
    /**
     * An unknown 64-bit little-endian CPU.
     */
    unknown64(8, ByteOrder.LITTLE_ENDIAN, Set.of("unknown"), true),
    /**
     * An unknown 64-bit big-endian CPU.
     */
    unknown64be(8, ByteOrder.BIG_ENDIAN, Set.of(), true),

    /**
     * Intel/AMD 64-bit {@code x64} architecture.
     */
    x64(8, ByteOrder.LITTLE_ENDIAN, Set.of("x86_64", "amd64"), false),
    /**
     * Intel 32-bit {@code x86} architecture.
     */
    x86(4, ByteOrder.LITTLE_ENDIAN, Set.of("i386", "i486", "i586", "i686"), false),

    /**
     * {@code aarch64}, also known as {@code arm64}.
     */
    aarch64(8, ByteOrder.LITTLE_ENDIAN, Set.of("arm64"), false),
    /**
     * {@code arm}, also known as {@code armv7}, etc.
     */
    arm(4, ByteOrder.LITTLE_ENDIAN, Set.of("armv7", "armv7hl", "aarch32"), false),

    /**
     * {@code riscv}.
     */
    riscv(8, ByteOrder.LITTLE_ENDIAN, Set.of("riscv64"), false),

    /**
     * PowerPC 32-bit (big-endian).
     */
    ppc32(4, ByteOrder.BIG_ENDIAN, Set.of("ppc32be"), false),
    /**
     * PowerPC 32-bit (little-endian).
     */
    ppc32le(4, ByteOrder.LITTLE_ENDIAN, Set.of(), false),
    /**
     * PowerPC 64-bit (big-endian).
     */
    ppc(8, ByteOrder.BIG_ENDIAN, Set.of("ppc64", "ppcbe", "ppc64be"), false),
    /**
     * PowerPC 64-bit (little-endian).
     */
    ppcle(8, ByteOrder.LITTLE_ENDIAN, Set.of("ppc64le"), false),

    /**
     * WebAssembly 32-bit.
     */
    wasm32(4, ByteOrder.LITTLE_ENDIAN, Set.of("wasm"), false),

    // todo: s390

    /**
     * MIPS 32-bit (big-endian).
     */
    mips(4, ByteOrder.BIG_ENDIAN, Set.of("mips32, mipsbe, mips32be"), false),
    /**
     * MIPS 32-bit (little-endian).
     */
    mipsel(4, ByteOrder.LITTLE_ENDIAN, Set.of("mips32el"), false),
    /**
     * MIPS 64-bit (big-endian).
     */
    mips64(4, ByteOrder.BIG_ENDIAN, Set.of("mips64be"), false),
    /**
     * MIPS 64-bit (little-endian).
     */
    mips64el(4, ByteOrder.LITTLE_ENDIAN, Set.of(), false),
    ;

    /**
     * All of the possible CPU values, in order.
     */
    public static final List<CPU> values = List.of(values());

    private static final CPU hostCpu;

    // a "map" which is sorted by name, ignoring case
    private static final List<Map.Entry<String, CPU>> index = values.stream()
            .flatMap(cpu -> Stream.concat(Stream.of(cpu.name()), cpu.aliases().stream()).map(v -> Map.entry(v, cpu)))
            .sorted(Map.Entry.comparingByKey(String::compareToIgnoreCase))
            .toList();

    private final int pointerSizeBytes;
    private final ByteOrder nativeByteOrder;
    private final Set<String> aliases;
    private final boolean unknown;

    CPU(final int pointerSizeBytes, final ByteOrder nativeByteOrder, final Set<String> aliases, final boolean unknown) {
        this.pointerSizeBytes = pointerSizeBytes;
        this.nativeByteOrder = nativeByteOrder;
        this.aliases = aliases;
        this.unknown = unknown;
    }

    /**
     * {@return this CPU's pointer size, in bytes}
     */
    public int pointerSizeBytes() {
        return pointerSizeBytes;
    }

    /**
     * {@return this CPU's pointer size, in bits}
     */
    public int pointerSizeBits() {
        return pointerSizeBytes << 3;
    }

    /**
     * {@return this CPU's native byte order}
     */
    public ByteOrder nativeByteOrder() {
        return nativeByteOrder;
    }

    /**
     * {@return other names that this CPU is known by}
     */
    public Set<String> aliases() {
        return aliases;
    }

    /**
     * {@return <code>true</code> if this CPU is unknown}
     */
    public boolean isUnknown() {
        return unknown;
    }

    /**
     * Names are compared case-insensitively.
     *
     * @param name a <code>String</code> for the CPU name
     * @return the CPU for the given name
     * @throws NoSuchElementException if no such CPU is found
     */
    public static CPU forName(String name) throws NoSuchElementException {
        CPU cpu = forNameOrNull(name);
        if (cpu == null) {
            throw new NoSuchElementException();
        }
        return cpu;
    }

    /**
     * Names are compared case-insensitively.
     *
     * @param name a <code>String</code> for the CPU name
     * @return the CPU for the given name or <code>null</code> if it is not found
     */
    public static CPU forNameOrNull(String name) throws NoSuchElementException {
        Comparator<String> cmp = String::compareToIgnoreCase;

        int low = 0;
        int high = index.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Map.Entry<String, CPU> entry = index.get(mid);
            int res = cmp.compare(entry.getKey(), name);

            if (res < 0) {
                low = mid + 1;
            } else if (res > 0) {
                high = mid - 1;
            } else {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Names are compared case-insensitively.
     *
     * @param name a <code>String</code> for the CPU name
     * @return the optional CPU for the given name
     */
    public static Optional<CPU> forNameOpt(String name) throws NoSuchElementException {
        return Optional.ofNullable(forNameOrNull(name));
    }

    /**
     * {@return the host CPU type}
     */
    public static CPU host() {
        return hostCpu;
    }

    static {
        hostCpu = forNameOpt(System.getProperty("os.arch", "???")).map(CPU::check).orElseThrow();
    }

    private static CPU check(CPU cpu) {
        ByteOrder no = ByteOrder.nativeOrder();
        int bytes = JDK22Specific.ADDRESS_SIZE;
        if (cpu.pointerSizeBytes() == bytes && cpu.nativeByteOrder() == no) {
            // OK
            return cpu;
        }
        // CPU is unknown or doesn't match observed host characteristics
        return no == ByteOrder.BIG_ENDIAN ? bytes == 4 ? unknown32be : unknown64be : bytes == 4 ? unknown32 : unknown64;
    }
}
