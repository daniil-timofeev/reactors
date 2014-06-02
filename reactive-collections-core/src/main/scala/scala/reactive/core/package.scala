package scala.reactive



import scala.reflect.ClassTag



package object core {

  def invalid(msg: String) = throw new IllegalStateException(msg)

  def unsupported(msg: String) = throw new UnsupportedOperationException(msg)

  implicit class ConcOps[T](val self: Conc[T]) extends AnyVal {
    def apply(i: Int) = {
      require(i >= 0 && i < self.size)
      ConcUtils.apply(self, i)
    }
    def foreach[U](f: T => U) = ConcUtils.foreach(self, f)
    def <>(that: Conc[T]) = ConcUtils.concatTop(self.normalized, that.normalized)
  }

  implicit class ConcModificationOps[@specialized(Byte, Char, Int, Long, Float, Double) T: ClassTag](val self: Conc[T]) {
    def update(i: Int, y: T) = {
      require(i >= 0 && i < self.size)
      ConcUtils.update(self, i, y)
    }
    def insert(i: Int, y: T) = {
      require(i >= 0 && i <= self.size)
      ConcUtils.insert(self, i, y)
    }
    def rappend(y: T) = ConcUtils.appendTop(self, new Conc.Single(y))
  }

  implicit class ConqueueApi[T: ClassTag](@specialized(Byte, Char, Int, Long, Float, Double) val self: Conqueue[T]) {
    import Conc._
    import Conqueue._
    def head: T = (ConcUtils.head(self): @unchecked) match {
      case s: Single[T] => s.x
      case c: Chunk[T] => c.array(0)
      case null => unsupported("empty")
    }
    def last: T = (ConcUtils.last(self): @unchecked) match {
      case s: Single[T] => s.x
      case c: Chunk[T] => c.array(c.size - 1)
      case null => unsupported("empty")
    }
    def tail: Conqueue[T] = (ConcUtils.head(self): @unchecked) match {
      case s: Single[T] =>
        ConcUtils.popHeadTop(self)
      case c: Chunk[T] =>
        val popped = ConcUtils.popHeadTop(self)
        if (c.size == 1) popped
        else {
          val nhead = new Chunk(ConcUtils.removedArray(c.array, 0, 0, c.size), c.size - 1, c.k)
          ConcUtils.pushHeadTop(popped, nhead)
        }
      case null =>
        unsupported("empty")
    }
    def init: Conqueue[T] = (ConcUtils.last(self): @unchecked) match {
      case s: Single[T] =>
        ConcUtils.popLastTop(self)
      case c: Chunk[T] =>
        val popped = ConcUtils.popLastTop(self)
        if (c.size == 1) popped
        else {
          val nlast = new Chunk(ConcUtils.removedArray(c.array, 0, c.size - 1, c.size), c.size - 1, c.k)
          ConcUtils.pushLastTop(popped, nlast)
        }
      case null =>
        unsupported("empty")
    }
    def :+(y: T) = (ConcUtils.last(self): @unchecked) match {
      case s: Single[T] =>
        ConcUtils.pushLastTop(self, new Single(y))
      case c: Chunk[T] if c.size == c.k =>
        val na = new Array[T](1)
        na(1) = y
        val nc = new Chunk(na, 1, c.k)
        ConcUtils.pushLastTop(self, nc)
      case c: Chunk[T] =>
        val popped = ConcUtils.popLastTop(self)
        val nlast = new Chunk(ConcUtils.insertedArray(c.array, 0, c.size, y, c.size), c.size + 1, c.k)
        ConcUtils.pushLastTop(popped, nlast)
      case null =>
        Tip(One(new Single(y)))
    }
    def +:(y: T) = (ConcUtils.head(self): @unchecked) match {
      case s: Single[T] =>
        ConcUtils.pushHeadTop(self, new Single(y))
      case c: Chunk[T] if c.size == c.k =>
        val na = new Array[T](1)
        na(1) = y
        val nc = new Chunk(na, 1, c.k)
        ConcUtils.pushHeadTop(self, nc)
      case c: Chunk[T] =>
        val popped = ConcUtils.popHeadTop(self)
        val nlast = new Chunk(ConcUtils.insertedArray(c.array, 0, 0, y, c.size), c.size + 1, c.k)
        ConcUtils.pushHeadTop(popped, nlast)
      case null =>
        Tip(One(new Single(y)))
    }
    def isEmpty = ConcUtils.isEmptyConqueue(self)
    def nonEmpty = !isEmpty
    def <|>(that: Conqueue[T]) = ConcUtils.concatConqueueTop(self, that)
  }

}










