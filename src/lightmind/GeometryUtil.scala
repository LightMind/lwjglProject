package lightmind

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.{GL11, GL20, GL15, GL30}
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

object GeometryUtil {
  def initFullscreenQuad() = {

    val a = 1f
    val vertices = Array[Float](
      // Left bottom triangle
      a, -a, 0f,
      -a, -a, 0f,
      -a, a, 0f,
      a, a, 0f
    )
    // Sending data to OpenGL requires the usage of (flipped) byte buffers
    val verticesBuffer = toBuffer(vertices)

    val indices = Array[Byte](
      0, 1, 2,
      2, 3, 0
    )

    val indicesCount = indices.length
    val indicesBuffer = toBuffer(indices)

    val uv = Array[Float](
      1, 0,
      0, 0,
      0, 1,
      1, 1
    )

    val uvBuffer = BufferUtils.createFloatBuffer(uv.length)
    uvBuffer.put(uv)
    uvBuffer.flip()

    val fullscrenVAO = GL30.glGenVertexArrays()
    GL30.glBindVertexArray(fullscrenVAO)

    // Create a new Vertex Buffer Object in memory and select it (bind)
    // A VBO is a collection of Vectors which in this case resemble the location of each vertex.
    val fullscrenVBO = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, fullscrenVBO)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW)
    // Put the VBO in the attributes list at index 0
    GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0)
    checkError("Creating fullscren VBO")

    val fullscreenVBOUV = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, fullscreenVBOUV)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, uvBuffer, GL15.GL_STATIC_DRAW)
    GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 0, 0)
    checkError("Creating fullscreen VBOUV")

    GL20.glEnableVertexAttribArray(0)
    GL20.glEnableVertexAttribArray(1)
    checkError("Enabling vertex attribute array for fullscreen quad")

    // Deselect (bind to 0) the VAO
    GL30.glBindVertexArray(0)

    val fullscrenIndicies = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, fullscrenIndicies)
    checkError("Creating indices buffer for fullscreen")
    GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW)
    checkError("Uploading indicies for fullscreen")
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)

    (fullscrenVAO, fullscrenVBO, fullscrenIndicies, fullscreenVBOUV, indicesCount)
  }

  def makeQuad(ws: Float, hs: Float, adj: Float, sprites: SpriteMap) = {

    // OpenGL expects vertices to be defined counter clockwise by default
    val vertices = Array[Float](
      // Left bottom triangle
      ws + adj, -adj, 0f,
      -adj, -adj, 0f,
      -adj, hs + adj, 0f,
      ws + adj, hs + adj, 0f
    )
    // Sending data to OpenGL requires the usage of (flipped) byte buffers
    val verticesBuffer = toBuffer(vertices)
    val vertexCount = 4

    val indices = Array[Byte](
      0, 1, 2,
      2, 3, 0
    )

    val indicesCount = indices.length
    val indicesBuffer = toBuffer(indices)

    val uvBuffer = sprites.getBuffer()

    // Create a new Vertex Array Object in memory and select it (bind)
    // A VAO can have up to 16 attributes (VBO's) assigned to it by default
    val vaoId = GL30.glGenVertexArrays()
    GL30.glBindVertexArray(vaoId)

    // Create a new Vertex Buffer Object in memory and select it (bind)
    // A VBO is a collection of Vectors which in this case resemble the location of each vertex.
    val vboId = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW)
    // Put the VBO in the attributes list at index 0
    GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0)

    val vbouvId = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbouvId)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, uvBuffer, GL15.GL_STATIC_DRAW)
    GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 0, 0)

    GL20.glEnableVertexAttribArray(0)
    GL20.glEnableVertexAttribArray(1)

    // Deselect (bind to 0) the VAO
    GL30.glBindVertexArray(0)

    val vboiId = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId)
    GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW)
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)

    (vaoId, vboId, vertexCount, vbouvId, vboiId, indicesCount)
  }
}