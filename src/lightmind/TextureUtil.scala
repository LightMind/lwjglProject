package lightmind

import java.io.FileInputStream
import java.nio.ByteBuffer

import de.matthiasmann.twl.utils.PNGDecoder
import de.matthiasmann.twl.utils.PNGDecoder.Format
import lightmind.opengl.Texture
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.{GL11, GL13}
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
import Mind.checkError

/**
 * Created by Lukas on 02-07-14.
 */
object TextureUtil {
  def loadPNGTexture(filename: String, textureUnit: Int): Texture = {
    val (buf, width, height) = loadTexture(filename)
    val texID = GL11.glGenTextures()
    checkError("LoadPNG: creating texture")
    GL13.glActiveTexture(GL_TEXTURE0)
    checkError("LoadPNG: set active texture")
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID)
    checkError("LoadPNG: Bind Texture")
    GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)
    checkError("LoadPNG: unpack alignment")
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf)
    checkError("LoadPNG: setting texture")
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)
    checkError("LoadPNG: setting texture filters")
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
    checkError("LoadPNG: using nearest as min filter")
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    checkError("LoadPNG: using nearest as mag filter")
    new Texture(texID)
  }

  def loadTexture(filename: String) = {
    var id: ByteBuffer = null
    var tWidth = 0
    var tHeight = 0

    try {
      // Open the PNG file as an InputStream
      val in = new FileInputStream(filename)
      // Link the PNG decoder to this stream
      val decoder = new PNGDecoder(in)

      // Get the width and height of the texture
      tWidth = decoder.getWidth()
      tHeight = decoder.getHeight()

      // Decode the PNG file in a ByteBuffer
      id = ByteBuffer.allocateDirect(
        4 * decoder.getWidth() * decoder.getHeight())
      decoder.decode(id, decoder.getWidth() * 4, Format.RGBA)
      id.flip()

      in.close()
    }
    (id, tWidth, tHeight)
  }

  def generateTexture(width: Int, height: Int, textureUnit: Int) = {
    val texId = glGenTextures()
    GL13.glActiveTexture(GL_TEXTURE0 + textureUnit)
    glBindTexture(GL_TEXTURE_2D, texId)
    glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)

    val none: ByteBuffer = null
    GL11.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
      GL_RGBA, GL_UNSIGNED_BYTE, none)
    glBindTexture(GL_TEXTURE_2D, 0)

    Mind.checkError("Generate Texture")
    val texture = new Texture(texId)
    texture
  }
}