package io.smallrye.common.serial;

import java.io.IOException;
import java.lang.reflect.Proxy;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.serial.spi.ObjectSerializer;

/**
 * The serialized representation of a dynamic proxy instance.
 * This captures a proxy's class descriptor and the serialized form of its
 * {@link java.lang.reflect.InvocationHandler}.
 * <p>
 * In the Java serialization protocol, a proxy object is written as a
 * {@code TC_OBJECT} whose class descriptor is a {@code TC_PROXYCLASSDESC}.
 * The proxy's only serializable state is its invocation handler.
 */
public final class SerializedProxyObject extends Serialized {
    private final SerializedProxyClass proxyClass;
    private Serialized invocationHandler;

    /**
     * Construct a new instance from pre-existing data.
     * This constructor is intended for wire protocol readers and manual graph construction.
     *
     * @param proxyClass the proxy class descriptor (must not be {@code null})
     * @param invocationHandler the serialized invocation handler (must not be {@code null};
     *        may be {@link SerializedNull#INSTANCE})
     */
    public SerializedProxyObject(final SerializedProxyClass proxyClass, final Serialized invocationHandler) {
        this.proxyClass = Assert.checkNotNullParam("proxyClass", proxyClass);
        this.invocationHandler = Assert.checkNotNullParam("invocationHandler", invocationHandler);
    }

    /**
     * Construct a new instance during serialization by capturing data from a live proxy.
     * Pre-registers this instance in the identity map before serializing the invocation handler,
     * so that circular references are handled correctly.
     *
     * @param proxy the proxy instance (must not be {@code null})
     * @param proxyClass the proxy class descriptor (must not be {@code null})
     * @param ctxt the serializer context (must not be {@code null})
     */
    SerializedProxyObject(final Object proxy, final SerializedProxyClass proxyClass, final ObjectSerializer.Context ctxt)
            throws IOException {
        this.proxyClass = proxyClass;
        ctxt.preSetSerialized(this);
        this.invocationHandler = ctxt.serialize(Proxy.getInvocationHandler(proxy));
    }

    /**
     * {@return the proxy class descriptor (not {@code null})}
     */
    public SerializedProxyClass proxyClass() {
        return proxyClass;
    }

    /**
     * {@return the serialized invocation handler (not {@code null})}
     */
    public Serialized invocationHandler() {
        return invocationHandler;
    }
}
