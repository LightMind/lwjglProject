package lightmind.opengl

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

class VertexArrayObject(val id: Int, val indices: Int, val indicesCoun: Int, val fbos: Array[Int]) {
  var alive = true

  def destroy() {
    if (!alive) return

    glBindVertexArray(0)
    glBindBuffer(GL_ARRAY_BUFFER, 0)
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

    fbos.foreach(glDeleteBuffers(_))
    glDeleteBuffers(indices)
    glDeleteVertexArrays(id)

    alive = false
  }
}
