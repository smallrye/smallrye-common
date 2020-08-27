package io.smallrye.common.powerannotations.impl;

class MethodNotFoundException extends RuntimeException {
    MethodNotFoundException(Class<?> type, String methodName, String[] argTypes) {
        this(type, methodName, argTypes, null);
    }

    MethodNotFoundException(Class<?> type, String methodName, String[] argTypes, Throwable cause) {
        super("no method " + Utils.signature(methodName, argTypes) + " in " + type, cause);
    }
}

class FieldNotFoundException extends RuntimeException {
    FieldNotFoundException(String fieldName, Class<?> type) {
        this(fieldName, type, null);
    }

    FieldNotFoundException(String fieldName, Class<?> type, Throwable cause) {
        super("no field '" + fieldName + "' in " + type, cause);
    }
}
