package eb2501.fluor
package core

import scala.annotation.targetName
import scala.collection.mutable
import scala.ref.WeakReference
import com.typesafe.scalalogging.Logger
import scala.collection.Stepper.UnboxingIntStepper

private[core] final class Context:

  private class Frame:
    private val buffer = mutable.ArrayBuffer.empty[Cell[_]]
    private val set = mutable.Set.empty[Cell[_]]

    def +=(cell: Cell[_]): Unit =
      if !set.contains(cell)
      then
        buffer += cell
        set += cell
    
    def toArray: Array[Cell[_]] = buffer.toArray
  
  ///

  private class Nucleus[T](
    val value: T,
    val callees: Array[Cell[_]],
  ):
    private val content = mutable.WeakHashMap.empty[Cell[_], Long]
    private var counter = 0L

    def +=(caller: Cell[_]): Unit =
      content += caller -> counter
      counter += 1
    
    def -=(caller: Cell[_]): Unit =
      content -= caller
    
    def callers: Iterable[Cell[_]] =
      content.toArray.sortBy(_._2).map(_._1)

  ///

  final class Cell[T](
    expr: => T,
    private var listener: Option[Event[T] => Unit]
  ) extends Clear[T], Node:
    private var slot: Option[Nucleus[T]] = None

    private[Cell] def signal(event: Event[T]): Unit =
      try listener.foreach { _(event) }
      catch
        case e: Throwable =>
          logger.warn("Caught exception!", e)
  
    private def silenced[T](f: => T): T =
      val listener = this.listener
      this.listener = None
      try f
      finally this.listener = listener
    
    override def isCached: Boolean =
      checkContext()
      slot.isDefined

    override def callees: Option[List[Node]] =
      checkContext()
      slot.map(_.callees.toList)

    override def callers: Option[List[Node]] =
      checkContext()
      slot.map(_.callers.toList)

    override def get(): T =
      checkContext()
      stack match {
        case frame :: _ => frame += this
        case _ =>
      }
      slot match {
        case Some(nucleus) => nucleus.value
        case None =>
          val frame = Frame()
          stack = frame :: stack
          try {
            val value = expr
            val callees = frame.toArray
            callees.foreach { cell =>
              cell.slot.get += this
              cell.signal(Event.CallerAdded(this))
            }
            val nucleus = Nucleus(value, callees.toArray)
            slot = Some(nucleus)
            signal(Event.Cached(value, callees.toSet))
            value
          } finally {
            stack = stack.tail
          }
      }

    override def set(value: T): Unit =
      checkContext()
      require(stack.isEmpty, "Cannot set while doing a get")
      silenced {
        invalidate()
        val nucleus = Nucleus(value, Array.empty)
        slot = Some(nucleus)
      }
      signal(Event.Set(value))

    override def clear(): Unit =
      checkContext()
      require(stack.isEmpty, "Cannot clear while doing a get")
      silenced {
        invalidate()
      }
      signal(Event.Cleared)

    private[Cell] def invalidate(): Unit =
      slot match
        case Some(nucleus) =>
          silenced {
            nucleus.callers.toList.foreach { _.invalidate() }
            nucleus.callees.foreach { callee =>
              callee.slot.get -= this
              callee.signal(Event.CallerRemoved(this))
            }
            slot = None        
          }
          signal(Event.Invalidated)
        case _ =>

  ///////

  final class Cell1Map[K, T](
    getter: K => T,
    listener: Option[(K, Event[T]) => Unit]
  ) extends (K => Cell[T]), Iterable[K]:
    private val content = mutable.Map[K, Cell[T]]()

    override def apply(key: K): Cell[T] =
      checkContext()
      if content.contains(key)
      then content(key)
      else
        val cell = Cell(
          getter(key),
          Some((event: Event[T]) =>
            event match
              case Event.Invalidated | Event.Cleared =>
                content.remove(key)
              case _ =>
            listener.foreach { _(key, event) }
          )
        )
        content += (key -> cell)
        cell

    override def iterator: Iterator[K] = content.keysIterator

  ///////

  final class Cell2Map[K1, K2, T](
    getter: (K1, K2) => T,
    listener: Option[(K1, K2, Event[T]) => Unit]
  ) extends ((K1, K2) => Cell[T]), Iterable[(K1, K2)]:
    private val content = mutable.Map[(K1, K2), Cell[T]]()

    override def apply(key1: K1, key2: K2): Cell[T] =
      checkContext()
      if content.contains((key1, key2))
      then content((key1, key2))
      else
        val cell = Cell(
          getter(key1, key2),
          Some((event: Event[T]) =>
            event match
              case Event.Invalidated | Event.Cleared =>
                content.remove((key1, key2))
              case _ =>
            listener.foreach { _(key1, key2, event) }
          )
        )
        content += ((key1, key2) -> cell)
        cell

    override def iterator: Iterator[(K1, K2)] =
      checkContext()
      content.keysIterator

  ///////

  private val logger = Logger[Context]
  private var stack: List[Frame] = List.empty

  ///////

  private def checkContext(): Unit =
    require(
      this equals Context.instance,
      "Cannot access a node from a different thread"
    )
  
///////

private[core] object Context:
  private val _mapping = ThreadLocal.withInitial(() => Context())

  def instance: Context = _mapping.get()
