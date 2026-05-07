package io.smallrye.common.serial;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for round-trip serialization and deserialization of dynamic proxy instances.
 */
class ProxyTest {

    private final SerialContext ctx = SerialContext.builder().addDefaultProviders().build();

    /**
     * A simple serializable invocation handler that records the method name.
     */
    public static class SimpleHandler implements InvocationHandler, Serializable {
        private static final long serialVersionUID = 1L;
        String lastMethodName;

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            lastMethodName = method.getName();
            return null;
        }
    }

    @Test
    void proxyRoundTrip() throws IOException, ClassNotFoundException {
        SimpleHandler handler = new SimpleHandler();
        handler.lastMethodName = "test";
        Object proxy = Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { Runnable.class },
                handler);

        Serialized serialized = ctx.serialize(proxy);
        assertInstanceOf(SerializedProxyObject.class, serialized);

        Object result = ctx.deserialize(serialized);
        assertTrue(Proxy.isProxyClass(result.getClass()));
        assertTrue(result instanceof Runnable);
        InvocationHandler resultHandler = Proxy.getInvocationHandler(result);
        assertInstanceOf(SimpleHandler.class, resultHandler);
        assertEquals("test", ((SimpleHandler) resultHandler).lastMethodName);
    }

    @Test
    void checkIntermediateStructure() throws IOException {
        Object proxy = Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { Runnable.class, Serializable.class },
                new SimpleHandler());

        Serialized serialized = ctx.serialize(proxy);
        SerializedProxyObject spo = (SerializedProxyObject) serialized;
        SerializedProxyClass pc = spo.proxyClass();
        List<String> ifNames = pc.interfaceNames();
        assertTrue(ifNames.contains("java.lang.Runnable"));
        assertTrue(ifNames.contains("java.io.Serializable"));
        assertNotNull(spo.invocationHandler());
    }
}
