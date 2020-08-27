package io.smallrye.common.powerannotations.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

import io.smallrye.common.powerannotations.index.AnnotationInstance;
import io.smallrye.common.powerannotations.index.AnnotationValue;
import io.smallrye.common.powerannotations.index.ClassInfo;

/**
 * {@link #build() Builds} a {@link Proxy dynamic proxy} that delegates to three
 * function objects for the implementation.
 */
class AnnotationProxy {
    static Annotation proxy(AnnotationInstance annotationInstance) {
        return new AnnotationProxy(
                annotationInstance.typeName(),
                annotationInstance.toString(),
                name -> annotationInstance.value(name).value())
                        .build();
    }

    private final String typeName;
    private final String toString;
    private final Function<String, Object> property;

    private AnnotationProxy(String typeName, String toString, Function<String, Object> property) {
        this.typeName = typeName;
        this.toString = toString;
        this.property = property;
    }

    private Annotation build() {
        Class<?>[] interfaces = new Class[] { getAnnotationType(), Annotation.class };
        return (Annotation) Proxy.newProxyInstance(getClassLoader(), interfaces, this::invoke);
    }

    private Class<?> getAnnotationType() {
        return loadClass(typeName);
    }

    private static Class<?> loadClass(String typeName) {
        try {
            return getClassLoader().loadClass(typeName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("can't load annotation type " + typeName, e);
        }
    }

    private static ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return (classLoader == null) ? ClassLoader.getSystemClassLoader() : classLoader;
    }

    Object invoke(Object proxy, Method method, Object... args) {
        String name = method.getName();
        if (method.getParameterCount() == 1 && "equals".equals(name))
            return toString.equals(args[0].toString());
        // no other methods on annotations can have arguments (except for one `wait`)
        assert method.getParameterCount() == 0;
        assert args == null || args.length == 0;
        if ("hashCode".equals(name))
            return toString.hashCode();
        if ("annotationType".equals(name))
            return getAnnotationType();
        if ("toString".equals(name))
            return toString;

        Object value = property.apply(name);

        return toType(value, method.getReturnType());
    }

    private Object toType(Object value, Class<?> returnType) {
        if (returnType.isAnnotation())
            return proxy(AnnotationInstance.from(value));
        if (returnType.isEnum())
            return Utils.enumValue(returnType, (String) value);
        if (returnType.equals(Class.class))
            return ClassInfo.toClass(value);
        if (returnType.isArray())
            return toArray(returnType.getComponentType(), (Object[]) value);
        return value;
    }

    private Object toArray(Class<?> componentType, Object[] values) {
        Object array = Array.newInstance(componentType, values.length);
        for (int i = 0; i < values.length; i++)
            Array.set(array, i, toType(AnnotationValue.of(values[i]), componentType));
        return array;
    }
}
