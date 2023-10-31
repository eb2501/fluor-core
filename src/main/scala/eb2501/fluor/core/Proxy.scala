package eb2501.fluor.core

private class Proxy

private [core] object Proxy:

    private class _ReadProxy[T](
        expr: => T
    ) extends Read[T]:
        override def get(): T = expr

    private class _WriteProxy[T](
        expr: => T,
        setter: T => Unit
    ) extends _ReadProxy(expr), Write[T]:
        override def set(value: T): Unit = setter(value)

    private class _ClearProxy[T](
        expr: => T,
        setter: T => Unit,
        clearer: () => Unit
    ) extends _WriteProxy(expr, setter), Clear[T]:
        override def clear(): Unit = clearer()

    ///////

    def apply[T](expr: => T): Read[T] =
        _ReadProxy(expr)

    def apply[T](expr: => T, setter: T => Unit): Write[T] =
        _WriteProxy(expr, setter)
    
    def apply[T](expr: => T, setter: T => Unit, clearer: () => Unit): Clear[T] =
        _ClearProxy(expr, setter, clearer)
