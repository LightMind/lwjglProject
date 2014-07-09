import org.lwjgl.BufferUtils

package object lightmind {
  def toBuffer(values: Array[Int]) = {
    val b = BufferUtils.createIntBuffer(values.length)
    b.put(values)
    b.flip()
    b
  }

  def toBuffer(values: Array[Byte]) = {
    val b = BufferUtils.createByteBuffer(values.length)
    b.put(values)
    b.flip()
    b
  }

  def toBuffer(values: Array[Float]) = {
    val b = BufferUtils.createFloatBuffer(values.length)
    b.put(values)
    b.flip()
    b
  }
}
