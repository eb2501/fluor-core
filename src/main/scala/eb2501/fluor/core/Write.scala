package eb2501.fluor.core

trait Write[T] extends Read[T]:
  def set(value: T): Unit

  final def `^_=`(value: T): Unit =
    set(value)
