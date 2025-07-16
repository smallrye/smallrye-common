package io.smallrye.common.classloader;

import static java.lang.constant.ConstantDescs.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.extras.reflect.AccessFlag;

class ClassDefinerTest {
    @Test
    void defineClass() throws Exception {
        Class<?> helloClass = ClassDefiner.defineClass(MethodHandles.lookup(), ClassDefinerTest.class,
                "io.smallrye.common.classloader.TestHello",
                getHelloClass("io.smallrye.common.classloader.TestHello"));
        assertNotNull(helloClass);

        Object hello = helloClass.getDeclaredConstructor().newInstance();
        Method helloMethod = helloClass.getDeclaredMethod("hello");
        assertEquals("hello", helloMethod.invoke(hello));
    }

    @Test
    void notAllowDifferentPackages() {
        assertThrows(IllegalArgumentException.class,
                () -> ClassDefiner.defineClass(MethodHandles.lookup(), ClassDefinerTest.class,
                        "io.smallrye.common.something.TestHello",
                        getHelloClass("io.smallrye.common.something.TestHello")));
    }

    private byte[] getHelloClass(final String name) {
        return ClassFile.of().build(ClassDesc.of(name), cb -> {
            cb.withVersion(ClassFile.JAVA_17_VERSION, 0);
            cb.withFlags(AccessFlag.PUBLIC);
            cb.withMethod("<init>", MethodTypeDesc.of(CD_void), 0, mb -> {
                mb.withFlags(AccessFlag.PUBLIC);
                mb.withCode(b0 -> {
                    b0.aload(0);
                    b0.invokespecial(CD_Object, "<init>", MethodTypeDesc.of(CD_void));
                    b0.return_();
                });
            });
            cb.withMethod("hello", MethodTypeDesc.of(CD_String), 0, mb -> {
                mb.withFlags(AccessFlag.PUBLIC);
                mb.withCode(b0 -> {
                    b0.loadConstant("hello");
                    b0.areturn();
                });
            });
        });
    }
}
