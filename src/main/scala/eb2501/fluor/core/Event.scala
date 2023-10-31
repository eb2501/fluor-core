package eb2501.fluor.core

import scala.collection.immutable

enum Event[+T] {
  case Cached(value: T, callees: immutable.Set[Node])
  case Invalidated
  case Set(value: T)
  case Cleared
  case CallerAdded(caller: Node)
  case CallerRemoved(caller: Node)
}
