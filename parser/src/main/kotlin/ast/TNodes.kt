package ast

sealed class TNode(open val position: Position)

data class IdNode(
    override val position: Position,
    val value: String
) : TNode(position)

data class ProtocolNode(
    override val position: Position,
    val name: IdNode,
    val states: List<TypeStateNode>
) : TNode(position)

data class TypeStateNode(
    override val position: Position,
    val name: IdNode,
    val transitions: List<TransitionNode>,
    val droppable: Boolean
) : TNode(position)

data class TransitionNode(
    override val position: Position,
    val method: MethodNode,
    val target: TargetNode
) : TNode(position)

data class MethodNode(
    override val position: Position,
    val name: IdNode,
    val args: List<JavaTypeNode>
) : TNode(position)

sealed class TargetNode(
    override val position: Position,
) : TNode(position)

data class TypeStateRefNode(
    override val position: Position,
    val name: IdNode
) : TargetNode(position)

data class OutPutStateNode(
    override val position: Position,
    val branches: List<BranchNode>
) : TargetNode(position)

data class BranchNode(
    override val position: Position,
    val label: IdNode,
    val target: TypeStateRefNode
) : TNode(position)

data class JavaTypeNode(
    override val position: Position,
    val name: List<IdNode>,
    val arrayLevel: Int
) : TNode(position)
