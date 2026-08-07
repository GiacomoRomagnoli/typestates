package language.model

import language.types.Id
import language.types.PT

data class JavaParameter(val name: Id, val type: PT) {
    constructor(name: String, type: PT) : this(Id(name), type)
}