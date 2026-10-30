package io.github.sevincfurkan.noads.sdk

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/** The class files of real ad SDK binaries, read with ASM so that nothing is loaded or run. */
class SdkBinaries private constructor(private val classes: Map<String, ClassInfo>) {
    class Method(val name: String, val descriptor: String)

    class ClassInfo(val superName: String?, val interfaces: List<String>, val methods: List<Method>, val fields: Set<String>)

    /** Methods declared by [className] or inherited from its supertypes in the SDKs; null if the class is missing. */
    fun methodsOf(className: String): List<Method>? {
        val info = classes[className.replace('.', '/')] ?: return null
        val supertypes = listOfNotNull(info.superName) + info.interfaces
        return info.methods + supertypes.flatMap { methodsOf(it) ?: emptyList() }
    }

    fun fieldsOf(className: String): Set<String>? = classes[className.replace('.', '/')]?.fields

    companion object {
        private val PACKAGES = listOf("com/google/android/gms/ads/", "com/applovin/", "com/unity3d/", "com/ironsource/")

        /** Reads the jars and aars listed in the system property [key], which the module's Gradle build sets. */
        fun fromSystemProperty(key: String): SdkBinaries {
            val paths = System.getProperty(key)?.split(File.pathSeparator)?.filter(String::isNotBlank)
            require(!paths.isNullOrEmpty()) { "System property $key is not set: run this test through Gradle." }
            val classes = HashMap<String, ClassInfo>()
            for (file in paths.map(::File)) {
                ZipFile(file).use { zip ->
                    for (entry in zip.entries()) {
                        when {
                            file.extension == "aar" && entry.name.endsWith(".jar") ->
                                ZipInputStream(zip.getInputStream(entry)).use { readJar(it, classes) }
                            file.extension == "jar" && entry.name.endsWith(".class") ->
                                zip.getInputStream(entry).use { readClass(entry.name, it, classes) }
                        }
                    }
                }
            }
            return SdkBinaries(classes)
        }

        private fun readJar(jar: ZipInputStream, into: MutableMap<String, ClassInfo>) {
            while (true) {
                val entry = jar.nextEntry ?: break
                if (entry.name.endsWith(".class")) readClass(entry.name, jar, into)
            }
        }

        private fun readClass(entryName: String, input: InputStream, into: MutableMap<String, ClassInfo>) {
            if (PACKAGES.none(entryName::startsWith)) return
            var superName: String? = null
            var interfaces = emptyList<String>()
            val methods = mutableListOf<Method>()
            val fields = mutableSetOf<String>()
            val visitor = object : ClassVisitor(Opcodes.ASM9) {
                override fun visit(
                    version: Int,
                    access: Int,
                    name: String,
                    signature: String?,
                    superClass: String?,
                    superInterfaces: Array<out String>?,
                ) {
                    superName = superClass
                    interfaces = superInterfaces?.toList() ?: emptyList()
                }

                override fun visitMethod(
                    access: Int,
                    name: String,
                    descriptor: String,
                    signature: String?,
                    exceptions: Array<out String>?,
                ): MethodVisitor? {
                    methods += Method(name, descriptor)
                    return null
                }

                override fun visitField(
                    access: Int,
                    name: String,
                    descriptor: String,
                    signature: String?,
                    value: Any?,
                ): FieldVisitor? {
                    fields += name
                    return null
                }
            }
            ClassReader(input.readBytes()).accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            into[entryName.removeSuffix(".class")] = ClassInfo(superName, interfaces, methods, fields)
        }
    }
}
