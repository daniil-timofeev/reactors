package io.reactors
package container



import io.reactors.common.HashMatrix
import scala.collection._
import scala.reflect.ClassTag



/** A reactive hash matrix.
 *
 *  @tparam T       type of the keys in the map, specialized
 */
class RHashMatrix[@spec(Int, Long, Double) T](
  implicit val arrayable: Arrayable[T]
) {
  private[reactors] var matrix: HashMatrix[T] = null
  private[reactors] var rawSize = 0
  private[reactors] var insertsEmitter: Events.Emitter[T] = null
  private[reactors] var removesEmitter: Events.Emitter[T] = null
  private[reactors] var subscription: Subscription = null

  protected def init(self: RHashMatrix[T]) {
    matrix = new HashMatrix[T]
    insertsEmitter = new Events.Emitter[T]
    removesEmitter = new Events.Emitter[T]
    subscription = Subscription.empty
  }

  init(this)

  def apply(x: Int, y: Int): T = matrix(x, y)

  def update(x: Int, y: Int, v: T): Unit = {
    val prev = matrix.applyAndUpdate(x, y, v)

    if (prev != nil) {
      removesEmitter.react(prev)
      rawSize -= 1
      if (v != nil) {
        insertsEmitter.react(prev)
        rawSize += 1
      }
    } else {
      if (v != nil) rawSize += 1
    }
  }

  def remove(x: Int, y: Int, v: T): Unit = update(x, y, nil)

  def nil: T = matrix.nil

  def foreach(f: T => Unit): Unit = matrix.foreach(f)

  def inserts: Events[T] = insertsEmitter

  def removes: Events[T] = removesEmitter

  def size: Int = rawSize

  def unsubscribe() = subscription.unsubscribe()

}


object RHashMatrix {
  implicit def factory[@spec(Int, Long, Double) T](
    implicit a: Arrayable[T]
  ): RContainer.Factory[(Int, Int, T), RHashMatrix[T]] = {
    new RContainer.Factory[(Int, Int, T), RHashMatrix[T]] {
      def apply(inserts: Events[(Int, Int, T)], removes: Events[(Int, Int, T)]):
        RHashMatrix[T] = {
        val hm = new RHashMatrix[T]
        hm.subscription = new Subscription.Composite(
          inserts.onEvent({ case (x, y, v) => hm.update(x, y, v) }),
          removes.onEvent({ case (x, y, v) => hm.remove(x, y, v) })
        )
        hm
      }
    }
  }
}
