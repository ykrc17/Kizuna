package com.ykrc17.android.kizuna.core.generator

import com.squareup.javapoet.*
import com.ykrc17.android.kizuna.core.parseOutputClassName
import java.io.File
import javax.lang.model.element.Modifier

abstract class BaseGenerator {
    fun generate(args: Arguments, srcDir: File, callback: (File) -> Unit) {
        if (args.outputClassName.isNullOrEmpty()) {
            args.outputClassName= parseOutputClassName(args.layoutResId, getFileSuffix())
        }

        val javaFile = JavaFile.builder(args.outputPackageName, createClass(args)).build()
        javaFile.writeTo(srcDir, callback)
    }

    private fun createClass(args: Arguments): TypeSpec {
        return TypeSpec.classBuilder(args.outputClassName).apply {
            addModifiers(Modifier.PUBLIC)
            addJavadoc("Generated by kizuna(https://github.com/ykrc17/Kizuna)\n" +
                    "DO NOT MODIFY\n")

            addField(FieldSpec.builder(TypeName.INT, "LAYOUT_RES").apply {
                addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                initializer(CodeBlock.of("\$T.layout.${args.layoutResId}", args.rClass.toPoetClassName()))
            }.build())

            onCreateClass(this)
            args.layoutElements.forEach {
                addField(it.viewClass.toPoetClassName(), it.viewId, Modifier.PUBLIC)
            }

            public("bind") {
                addParameter(ClassName.get("android.view", "View"), "view")
                args.layoutElements.forEach {
                    addStatement("${it.viewId} = (\$T) view.findViewById(\$T.id.${it.viewId})", it.viewClass.toPoetClassName(), args.rClass.toPoetClassName())
                }
            }

            // 构造函数
            onCreateMethod(this)
        }.build()
    }

    abstract fun onCreateClass(type: TypeSpec.Builder)

    abstract fun onCreateMethod(type: TypeSpec.Builder)

    private fun JavaFile.writeTo(directory: File, callback: (File) -> Unit) {
        writeTo(directory)

        // copy from com.squareup.javapoet.JavaFile
        var outputDirectory = directory
        if (!packageName.isEmpty()) {
            for (packageComponent in packageName.split(".")) {
                outputDirectory = outputDirectory.resolve(packageComponent)
            }
        }
        callback(outputDirectory.resolve(typeSpec.name + ".java"))
    }

    fun TypeSpec.Builder.constructor(block: MethodSpec.Builder.() -> Unit) {
        val builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
        block(builder)
        addMethod(builder.build())
    }

    fun TypeSpec.Builder.public(name: String, block: MethodSpec.Builder.() -> Unit) {
        val builder = MethodSpec.methodBuilder(name).addModifiers(Modifier.PUBLIC)
        block(builder)
        addMethod(builder.build())
    }

    fun TypeSpec.Builder.publicStatic(name: String, block: MethodSpec.Builder.() -> Unit) {
        val builder = MethodSpec.methodBuilder(name).addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        block(builder)
        addMethod(builder.build())
    }

    abstract fun getFileSuffix(): String
}