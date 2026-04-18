package dev.aether.plugin

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.commons.Method

private const val XOR_KEY = 0x5A.toByte()

class StringEncryptPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val androidComponents = project.extensions
            .getByType(AndroidComponentsExtension::class.java)

        androidComponents.onVariants { variant ->
            if (variant.buildType == "release") {
                variant.instrumentation.transformClassesWith(
                    StringEncryptVisitorFactory::class.java,
                    InstrumentationScope.PROJECT
                ) { params ->
                    params.xorKey.set(XOR_KEY.toInt())
                }
                variant.instrumentation.setAsmFramesComputationMode(
                    FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
                )
            }
        }
    }
}

interface StringEncryptParams : InstrumentationParameters {
    @get:Input
    val xorKey: Property<Int>
}

abstract class StringEncryptVisitorFactory
    : AsmClassVisitorFactory<StringEncryptParams> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        val key = parameters.get().xorKey.get().toByte()
        return StringEncryptClassVisitor(Opcodes.ASM9, nextClassVisitor, key)
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        val name = classData.className
        return !name.startsWith("dev.aether.manager.security.StringCrypt") &&
               !name.startsWith("android.") &&
               !name.startsWith("androidx.") &&
               !name.startsWith("kotlin.") &&
               !name.startsWith("kotlinx.") &&
               !name.startsWith("com.google.") &&
               !name.startsWith("com.unity3d.")
    }
}

class StringEncryptClassVisitor(
    api: Int,
    cv: ClassVisitor,
    private val key: Byte
) : ClassVisitor(api, cv) {

    override fun visitMethod(
        access: Int, name: String, descriptor: String,
        signature: String?, exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return StringEncryptMethodVisitor(api, mv, access, name, descriptor, key)
    }
}

class StringEncryptMethodVisitor(
    api: Int,
    mv: MethodVisitor,
    access: Int,
    name: String,
    descriptor: String,
    private val key: Byte
) : AdviceAdapter(api, mv, access, name, descriptor) {

    override fun visitLdcInsn(value: Any?) {
        if (value is String && value.isNotEmpty()) {
            val encrypted: ByteArray = value.toByteArray(Charsets.UTF_8)
                .map { (it.toInt() xor key.toInt()).toByte() }
                .toByteArray()

            push(encrypted.size)
            newArray(Type.BYTE_TYPE)
            encrypted.forEachIndexed { i, b ->
                dup()
                push(i)
                push(b.toInt())
                arrayStore(Type.BYTE_TYPE)
            }

            invokeStatic(
                Type.getType("Ldev/aether/manager/security/StringCrypt;"),
                Method.getMethod("String d (byte[])")
            )
        } else {
            super.visitLdcInsn(value)
        }
    }
}
