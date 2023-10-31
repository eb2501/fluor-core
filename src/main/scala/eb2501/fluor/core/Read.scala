package eb2501.fluor.core

trait Read[+T]:
  def get(): T
  
  final def ^ : T = get()

object Read:
  given [T]: Conversion[Read[T], T] = _.get()
