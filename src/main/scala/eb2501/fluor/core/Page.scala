
package eb2501.fluor
package core

import scala.collection.mutable

class Page:
  private val ctx = Context.instance

  //
  // 0 Section
  //

  final protected def read[T](
    expr: => T
  ): Read[T] & Node =
    ctx.Cell(expr, None)
  
  final protected def write[T](
    expr: => T
  ): Write[T] & Node =
    ctx.Cell(expr, None)
  
  final protected def clear[T](
    expr: => T
  ): Clear[T] & Node =
    ctx.Cell(expr, None)

  ///

  final protected def read_ln[T](
    expr: => T
  )(
    listener: Event[T] => Unit
  ): Read[T] & Node =
    ctx.Cell(expr, Some(listener))
  
  final protected def write_ln[T](
    expr: => T
  )(
    listener: Event[T] => Unit
  ): Write[T] & Node =
    ctx.Cell(expr, Some(listener))
  
  final protected def clear_ln[T](
    expr: => T
  )(
    listener: Event[T] => Unit
  ): Clear[T] & Node =
    ctx.Cell(expr, Some(listener))

  ///

  final protected def proxy[T](
    expr: => T
  ): Read[T] =
    Proxy(expr)
  
  final protected def proxy[T](
    expr: => T
  )(
    setFn: T => Unit
  ): Write[T] =
    Proxy(expr, setFn)
  
  final protected def proxy[T](
    expr: => T
  )(
    setFn: T => Unit
  )(
    clearFn: () => Unit
  ): Clear[T] =
    Proxy(expr, setFn, clearFn)

  //
  // 1 Section
  //

  final protected def read1[K, T](
    getFn: K => T
  ): (K => Read[T] & Node) & Iterable[K] =
    ctx.Cell1Map(getFn, None)

  final protected def write1[K, T](
    getFn: K => T
  ): (K => Write[T] & Node) & Iterable[K] =
    ctx.Cell1Map(getFn, None)

  final protected def clear1[K, T](
    getFn: K => T
  ): (K => Clear[T] & Node) & Iterable[K] =
    ctx.Cell1Map(getFn, None)

  ///

  final protected def read1_ln[K, T](
    getter: K => T
  )(
    listener: (K, Event[T]) => Unit
  ): (K => Read[T] & Node) & Iterable[K] =
    ctx.Cell1Map(getter, Some(listener))
  
  final protected def write1_ln[K, T](
    getter: K => T
  )(
    listener: (K, Event[T]) => Unit
  ): (K => Write[T] & Node) & Iterable[K] =
    ctx.Cell1Map(getter, Some(listener))
    
  final protected def clear1_ln[K, T](
    getter: K => T
  )(
    listener: (K, Event[T]) => Unit
  ): (K => Clear[T] & Node) & Iterable[K] =
    ctx.Cell1Map(getter, Some(listener))

  ///

  final protected def proxy1[K, T](
    getter: K => T
  )(
    key: K
  ): Read[T] =
    Proxy(getter(key))

  final protected def proxy1[K, T](
    getter: K => T
  )(
    setter: (K, T) => Unit
  )(
    key: K
  ): Write[T] =
    Proxy(getter(key), setter(key, _))
  
  final protected def proxy1[K, T](
    getter: K => T
  )(
    setter: (K, T) => Unit
  )(
    clearer: K => Unit
  )(
    key: K
  ): Clear[T] =
    Proxy(getter(key), setter(key, _), () => clearer(key))

  //
  // 2 Section
  //

  final protected def read2[K1, K2, T](
    getter: (K1, K2) => T
  ): ((K1, K2) => Read[T] & Node) & Iterable[(K1, K2)] =
    ctx.Cell2Map(getter, None)
  
  final protected def write2[K1, K2, T](
    getter: (K1, K2) => T
  ): ((K1, K2) => Write[T] & Node) & Iterable[(K1, K2)] =
    ctx.Cell2Map(getter, None)

  final protected def clear2[K1, K2, T](
    getter: (K1, K2) => T
  ): ((K1, K2) => Clear[T] & Node) & Iterable[(K1, K2)] =
    ctx.Cell2Map(getter, None)
  
  ///

  final protected def read2_ln[K1, K2, T](
    getter: (K1, K2) => T
  )(
    listener: (K1, K2, Event[T]) => Unit
  ): ((K1, K2) => Read[T] & Node) & Iterable[(K1, K2)] =
    ctx.Cell2Map(getter, Some(listener))
  
  final protected def write2_ln[K1, K2, T](
    getter: (K1, K2) => T
  )(
    listener: (K1, K2, Event[T]) => Unit
  ): ((K1, K2) => Write[T] & Node) & Iterable[(K1, K2)] =
    ctx.Cell2Map(getter, Some(listener))

  final protected def clear2_listen[K1, K2, T](
    getter: (K1, K2) => T
  )(
    listener: (K1, K2, Event[T]) => Unit
  ): ((K1, K2) => Clear[T] & Node) & Iterable[(K1, K2)] =
    ctx.Cell2Map(getter, Some(listener))
  
  ///

  final protected def proxy2[K1, K2, T](
    getter: (K1, K2) => T
  )(
    key1: K1,
    key2: K2
  ): Read[T]
    = Proxy(getter(key1, key2))
  
  final protected def proxy2[K1, K2, T](
    getter: (K1, K2) => T
  )(
    setter: (K1, K2, T) => Unit
  )(
    key1: K1,
    key2: K2
  ): Write[T]
    = Proxy(getter(key1, key2), setter(key1, key2, _))
  
  final protected def proxy2[K1, K2, T](
    getter: (K1, K2) => T
  )(
    setter: (K1, K2, T) => Unit
  )(
    clearer: (K1, K2) => Unit
  )(
    key1: K1,
    key2: K2
  ): Clear[T]
    = Proxy(getter(key1, key2), setter(key1, key2, _), () => clearer(key1, key2))
