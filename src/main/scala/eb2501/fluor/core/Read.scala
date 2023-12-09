package eb2501.fluor
package core

trait Read[+T]:
  def get(): T
  
  final def ^ : T = get()

object Read:
  given [T]: Conversion[Read[T], T] = _.get()
