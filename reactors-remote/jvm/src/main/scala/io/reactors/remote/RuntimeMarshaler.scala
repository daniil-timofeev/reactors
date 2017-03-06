package io.reactors
package remote



import java.lang.reflect.Modifier



object RuntimeMarshaler {
  private val booleanClass = classOf[Boolean]
  private val byteClass = classOf[Byte]
  private val shortClass = classOf[Short]
  private val charClass = classOf[Char]
  private val intClass = classOf[Int]
  private val floatClass = classOf[Float]
  private val longClass = classOf[Long]
  private val doubleClass = classOf[Double]

  def marshalAs[T](clazz: Class[_], obj: T, inputData: Data): Data = {
    marshalAsInternal(clazz, obj, inputData)
  }

  private def marshalAsInternal[T](
    clazz: Class[_], obj: T, inputData: Data
  ): Data = {
    var data = inputData
    val fields = clazz.getDeclaredFields
    var i = 0
    while (i < fields.length) {
      val field = fields(i)
      if (!Modifier.isTransient(field.getModifiers)) {
        field.setAccessible(true)
        val tpe = field.getType
        if (tpe.isPrimitive) {
          tpe match {
            case RuntimeMarshaler.this.intClass =>
              val v = field.getInt(obj)
              if (data.spaceLeft < 4) data = data.flush()
              val pos = data.pos
              data.raw(pos + 0) = (v & 0x000000ff).toByte
              data.raw(pos + 1) = (v & 0x0000ff00).toByte
              data.raw(pos + 2) = (v & 0x00ff0000).toByte
              data.raw(pos + 3) = (v & 0xff000000).toByte
              data.pos += 4
            case RuntimeMarshaler.this.longClass =>
              val v = field.getLong(obj)
              if (data.spaceLeft < 8) data = data.flush()
              val pos = data.pos
              data.raw(pos + 0) = (v & 0x00000000000000ffL).toByte
              data.raw(pos + 1) = (v & 0x000000000000ff00L).toByte
              data.raw(pos + 2) = (v & 0x0000000000ff0000L).toByte
              data.raw(pos + 3) = (v & 0x00000000ff000000L).toByte
              data.raw(pos + 4) = (v & 0x000000ff00000000L).toByte
              data.raw(pos + 5) = (v & 0x0000ff0000000000L).toByte
              data.raw(pos + 6) = (v & 0x00ff000000000000L).toByte
              data.raw(pos + 7) = (v & 0xff00000000000000L).toByte
              data.pos += 8
            case RuntimeMarshaler.this.doubleClass =>
              val v = java.lang.Double.doubleToRawLongBits(field.getDouble(obj))
              if (data.spaceLeft < 8) data = data.flush()
              val pos = data.pos
              data.raw(pos + 0) = (v & 0x00000000000000ffL).toByte
              data.raw(pos + 1) = (v & 0x000000000000ff00L).toByte
              data.raw(pos + 2) = (v & 0x0000000000ff0000L).toByte
              data.raw(pos + 3) = (v & 0x00000000ff000000L).toByte
              data.raw(pos + 4) = (v & 0x000000ff00000000L).toByte
              data.raw(pos + 5) = (v & 0x0000ff0000000000L).toByte
              data.raw(pos + 6) = (v & 0x00ff000000000000L).toByte
              data.raw(pos + 7) = (v & 0xff00000000000000L).toByte
              data.pos += 8
            case RuntimeMarshaler.this.floatClass =>
              val v = java.lang.Float.floatToRawIntBits(field.getFloat(obj))
              if (data.spaceLeft < 4) data = data.flush()
              val pos = data.pos
              data.raw(pos + 0) = (v & 0x000000ff).toByte
              data.raw(pos + 1) = (v & 0x0000ff00).toByte
              data.raw(pos + 2) = (v & 0x00ff0000).toByte
              data.raw(pos + 3) = (v & 0xff000000).toByte
              data.pos += 4
            case RuntimeMarshaler.this.byteClass =>
              val v = field.getByte(obj)
              if (data.spaceLeft < 1) data = data.flush()
              val pos = data.pos
              data.raw(pos + 0) = v
              data.pos += 1
            case RuntimeMarshaler.this.booleanClass =>
              val v = field.getBoolean(obj)
              if (data.spaceLeft < 1) data = data.flush()
              val pos = data.pos
              data.raw(pos) = if (v) 1 else 0
              data.pos += 1
            case RuntimeMarshaler.this.charClass =>
              val v = field.getChar(obj)
              if (data.spaceLeft < 2) data = data.flush()
              val pos = data.pos
              data.raw(pos + 0) = (v & 0x000000ff).toByte
              data.raw(pos + 1) = (v & 0x0000ff00).toByte
              data.pos += 2
            case RuntimeMarshaler.this.shortClass =>
              val v = field.getInt(obj)
              if (data.spaceLeft < 2) data = data.flush()
              val pos = data.pos
              data.raw(pos + 0) = (v & 0x000000ff).toByte
              data.raw(pos + 1) = (v & 0x0000ff00).toByte
              data.pos += 2
          }
        } else if (tpe.isArray) {
          sys.error("Array marshaling is currently not supported.")
        } else {
          val value = field.get(obj)
          data = marshalInternal(value, data)
        }
      }
      i += 1
    }
    val superclazz = clazz.getSuperclass
    if (superclazz != null) marshalAs(superclazz, obj, data)
    else data
  }

  def marshal[T](obj: T, inputData: Data): Data = {
    marshalInternal(obj, inputData)
  }

  private def marshalInternal[T](obj: T, inputData: Data): Data = {
    var data = inputData
    // TODO: Write class identifier.
    ???
    val clazz = obj.getClass
    marshalAsInternal(clazz, obj, data)
  }
}
