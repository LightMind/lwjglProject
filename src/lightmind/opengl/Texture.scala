package lightmind.opengl

import org.lwjgl.opengl.GL11._

class Texture(val id: Int) {
  var alive = true

  def destroy() {
    if (!alive) return
    glDeleteTextures(id)
    alive = false
  }
}
