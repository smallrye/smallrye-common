package io.smallrye.common.classloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

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
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(V1_8, ACC_PUBLIC, name.replace('.', '/'), null, "java/lang/Object", null);

        {
            MethodVisitor methodVisitor = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }

        {
            MethodVisitor methodVisitor = writer.visitMethod(ACC_PUBLIC, "hello", "()Ljava/lang/String;", null, null);
            methodVisitor.visitLdcInsn("hello");
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
            writer.visitEnd();
        }

        return writer.toByteArray();
    }
}
