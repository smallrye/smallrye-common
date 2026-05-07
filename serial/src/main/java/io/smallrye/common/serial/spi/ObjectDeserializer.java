package io.smallrye.common.serial.spi;

import java.io.IOException;

import io.smallrye.common.serial.Deserializer;
import io.smallrye.common.serial.SerialContext;
import io.smallrye.common.serial.Serialized;

/**
 * An object deserializer is responsible for creating and initializing an object from a serialized representation.
 */
public non-sealed interface ObjectDeserializer extends Prioritized {
    /**
     * Deserialize a serialized object or delegate to the next deserializer.
     * Note that on return from each serializer, the serialized form of the object will be updated
     * as if {@link Context#preSetObject(Object)} was called.
     *
     * @param ctxt the deserialization context (not {@code null})
     * @param serialized the serialized object representation (not {@code null})
     * @return the deserialized object
     * @throws IOException if reading the serialized representation fails
     * @throws ClassNotFoundException if a class being deserialized cannot be loaded
     */
    Object deserialize(Context ctxt, Serialized serialized) throws IOException, ClassNotFoundException;

    /**
     * The context for an object deserializer.
     */
    sealed interface Context extends Deserializer permits SerialContext.DeserializerContextImpl {
        /**
         * Set or replace the value of the object being deserialized, even if deserialization
         * is not yet complete.
         *
         * @param obj the interim object value to set
         */
        void preSetObject(Object obj);

        /**
         * Delegate deserialization of this object to the next deserializer.
         *
         * @return the deserialized object
         * @throws IOException if deserialization fails due to an I/O error
         * @throws ClassNotFoundException if deserialization fails due to a missing required class
         */
        Object next() throws IOException, ClassNotFoundException;
    }
}
