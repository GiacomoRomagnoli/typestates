package processor

import protocol.model.Protocol
import javax.lang.model.element.Element

fun interface Loader {
    fun load(path: String): Protocol
}

fun interface Emitter {
    fun emit(msg: String, element: Element?)
}