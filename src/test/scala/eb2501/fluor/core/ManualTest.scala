package eb2501.fluor.core

import scala.language.postfixOps

import org.scalatest.funsuite.AnyFunSuite
import scala.collection.mutable.ArrayDeque
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.ref.WeakReference
import java.util.WeakHashMap

class ManualTest extends AnyFunSuite:
  
  test("Getting Started") {
    object FluorModel extends Page:
      val n = write { 0 }
      def x = n.^ + 2
      def y = x + 3
      def z = x * 2
      val t = read { y + z }

    assert(FluorModel.n.^ == 0)
    assert(FluorModel.x == 2)
    assert(FluorModel.y == 5)
    assert(FluorModel.z == 4)
    assert(FluorModel.t.^ == 9)

    FluorModel.n.^ = 1

    assert(FluorModel.n.^ == 1)
    assert(FluorModel.x == 3)
    assert(FluorModel.y == 6)
    assert(FluorModel.z == 6)
    assert(FluorModel.t.^ == 12)

    ///

    object FluorModelB extends Page:
      val n = write { 0 }
      def x = n.^ + 2
      def y = x + 3
      def z = x * 2

      var countT = 0
      val t = read {
        countT += 1
        y + z
      }

    assert(FluorModelB.countT == 0)

    assert(FluorModelB.t.^ == 9)
    assert(FluorModelB.countT == 1)

    val temp1 = FluorModelB.t.^ + 1
    assert(FluorModelB.countT == 1)

    FluorModelB.n.^ = 1

    assert(FluorModelB.t.^ == 12)
    assert(FluorModelB.countT == 2)

    val temp2 = FluorModelB.t.^ + 1
    assert(FluorModelB.countT == 2)

    ///
    
    class FluorView(modulo: Integer) extends Page:
      val u = read (FluorModel.t.^ % modulo)

    val myView1 = FluorView(7)
    assert(myView1.u.^ == 5)

    val myView2 = FluorView(12)
    assert(myView2.u.^ == 0)

    FluorModel.n.^ = 2
    assert(myView1.u.^ == 1)
    assert(myView2.u.^ == 3)
  }

  test("Dependency DAG") {
    object FluorModel extends Page:
      val n = write { 0 }
      def x = n.^ + 2
      def y = x + 3
      def z = x * 2
      val t = read { y + z }

    assert(!FluorModel.n.isCached)
    assert(FluorModel.n.callees == None)
    assert(FluorModel.n.callers == None)
    assert(!FluorModel.t.isCached)
    assert(FluorModel.t.callees == None)
    assert(FluorModel.t.callers == None)

    assert(FluorModel.t.^ == 9)

    assert(FluorModel.n.isCached)
    assert(FluorModel.n.callees == Some(List()))
    assert(FluorModel.n.callers == Some(List(FluorModel.t)))

    assert(FluorModel.t.isCached)
    assert(FluorModel.t.callees == Some(List(FluorModel.n)))
    assert(FluorModel.t.callers == Some(List()))

    ///

    class FluorView(modulo: Integer) extends Page:
      val u = read (FluorModel.t.^ % modulo)

    val myView1 = FluorView(7)
    assert(myView1.u.^ == 2)

    val myView2 = FluorView(12)
    assert(myView2.u.^ == 9)

    assert(FluorModel.t.callers == Some(List(myView1.u, myView2.u)))
  }

  test("The Invalidation Process") {
    object MyModel extends Page:
      val x = clear { 0 }
      val y = write { 11 }
      val flag = write { true }
      val t = read {
        val x2 = x.^ * 2
        if flag.^ then
          x2 + y.^
        else
          x2 + 1
      }
      val u = read { y.^ + 32 }
      val v = read { x.^ + u.^ }

    assert(!MyModel.x.isCached)
    assert(!MyModel.y.isCached)
    assert(!MyModel.flag.isCached)
    assert(!MyModel.t.isCached)
    assert(!MyModel.u.isCached)
    assert(!MyModel.v.isCached)

    ///

    assert(MyModel.x.^ == 0)
    assert(MyModel.flag.^ == true)

    assert(MyModel.x.callees == Some(List()))
    assert(MyModel.x.callers == Some(List()))
    assert(!MyModel.y.isCached)
    assert(MyModel.flag.callees == Some(List()))
    assert(MyModel.flag.callers == Some(List()))
    assert(!MyModel.t.isCached)
    assert(!MyModel.u.isCached)
    assert(!MyModel.v.isCached)

    ///

    assert(MyModel.v.^ == 43)

    assert(MyModel.x.callees == Some(List()))
    assert(MyModel.x.callers == Some(List(MyModel.v)))
    assert(MyModel.y.callees == Some(List()))
    assert(MyModel.y.callers == Some(List(MyModel.u)))
    assert(MyModel.flag.callees == Some(List()))
    assert(MyModel.flag.callers == Some(List()))
    assert(!MyModel.t.isCached)
    assert(MyModel.u.callees == Some(List(MyModel.y)))
    assert(MyModel.u.callers == Some(List(MyModel.v)))
    assert(MyModel.v.callees == Some(List(MyModel.x, MyModel.u)))
    assert(MyModel.v.callers == Some(List()))

    ///

    assert(MyModel.t.^ == 11)
    
    assert(MyModel.x.callees == Some(List()))
    assert(MyModel.x.callers == Some(List(MyModel.v, MyModel.t)))
    assert(MyModel.y.callees == Some(List()))
    assert(MyModel.y.callers == Some(List(MyModel.u, MyModel.t)))
    assert(MyModel.flag.callees == Some(List()))
    assert(MyModel.flag.callers == Some(List(MyModel.t)))
    assert(MyModel.t.callees == Some(List(MyModel.x, MyModel.flag, MyModel.y)))
    assert(MyModel.t.callers == Some(List()))
    assert(MyModel.u.callees == Some(List(MyModel.y)))
    assert(MyModel.u.callers == Some(List(MyModel.v)))
    assert(MyModel.v.callees == Some(List(MyModel.x, MyModel.u)))
    assert(MyModel.v.callers == Some(List()))

    ///

    MyModel.x.clear()
    
    assert(!MyModel.x.isCached)
    assert(MyModel.y.callees == Some(List()))
    assert(MyModel.y.callers == Some(List(MyModel.u)))
    assert(MyModel.flag.callees == Some(List()))
    assert(MyModel.flag.callers == Some(List()))
    assert(!MyModel.t.isCached)
    assert(MyModel.u.callees == Some(List(MyModel.y)))
    assert(MyModel.u.callers == Some(List()))
    assert(!MyModel.v.isCached)

    ///

    assert(MyModel.v.^ == 43)
    assert(MyModel.t.^ == 11)
    MyModel.flag.^ = false

    assert(MyModel.x.callees == Some(List()))
    assert(MyModel.x.callers == Some(List(MyModel.v)))
    assert(MyModel.y.callees == Some(List()))
    assert(MyModel.y.callers == Some(List(MyModel.u)))
    assert(MyModel.flag.callees == Some(List()))
    assert(MyModel.flag.callers == Some(List()))
    assert(!MyModel.t.isCached)
    assert(MyModel.u.callees == Some(List(MyModel.y)))
    assert(MyModel.u.callers == Some(List(MyModel.v)))
    assert(MyModel.v.callees == Some(List(MyModel.x, MyModel.u)))
    assert(MyModel.v.callers == Some(List()))

    ///

    assert(MyModel.t.^ == 1)

    assert(MyModel.x.callees == Some(List()))
    assert(MyModel.x.callers == Some(List(MyModel.v, MyModel.t)))
    assert(MyModel.y.callees == Some(List()))
    assert(MyModel.y.callers == Some(List(MyModel.u)))
    assert(MyModel.flag.callees == Some(List()))
    assert(MyModel.flag.callers == Some(List(MyModel.t)))
    assert(MyModel.t.callees == Some(List(MyModel.x, MyModel.flag)))
    assert(MyModel.t.callers == Some(List()))
    assert(MyModel.u.callees == Some(List(MyModel.y)))
    assert(MyModel.u.callers == Some(List(MyModel.v)))
    assert(MyModel.v.callees == Some(List(MyModel.x, MyModel.u)))
    assert(MyModel.v.callers == Some(List()))
  }

  test("Proxies") {
    class Converter extends Page:
      val celsius = write { 0.0 }
      val fahrenheit = read { celsius.^ * 1.8 + 32.0 }

      def adjust(value: Double): Unit =
        celsius.^ += value
    
    val converter = Converter()

    assert(converter.celsius.^ == 0.0)
    assert(converter.fahrenheit.^ == 32.0)
    converter.celsius.^ = 100.0
    assert(converter.fahrenheit.^ == 212.0)
    converter.adjust(-20.0)
    assert(converter.fahrenheit.^ == 176.0)

    ///

    object Provider extends Page:
      val temperature = write { 20.0 }

    class Converter2 extends Converter:
      override val celsius = write { Provider.temperature.^ }

    val converter2 = Converter2()

    assert(converter2.fahrenheit.^ == 68.0)
    Provider.temperature.^ = 100.0
    assert(converter2.fahrenheit.^ == 212.0)
    converter2.adjust(-20.0)
    assert(converter2.fahrenheit.^ == 176.0)
    Provider.temperature.^ = 0.0
    assert(converter2.fahrenheit.^ != 32.0)

    ///

    class ConverterB extends Page:
      val celsius: Write[Double] = write { 0.0 }
      val fahrenheit = read { celsius.^ * 1.8 + 32.0 }

      def adjust(value: Double): Unit =
        celsius.^ += value
 
    class ConverterB2 extends ConverterB:
      override val celsius = proxy {
        Provider.temperature.^
      } { value =>
        Provider.temperature.^ = value
      }

    val converterB2 = ConverterB2()

    Provider.temperature.^ = 20.0
    assert(converterB2.fahrenheit.^ == 68.0)
    Provider.temperature.^ = 100.0
    assert(converterB2.fahrenheit.^ == 212.0)
    converterB2.adjust(-20.0)
    assert(converterB2.fahrenheit.^ == 176.0)
    Provider.temperature.^ = 0.0
    assert(converterB2.fahrenheit.^ == 32.0)
  }

  test("Parameterized Nodes") {
    object Model extends Page:
      val x = write { 1 }
      val y = write { 2 }
      val z = read1 { (key: Boolean) =>
        if key
        then x.^ + y.^
        else x.^ * 2
      }

    assert(Model.z(true).^ == 3)
    assert(Model.z.toSet == Set(true))
  }
  
  test("Monitoring Nodes") {
    object Model extends Page:
      val x = write { 1 }

      val events = ArrayDeque.empty[Event[Int]]
      val y = write_ln { x.^ + 1 } { events += _ }

      val z = write { y.^ + 1 }

    // y is becoming cached
    assert(Model.y.^ == 2)
    assert(Model.events == List(
      Event.Cached(2, Set(Model.x))
    ))
    Model.events.clear()

    // y has a new caller
    assert(Model.z.^ == 3)
    assert(Model.events == List(
      Event.CallerAdded(Model.z)
    ))
    Model.events.clear()

    // y has a caller removed
    Model.z.^ = 4
    assert(Model.events == List(
      Event.CallerRemoved(Model.z)
    ))
    Model.events.clear()

    // y is invalidated
    Model.x.^ = 2
    assert(Model.events == List(
      Event.Invalidated
    ))
    Model.events.clear()

    // y is assigned a value
    Model.y.^ = 5
    assert(Model.events == List(
      Event.Set(5)
    ))

    ///

    object ModelB extends Page:
      val x = write { 1 }
      val y = write { 2 }

      val events = ArrayDeque.empty[(Boolean, Event[Int])]
      val z = write1_ln { (key: Boolean) =>
        if key
        then x.^ + y.^
        else x.^ * 2
      } { (k: Boolean, e: Event[Int]) =>
        events += ((k, e))
      }
  }

  test("Threads Compatibility") {
    object Model extends Page:
      val x = write { 1 }

    assert(Model.x.^ == 1)
    val f = Future {
      Model.x.^ = 2
      ()
    }

    assertThrows[IllegalArgumentException] {
      Await.result(f, Duration.Inf)
    }
  }

  test("Implicit Conversion") {
    object Model extends Page:
      val x = write { 1 }
      val y = read { x.^ + 1 }
      val z = read { x + 1 }

    assert(Model.y.^ == 2)
    assert(Model.z.^ == 2)
  }

  test("Transparency") {
    class Item(start: Int) extends Page:
      val x = write { start }

    object Item:
      given Ordering[Item] with
        override def compare(a: Item, b: Item): Int =
          a.x.^ compare b.x.^

    val items = List(Item(5), Item(2), Item(3), Item(1), Item(4))

    def sortFromLibrary(items: List[Item]): List[Item] =
      items.sorted
    
    object Model extends Page:
      val sequence = read { sortFromLibrary(items) }

    assert(Model.sequence.^ == List(items(3), items(1), items(2), items(4), items(0)))
    assert(Model.sequence.callees.map(_.toSet) == Some((for i <- items yield i.x).toSet))

    ///

    items(1).x.^ = 10
    assert(Model.sequence.^ == List(items(3), items(2), items(4), items(0), items(1)))
  }

  test("Interfaces") {
    trait Model extends Page:
      val x: Write[Int] = write { 1 }
      val y: Read[Int] = read { x.^ + 1 }

    class ModelB extends Model:
      override val x = proxy { 2 } { _ => () }
  }

  test("Side Effects") {
    object Model extends Page:
      val x = write { 1 }
      val y = write { true }
      val z = read {
        y.^ = false
        x.^ + 1
      }

    assertThrows[IllegalArgumentException] {
      Model.z.^
    }
  }

  test("Capturing Dependencies") {
    class Data(value: Int) extends Page:
      val x = write { value }

    val data1 = Data(1)
    val data2 = Data(2)

    var source = data1

    object Reader extends Page:
      val value = read { source.x.^ + 5 }

    assert(Reader.value.^ == 6)

    ///

    source = data2
    assert(Reader.value.^ == 6)
 
    ///

    object ReaderB extends Page:
      val source = write { data1 }
      val value = read { source.x.^ + 5 }

    assert(ReaderB.value.^ == 6)

    ReaderB.source.^ = data2
    assert(ReaderB.value.^ == 7)
  }

  test("Garbage Collection") {
    object Model extends Page:
      val x = write { 1 }

    class Reader extends Page:
      val y = read { Model.x.^ + 1 }

    var reader = Reader()
    assert(reader.y.^ == 2)

    assert(Model.x.callers.map(_.size) == Some(1))

    ///

    val weakref = WeakReference(reader.y)
    reader = null
    while (weakref.get != None) {
      System.gc()
      Thread.sleep(10)
    }

    assert(Model.x.callers.map(_.size) == Some(0))
  }