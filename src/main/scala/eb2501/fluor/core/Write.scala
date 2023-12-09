package eb2501.fluor
package core

trait Write[T] extends Read[T]:
  def set(value: T): Unit

  final def `^_=`(value: T): Unit =
    set(value)
