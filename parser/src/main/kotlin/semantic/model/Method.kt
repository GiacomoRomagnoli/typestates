package semantic.model

import ast.MethodNode

data class Method private constructor(
    val simpleName: String,
    val args: List<JavaType>
) {
    companion object {
        fun build(node: MethodNode): Method = Method(node.name.value, node.args.map { JavaType.build(it) })
    }
}
