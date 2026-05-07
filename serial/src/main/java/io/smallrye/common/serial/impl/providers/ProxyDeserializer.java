package io.smallrye.common.serial.impl.providers;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.SerializedProxyObject;
import io.smallrye.common.serial.spi.ObjectDeserializer;

/**
 * Deserializer that handles {@link SerializedProxyObject} instances,
 * reconstructing dynamic proxy instances from their serialized representation.
 */
public final class ProxyDeserializer implements ObjectDeserializer {

    /**
     * Construct a new instance.
     */
    public ProxyDeserializer() {
    }

    public Object deserialize(final Context ctxt, final Serialized serialized) throws IOException, ClassNotFoundException {
        if (serialized instanceof SerializedProxyObject spo) {
            var proxyClass = spo.proxyClass();
            var interfaceNames = proxyClass.interfaceNames();
            ClassLoader cl = ctxt.deserialize(proxyClass.classLoader(), ClassLoader.class);
            Class<?>[] interfaces = new Class<?>[interfaceNames.size()];
            for (int i = 0; i < interfaces.length; i++) {
                interfaces[i] = Class.forName(interfaceNames.get(i), false, cl);
            }
            InvocationHandler handler = ctxt.deserialize(spo.invocationHandler(), InvocationHandler.class);
            Object proxy = Proxy.newProxyInstance(cl, interfaces, handler);
            return proxy;
        } else {
            return ctxt.next();
        }
    }

    public int priority() {
        return PRIORITY_BASIC;
    }
}
