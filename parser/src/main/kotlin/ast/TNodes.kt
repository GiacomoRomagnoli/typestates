package ast

sealed class TNode(open val position: Position)

data class IdNode(
    override val position: Position,
    val value: String
) : TNode(position)

data class TypeStateNode(
    override val position: Position,
    val name: IdNode,
    val states: List<StateNode>
) : TNode(position)

data class StateNode(
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
    val args: List<TypeNode>
) : TNode(position)

sealed class TargetNode(
    override val position: Position,
) : TNode(position)

sealed class StateTargetNode(
    override val position: Position,
) : TargetNode(position)

data class StateRefNode(
    override val position: Position,
    val name: IdNode
) : StateTargetNode(position)

data class EndStateNode(
    override val position: Position
) : StateTargetNode(position)

data class DecisionTargetNode(
    override val position: Position,
    val branches: List<BranchNode>
) : TargetNode(position)

data class BranchNode(
    override val position: Position,
    val label: IdNode,
    val target: StateTargetNode
) : TNode(position)

data class TypeNode(
    override val position: Position,
    val name: List<IdNode>,
    val arrayLevel: Int
) : TNode(position)
