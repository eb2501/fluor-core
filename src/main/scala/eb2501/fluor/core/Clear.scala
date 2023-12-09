package eb2501.fluor
package core

trait Clear[T] extends Write[T]:
  def clear(): Unit

