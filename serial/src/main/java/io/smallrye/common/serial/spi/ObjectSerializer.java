package io.smallrye.common.serial.spi;

import java.io.IOException;

import io.smallrye.common.serial.SerialContext;
import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.Serializer;

/**
 * An object serializer is responsible for performing serialization on some specific kind of object.
 */
public non-sealed interface ObjectSerializer extends Prioritized {
    /**
     * Serialize an object or delegate serialization to the next serializer.
     * Note that on return from each serializer, the serialized form of the object will be updated
     * as if {@link Context#preSetSerialized(Serialized)} was called.
     *
     * @param ctxt the serialization context (not {@code null})
     * @param object the object to serialize
     * @return the serialized representation of the object (must not be {@code null})
     * @throws IOException if serialization fails due to an I/O error
     */
    Serialized serialize(Context ctxt, Object object) throws IOException;

    /**
     * The context for an object serializer.
     */
    sealed interface Context extends Serializer permits SerialContext.SerializerContextImpl {

        /**
         * Set or replace the serialized representation of the object currently being serialized, even if serialization
         * is not yet complete.
         *
         * @param obj the interim object value to set
         */
        void preSetSerialized(Serialized obj);

        /**
         * Delegate serialization to the next serializer.
         *
         * @return the serialized value
         * @throws IOException if serialization fails due to an I/O error
         */
        Serialized next() throws IOException;
    }
}
