import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL21._
import org.lwjgl.opengl.GL30._
import org.lwjgl.opengl.GL31._
import org.lwjgl.opengl.GL32._
import org.lwjgl.opengl.GL33._

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

  def checkError(msg: String) {
    val e = glGetError()
    if (e == GL_INVALID_ENUM) {
      println(msg + " :" + " invalid enum")
      Mind.quit()
    }
    if (e == GL_INVALID_VALUE) {
      println(msg + " :" + " invalid value")
      Mind.quit()
    }
    if (e == GL_INVALID_OPERATION) {
      println(msg + " :" + " invalid operation")
      Mind.quit()
    }
    if (e == GL_INVALID_FRAMEBUFFER_OPERATION) {
      println(msg + " :" + " invalid framebuffer not done")
      Mind.quit()
    }
    if (e == GL_OUT_OF_MEMORY) {
      println(msg + " :" + " Out of memory!")
      Mind.quit()
    }
  }
}
