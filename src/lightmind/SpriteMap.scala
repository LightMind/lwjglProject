package lightmind

import org.lwjgl.BufferUtils

/**
 * Created by Lukas on 02-07-14.
 */
class SpriteMap(tilesRight: Int, tilesDown: Int) {
  val sizeWidth = 1f / tilesRight.toFloat
  val sizeHeight = 1f / tilesDown.toFloat

  private def init() = {
    val buffers =
      for (i <- 0 until tilesDown) yield {
        for (j <- 0 until tilesRight) yield {
          getBuffer(i, j)
        }
      }
    buffers.flatten
  }

  def getBuffer(i: Int, j: Int) = {
    val left = i.toFloat
    val top = j.toFloat

    val uv = Array[Float](
      sizeWidth * (left + 1), sizeHeight * (top),
      sizeWidth * left, sizeHeight * top,
      sizeWidth * (left), sizeHeight * (top + 1),
      sizeWidth * (left + 1), sizeHeight * (top + 1)
    )

    val uvBuffer = BufferUtils.createFloatBuffer(uv.length)
    uvBuffer.put(uv)
    uvBuffer.flip()
    uvBuffer
  }

}
