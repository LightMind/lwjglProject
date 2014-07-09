package lightmind.opengl

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL21._
import org.lwjgl.opengl.GL30._
import org.lwjgl.opengl.GL31._
import org.lwjgl.opengl.GL32._
import org.lwjgl.opengl.GL33._

object ShaderProgram {

}

class ShaderProgram(val program: Int, val vertexShader: Int, val fragmentShader: Int) {
  def destroy() {
    glUseProgram(0)
    glDetachShader(program, vertexShader)
    glDetachShader(program, fragmentShader)

    glDeleteShader(program)
    glDeleteShader(fragmentShader)
    glDeleteProgram(vertexShader)
  }
}
