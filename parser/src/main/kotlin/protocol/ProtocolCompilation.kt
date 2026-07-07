package protocol

import protocol.error.SemanticError
import protocol.model.Protocol

data class ProtocolCompilation(val errors: List<SemanticError>, val protocol: Protocol, val binding: ProtocolBinding)
