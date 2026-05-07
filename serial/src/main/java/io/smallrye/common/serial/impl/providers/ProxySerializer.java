package io.smallrye.common.serial.impl.providers;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.stream.Collectors;

import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.SerializedProxyClass;
import io.smallrye.common.serial.impl.Util;
import io.smallrye.common.serial.spi.ObjectSerializer;

/**
 * Serializer that handles dynamic proxy instances.
 * Proxy objects are serialized by capturing the list of interface names
 * from the proxy class and the invocation handler as a nested serialized value.
 */
public final class ProxySerializer implements ObjectSerializer {

    /**
     * Construct a new instance.
     */
    public ProxySerializer() {
    }

    public Serialized serialize(final Context ctxt, final Object object) throws IOException {
        if (Proxy.isProxyClass(object.getClass())) {
            Class<?>[] interfaces = object.getClass().getInterfaces();
            var interfaceNames = Arrays.stream(interfaces)
                    .map(Class::getName)
                    .collect(Collectors.toUnmodifiableList());
            Serialized classLoader = ctxt.serialize(object.getClass().getClassLoader());
            SerializedProxyClass proxyClass = new SerializedProxyClass(interfaceNames, classLoader);
            return Util.newSerializedProxyObject(object, proxyClass, ctxt);
        } else {
            return ctxt.next();
        }
    }

    public int priority() {
        return PRIORITY_BASIC;
    }
}
