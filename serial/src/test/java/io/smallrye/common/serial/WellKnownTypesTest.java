package io.smallrye.common.serial;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * Tests for round-trip serialization of well-known JDK types that exercise
 * various serialization mechanisms (custom writeObject/readObject, writeReplace/readResolve, etc.).
 */
class WellKnownTypesTest {

    private final SerialContext ctx = SerialContext.builder().addDefaultProviders().build();

    @Test
    void arrayListRoundTrip() throws IOException, ClassNotFoundException {
        ArrayList<String> original = new ArrayList<>(List.of("alpha", "beta", "gamma"));
        Serialized serialized = ctx.serialize(original);
        ArrayList<?> result = (ArrayList<?>) ctx.deserialize(serialized);
        assertEquals(original, result);
    }

    @Test
    void hashMapRoundTrip() throws IOException, ClassNotFoundException {
        HashMap<String, Integer> original = new HashMap<>(Map.of("a", 1, "b", 2, "c", 3));
        Serialized serialized = ctx.serialize(original);
        HashMap<?, ?> result = (HashMap<?, ?>) ctx.deserialize(serialized);
        assertEquals(original, result);
    }

    @Test
    void linkedHashMapRoundTrip() throws IOException, ClassNotFoundException {
        LinkedHashMap<String, Integer> original = new LinkedHashMap<>();
        original.put("first", 1);
        original.put("second", 2);
        original.put("third", 3);
        Serialized serialized = ctx.serialize(original);
        LinkedHashMap<?, ?> result = (LinkedHashMap<?, ?>) ctx.deserialize(serialized);
        assertEquals(original, result);
        // verify insertion order is preserved
        assertIterableEquals(original.keySet(), result.keySet());
    }

    @Test
    void hashSetRoundTrip() throws IOException, ClassNotFoundException {
        HashSet<String> original = new HashSet<>(Set.of("x", "y", "z"));
        Serialized serialized = ctx.serialize(original);
        HashSet<?> result = (HashSet<?>) ctx.deserialize(serialized);
        assertEquals(original, result);
    }

    @Test
    void treeMapRoundTrip() throws IOException, ClassNotFoundException {
        TreeMap<String, Integer> original = new TreeMap<>();
        original.put("cherry", 3);
        original.put("apple", 1);
        original.put("banana", 2);
        Serialized serialized = ctx.serialize(original);
        TreeMap<?, ?> result = (TreeMap<?, ?>) ctx.deserialize(serialized);
        assertEquals(original, result);
        assertIterableEquals(original.keySet(), result.keySet());
    }

    @Test
    void treeSetRoundTrip() throws IOException, ClassNotFoundException {
        TreeSet<String> original = new TreeSet<>(Set.of("cherry", "apple", "banana"));
        Serialized serialized = ctx.serialize(original);
        TreeSet<?> result = (TreeSet<?>) ctx.deserialize(serialized);
        assertEquals(original, result);
        assertIterableEquals(original, result);
    }

    @Test
    void collectionsUnmodifiableListRoundTrip() throws IOException, ClassNotFoundException {
        List<String> original = Collections.unmodifiableList(new ArrayList<>(List.of("a", "b", "c")));
        Serialized serialized = ctx.serialize(original);
        List<?> result = (List<?>) ctx.deserialize(serialized);
        assertEquals(original, result);
    }

    @Test
    void dateRoundTrip() throws IOException, ClassNotFoundException {
        Date original = new Date(1234567890000L);
        Serialized serialized = ctx.serialize(original);
        Date result = (Date) ctx.deserialize(serialized);
        assertEquals(original, result);
    }

    @Test
    void enumMapRoundTrip() throws IOException, ClassNotFoundException {
        EnumMap<Thread.State, String> original = new EnumMap<>(Thread.State.class);
        original.put(Thread.State.NEW, "new");
        original.put(Thread.State.RUNNABLE, "running");
        Serialized serialized = ctx.serialize(original);
        EnumMap<?, ?> result = (EnumMap<?, ?>) ctx.deserialize(serialized);
        assertEquals(original, result);
    }

    @Test
    void enumSetRoundTrip() throws IOException, ClassNotFoundException {
        EnumSet<Thread.State> original = EnumSet.of(Thread.State.NEW, Thread.State.BLOCKED);
        Serialized serialized = ctx.serialize(original);
        EnumSet<?> result = (EnumSet<?>) ctx.deserialize(serialized);
        assertEquals(original, result);
    }
}
