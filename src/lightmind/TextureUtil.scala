package lightmind

import java.io.FileInputStream
import java.nio.ByteBuffer

import de.matthiasmann.twl.utils.PNGDecoder
import de.matthiasmann.twl.utils.PNGDecoder.Format
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.{GL11, GL13}

/**
 * Created by Lukas on 02-07-14.
 */
object TextureUtil {
  def loadPNGTexture(filename: String, textureUnit: Int): Int = {
    val (buf, width, height) = loadTexture(filename)
    val texID = GL11.glGenTextures()
    GL13.glActiveTexture(texID)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID)
    GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf)

    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)

    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

    texID
  }

  def loadTexture(filename: String) = {
    var buf: ByteBuffer = null
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
      buf = ByteBuffer.allocateDirect(
        4 * decoder.getWidth() * decoder.getHeight())
      decoder.decode(buf, decoder.getWidth() * 4, Format.RGBA)
      buf.flip()

      in.close()
    }
    (buf, tWidth, tHeight)
  }

  def generateTexture(width: Int, height: Int) = {
    val texId = glGenTextures()
    GL13.glActiveTexture(texId)
    glBindTexture(GL_TEXTURE_2D, texId)
    glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

    var none: ByteBuffer = null
    GL11.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
      GL_RGBA, GL_UNSIGNED_BYTE, none)
    glBindTexture(GL_TEXTURE_2D, 0)

    texId
  }
}
