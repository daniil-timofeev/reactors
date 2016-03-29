package io.reactors



import io.reactors.common.Monitor
import io.reactors.common.UnrolledRing
import scala.collection._



/** A queue that buffers events that arrive on the corresponding channel.
 *
 *  The `enqueue` method may have FIFO queue, priority queue, or some other semantics,
 *  depending on the implementation.
 */
trait EventQueue[@spec(Int, Long, Double) T] {

  /** Atomically enqueues the event `x` on the event queue.
   *
   *  This operation is linearizable and can be called by any number of threads.
   *
   *  @param x   the event to enqueue
   *  @return    the number of enqueued events when the event was enqueued
   */
  def enqueue(x: T): Int

  /** Atomically dequeues an event from the queue, and emits it on the event stream.
   *
   *  This operation is linearizable, but can only be invoked by the queue's owner, that
   *  is, by the owning reactor.
   *
   *  @param s   specialization implicit
   *  @return    the number of elements at the point when the event was dequeued
   */
  def dequeue()(implicit s: Spec[T]): Int

  /** Event stream on which this event queue releases events.
   *
   *  An event is released when `dequeue` is called. Note that only the owning reactor
   *  can subscribe to the event stream.
   */
  def events: Events[T]

  /** Size of the event queue.
   */
  def size: Int

}


/** Contains default event queue implementations.
 */
object EventQueue {

  abstract class Factory extends Serializable {
    def newInstance[@spec(Int, Long, Double) T: Arrayable]: EventQueue[T]
  }

  /** Event queue that drops all enqueued events.
   */
  class Zero[@spec(Int, Long, Double) T: Arrayable]
  extends EventQueue[T] {
    def enqueue(x: T) = 0
    def dequeue()(implicit s: Spec[T]) = 0
    def size = 0
    val events = new Events.Never[T]
  }

  def isZero(q: EventQueue[_]): Boolean = q.isInstanceOf[Zero[_]]

  /** Event queue backed by a synchronized, expandable unrolled ring.
   */
  class UnrolledRing[@spec(Int, Long, Double) T: Arrayable](
    private[reactors] val monitor: Monitor = new Monitor
  ) extends EventQueue[T] {
    private[reactors] val ring = new io.reactors.common.UnrolledRing[T]
    private[reactors] val emitter = new Events.Emitter[T]

    def enqueue(x: T): Int = monitor.synchronized {
      ring.enqueue(x)
      ring.size
    }

    def dequeue()(implicit s: Spec[T]): Int = {
      var remaining = -1
      val x: T = monitor.synchronized {
        remaining = ring.size - 1
        ring.dequeue()
      }
      emitter.react(x, null)
      remaining
    }

    def events: Events[T] = emitter

    def size: Int = monitor.synchronized {
      ring.size
    }
  }

  object UnrolledRing {

    object Factory extends EventQueue.Factory {
      def newInstance[@spec(Int, Long, Double) T: Arrayable]: EventQueue[T] = {
        new UnrolledRing[T]
      }
    }

  }

}