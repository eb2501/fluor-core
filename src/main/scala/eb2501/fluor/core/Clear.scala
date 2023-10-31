package eb2501.fluor.core

trait Clear[T] extends Write[T]:
  def clear(): Unit

