package semantic.model

import ast.JavaTypeNode

data class JavaType private constructor(
    val qualifiedName: String,
    val arrayLevel: Int
) {
    companion object {
        fun build(node: JavaTypeNode): JavaType = JavaType(node.name.joinToString("."){ it.value }, node.arrayLevel)
    }
}
