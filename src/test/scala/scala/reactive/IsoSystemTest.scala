package scala.reactive



import org.scalatest.{FunSuite, Matchers}
import scala.concurrent._
import scala.concurrent.duration._



class IsoSystemTest extends FunSuite with Matchers {

  class TestIsoSystem extends IsoSystem {
    def uniqueId() = ???
    def uniqueName(name: String) = ???
    def releaseNames(name: String) = ???
    def isolate[@spec(Int, Long, Double) T: Arrayable](p: Proto[Iso[T]]): Channel[T] = {
      ???
    }
    def newChannel[@spec(Int, Long, Double) Q](reactor: Reactor[Q]): Channel[Q] = ???
    def name = "TestIsoSystem"
    def bundle = IsoSystem.defaultBundle
    def channels = ???
  }

  test("tryCreateIsolate should return without throwing") {
    val system = new TestIsoSystem
    val proto = Proto[IsoSystemTest.TestIso]
    system.tryCreateIsolate(proto)
    assert(system.state.frames.forName("isolate-0") != null)
  }

  test("tryCreateIsolate should return without throwing and use custom name") {
    val system = new TestIsoSystem
    val proto = Proto[IsoSystemTest.TestIso].withName("Izzy")
    system.tryCreateIsolate(proto)
    assert(system.state.frames.forName("Izzy") != null)
    assert(system.state.frames.forName("Izzy").name == "Izzy")
  }

  test("tryCreateIsolate should throw when attempting to reuse the same name") {
    val system = new TestIsoSystem
    system.tryCreateIsolate(Proto[IsoSystemTest.TestIso].withName("Izzy"))
    intercept[IllegalArgumentException] {
      system.tryCreateIsolate(Proto[IsoSystemTest.TestIso].withName("Izzy"))
    }
  }

  test("tryCreateIsolate should create a default channel for the isolate") {
    val system = new TestIsoSystem
    val channel = system.tryCreateIsolate(Proto[IsoSystemTest.TestIso].withName("Izzy"))
    assert(channel != null)
    val conn = system.state.frames.forName("Izzy").connectors.forName("default")
    assert(conn != null)
    assert(conn.channel eq channel)
    assert(!conn.isDaemon)
  }

  test("tryCreateIsolate should create a system channel for the isolate") {
    val system = new TestIsoSystem
    system.tryCreateIsolate(Proto[IsoSystemTest.TestIso].withName("Izzy"))
    val conn = system.state.frames.forName("Izzy").connectors.forName("system")
    assert(conn != null)
    assert(conn.isDaemon)
  }

  test("tryCreateIsolate should schedule isolate's ctor for execution") {
    val system = new TestIsoSystem
    val p = Promise[Unit]()
    system.tryCreateIsolate(Proto[IsoSystemTest.PromiseIso](p))
    Await.result(p.future, 5.seconds)
  }

  test("tryCreateIsolate should invoke the ctor with the Iso.self set") {
    val system = new TestIsoSystem
    val p = Promise[Boolean]()
    system.tryCreateIsolate(Proto[IsoSystemTest.IsoSelfIso](p))
    assert(Await.result(p.future, 5.seconds))
  }

}


object IsoSystemTest {

  class TestIso extends Iso[Unit]

  class PromiseIso(val p: Promise[Unit]) extends Iso[Unit] {
    p.success(())
  }

  class IsoSelfIso(val p: Promise[Boolean]) extends Iso[Boolean] {
    if (Iso.self[Iso[_]] eq this) p.success(true)
    else p.success(false)
  }

}
