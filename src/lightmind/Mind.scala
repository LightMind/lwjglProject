package lightmind

import java.nio.{ByteBuffer, ByteOrder}

import lightmind.terrain.TileManager
import org.lwjgl.BufferUtils
import org.lwjgl.input.Mouse
import org.lwjgl.opengl._
import org.lwjgl.util.glu.GLU

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL21._
import org.lwjgl.opengl.GL30._
import org.lwjgl.opengl.GL31._
import org.lwjgl.opengl.GL32._
import org.lwjgl.opengl.GL33._


import scala.util.Random

object Mind extends App {
  var x = 0
  val w = 1280
  val h = 720
  var t = 1.0f
  var useShader = false
  var vaoId = 0
  var vboId = 0
  var vbocId = 0
  var vboiId = 0
  var vbouvId = 0
  var vertexCount = 0
  var indicesCount = 0
  var normalTexId = 0
  var heightmapTexId = 0
  var texId = 0
  var specularTexId = 0

  val scalarw = w / 64
  val scalarh = h / 64

  val ws = 64f
  val hs = 64f

  val adj = 0.0f

  val tileManager = new TileManager()
  val r = new Random()

  initDisplay()

  val (fullscrenVAO, fullscrenVBO, fullscrenIndicies, fullscreenVBOUV, fullscrenIndicesCount) = initFullscreenQuad()

  normalTexId = TextureUtil.loadPNGTexture("res/sprite1-normal.png", 0)
  texId = TextureUtil.loadPNGTexture("res/sprite1-color.png", 0)
  specularTexId = TextureUtil.loadPNGTexture("res/sprite1-specular.png", 0)
  heightmapTexId = TextureUtil.loadPNGTexture("res/sprite1-height.png", 0)

  val sprites16x16 = new SpriteMap(16, 16)

  val fboEnabled = GLContext.getCapabilities().GL_EXT_framebuffer_object
  val gbuffer1 = TextureUtil.generateTexture(w, h)
  val gbuffer2 = TextureUtil.generateTexture(w, h)
  val gbuffer3 = TextureUtil.generateTexture(w, h)
  val fbo = initFramebuffer(Array(gbuffer1, gbuffer2, gbuffer3))

  val (vertexOne, fragmentOne, programOne) = compileShaders("screen1.vert", "screen1.frag")
  val (vertexGBuffer, fragmentGBuffer, programGBuffer) = compileShaders("gpass.vert", "gpass.frag")
  makeQuad()
  initTiles()

  while (!Display.isCloseRequested) {
    x = x + 1
    t += 0.002f
    drawWithShader()
    Display.update()
  }

  clean()

  def initFramebuffer(textureIDs: Array[Int]) = {
    val attachments = Array[Int](GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3, GL_COLOR_ATTACHMENT4, GL_COLOR_ATTACHMENT5, GL_COLOR_ATTACHMENT6, GL_COLOR_ATTACHMENT7)
    val buffer =
      if (fboEnabled) {
        val buffer = ByteBuffer.allocateDirect(1 * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
        GL30.glGenFramebuffers(buffer)
        buffer.get()
      } else {
        -1
      }


    for (i <- 0 until textureIDs.length.min(8)) {
      GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, buffer)
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureIDs(1))
      GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
        GL11.GL_TEXTURE_2D, attachments(1), 0)
    }

    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)

    buffer
  }

  def initTiles() = {
    for (i <- 0 until w by 64) {
      for (j <- 0 until 320 by 64) {
        tileManager.createTile((i, j), (r.nextInt(2), 0))
      }
    }

    for (i <- 0 until w by 64) {
      for (j <- 320 until h by 64) {
        tileManager.createTile((i, j), (2, 1))
      }
    }
  }

  def clean() {
    // Disable the VBO index from the VAO attributes list
    GL20.glDisableVertexAttribArray(0)

    // Delete the vertex VBO
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    GL15.glDeleteBuffers(vboId)

    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    GL15.glDeleteBuffers(vbocId)

    // Delete the index VBO
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
    GL15.glDeleteBuffers(vboiId)

    // Delete the VAO
    GL30.glBindVertexArray(0)
    GL30.glDeleteVertexArrays(vaoId)

    GL20.glUseProgram(0)
    GL20.glDetachShader(programOne, vertexOne)
    GL20.glDetachShader(programOne, fragmentOne)

    GL20.glDeleteShader(vertexOne)
    GL20.glDeleteShader(fragmentOne)
    GL20.glDeleteProgram(programOne)

    GL20.glDetachShader(programGBuffer, vertexGBuffer)
    GL20.glDetachShader(programGBuffer, vertexGBuffer)

    GL20.glDeleteShader(vertexGBuffer)
    GL20.glDeleteShader(vertexGBuffer)
    GL20.glDeleteProgram(programGBuffer)

    Display.destroy()
  }

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
    val verticesBuffer = BufferUtils.createFloatBuffer(vertices.length)
    verticesBuffer.put(vertices)
    verticesBuffer.flip()

    val indices = Array[Byte](
      0, 1, 2,
      2, 3, 0
    )

    val indicesCount = indices.length
    val indicesBuffer = BufferUtils.createByteBuffer(indicesCount)
    indicesBuffer.put(indices)
    indicesBuffer.flip()

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
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)

    val fullscreenVBOUV = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, fullscreenVBOUV)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, uvBuffer, GL15.GL_STATIC_DRAW)
    GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 0, 0)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)

    GL20.glEnableVertexAttribArray(0)
    GL20.glEnableVertexAttribArray(1)

    // Deselect (bind to 0) the VAO
    GL30.glBindVertexArray(0)

    val fullscrenIndicies = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, fullscrenIndicies)
    GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW)
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)

    (fullscrenVAO, fullscrenVBO, fullscrenIndicies, fullscreenVBOUV, indicesCount)
  }

  def makeQuad() {

    // OpenGL expects vertices to be defined counter clockwise by default
    val vertices = Array[Float](
      // Left bottom triangle
      ws + adj, -adj, 0f,
      -adj, -adj, 0f,
      -adj, hs + adj, 0f,
      ws + adj, hs + adj, 0f
    )
    // Sending data to OpenGL requires the usage of (flipped) byte buffers
    val verticesBuffer = BufferUtils.createFloatBuffer(vertices.length)
    verticesBuffer.put(vertices)
    verticesBuffer.flip()
    vertexCount = 4

    val indices = Array[Byte](
      0, 1, 2,
      2, 3, 0
    )

    indicesCount = indices.length
    val indicesBuffer = BufferUtils.createByteBuffer(indicesCount)
    indicesBuffer.put(indices)
    indicesBuffer.flip()

    val colors = Array[Float](
      1f, 0f, 0f, 1f,
      0f, 1f, 0f, 1f,
      0f, 0f, 1f, 1f,
      1f, 1f, 1f, 1f
    )

    val colorsBuffer = BufferUtils.createFloatBuffer(colors.length)
    colorsBuffer.put(colors)
    colorsBuffer.flip()

    val uvBuffer = sprites16x16.getBuffer()

    // Create a new Vertex Array Object in memory and select it (bind)
    // A VAO can have up to 16 attributes (VBO's) assigned to it by default
    vaoId = GL30.glGenVertexArrays()
    GL30.glBindVertexArray(vaoId)

    // Create a new Vertex Buffer Object in memory and select it (bind)
    // A VBO is a collection of Vectors which in this case resemble the location of each vertex.
    vboId = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW)
    // Put the VBO in the attributes list at index 0
    GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0)
    // Deselect (bind to 0) the VBO
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)

    vbocId = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbocId)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, colorsBuffer, GL15.GL_STATIC_DRAW)
    GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, 0, 0)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)

    vbouvId = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbouvId)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, uvBuffer, GL15.GL_STATIC_DRAW)
    GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 0, 0)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)

    GL20.glEnableVertexAttribArray(0)
    GL20.glEnableVertexAttribArray(1)
    GL20.glEnableVertexAttribArray(2)

    // Deselect (bind to 0) the VAO
    GL30.glBindVertexArray(0)

    vboiId = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId)
    GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW)
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
  }

  def getLights() = {
    val mx = Mouse.getX
    val my = h - Mouse.getY

    val lights = Array[Float](
      mx, my,
      200f, 400f,
      225f, 400f,
      100f, 100f
    )
    // Sending data to OpenGL requires the usage of (flipped) byte buffers
    val lightBuffer = BufferUtils.createFloatBuffer(lights.length)
    lightBuffer.put(lights)
    lightBuffer.flip()

    val lightsCount = 4
    (lightBuffer, lightsCount)
  }

  def drawWithShader() {
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
    if (useShader) {
      ARBShaderObjects.glUseProgramObjectARB(programGBuffer)

      val locTime: Int = GL20.glGetUniformLocation(programGBuffer, "time")
      GL20.glUniform1f(locTime, t)

      val screenInfo = GL20.glGetUniformLocation(programGBuffer, "screen")
      GL20.glUniform4f(screenInfo, w, h, 0f, 0f)

      val uvScalars = GL20.glGetUniformLocation(programGBuffer, "uvScalars")
      GL20.glUniform2f(uvScalars, sprites16x16.sizeWidth, sprites16x16.sizeHeight)

      val normLocation = GL20.glGetUniformLocation(programGBuffer, "norm")
      val texLocation = GL20.glGetUniformLocation(programGBuffer, "tex")
      val specLocation = GL20.glGetUniformLocation(programGBuffer, "specular")
      val heightLocation = GL20.glGetUniformLocation(programGBuffer, "heightMap")

      GL20.glUniform1i(normLocation, 0)
      GL20.glUniform1i(texLocation, 2)
      GL20.glUniform1i(specLocation, 4)
      GL20.glUniform1i(heightLocation, 6)

      GL13.glActiveTexture(GL13.GL_TEXTURE0 + 0)
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalTexId)
      GL33.glBindSampler(0, GL11.GL_NEAREST)

      GL13.glActiveTexture(GL13.GL_TEXTURE0 + 2)
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId)
      GL33.glBindSampler(2, GL11.GL_NEAREST)

      GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4)
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, specularTexId)
      GL33.glBindSampler(4, GL11.GL_NEAREST)

      GL13.glActiveTexture(GL13.GL_TEXTURE0 + 6)
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, heightmapTexId)
      GL33.glBindSampler(6, GL11.GL_NEAREST)
    }

    // Bind to the VAO that has all the information about the quad vertices
    GL30.glBindVertexArray(vaoId)

    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId)

    GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbo)
    val indi = Array[Int](GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2)
    val indibuffer = BufferUtils.createIntBuffer(indi.length)
    indibuffer.put(indi)
    indibuffer.flip()
    glDrawBuffers(indibuffer)


    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)
    GL11.glDisable(GL11.GL_DEPTH_TEST)

    val posLocation = GL20.glGetUniformLocation(programGBuffer, "position")
    val uvPosition = GL20.glGetUniformLocation(programGBuffer, "uvPosition")

    for (tile <- tileManager.tiles) {
      val (i, j) = tile.texture
      val (x, y) = tile.pos
      GL20.glUniform2f(uvPosition, i, j)
      GL20.glUniform2f(posLocation, x, y)
      GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_BYTE, 0)
    }

    GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0)

    ARBShaderObjects.glUseProgramObjectARB(programOne)

    // bind the gbuffer to a texture
    val gbuffer1Location = GL20.glGetUniformLocation(programOne, "g1")
    GL20.glUniform1i(gbuffer1Location, 0)
    GL13.glActiveTexture(GL13.GL_TEXTURE0 + 0)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, gbuffer1)
    GL33.glBindSampler(0, GL11.GL_NEAREST)

    val gbuffer2Location = GL20.glGetUniformLocation(programOne, "g2")
    GL20.glUniform1i(gbuffer2Location, 2)
    GL13.glActiveTexture(GL13.GL_TEXTURE0 + 2)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, gbuffer2)
    GL33.glBindSampler(2, GL11.GL_NEAREST)

    val gbuffer3Location = GL20.glGetUniformLocation(programOne, "g3")
    GL20.glUniform1i(gbuffer3Location, 4)
    GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, gbuffer3)
    GL33.glBindSampler(4, GL11.GL_NEAREST)

    GL30.glBindVertexArray(fullscrenVAO)
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, fullscrenIndicies)
    GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_BYTE, 0)

    // Put everything back to default (deselect)
    //  GL20.glDisableVertexAttribArray(0)
    //  GL20.glDisableVertexAttribArray(1)
    //  GL20.glDisableVertexAttribArray(2)
    GL30.glBindVertexArray(0)

    if (useShader) ARBShaderObjects.glUseProgramObjectARB(0)
  }

  def initDisplay() {
    val pf = new PixelFormat()
    val ca = new ContextAttribs(3, 3).withForwardCompatible(true).withProfileCore(true)

    Display.setDisplayMode(new DisplayMode(w, h))
    Display.setTitle("2d light")
    Display.create(pf, ca)
    Display.sync(60)
    GL11.glDisable(GL11.GL_DEPTH_TEST)
    System.out.println("OpenGL version: " + GL11.glGetString(GL11.GL_VERSION))
  }

  def initView() {
    GL11.glViewport(0, 0, w, h)
    GL11.glMatrixMode(GL11.GL_PROJECTION)
    GL11.glLoadIdentity
    GLU.gluPerspective(45.0f, (w.asInstanceOf[Float] / h.asInstanceOf[Float]), 0.1f, 100.0f)
    GL11.glMatrixMode(GL11.GL_MODELVIEW)
    GL11.glLoadIdentity
    GL11.glShadeModel(GL11.GL_SMOOTH)
    GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
    GL11.glClearDepth(1.0f)
    GL11.glDisable(GL11.GL_DEPTH_TEST)
    GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST)
  }

  def compileShaders(vertex: String, fragment: String): (Int, Int, Int) = {
    var vertShader: Int = 0
    var fragShader: Int = 0

    val none = (0, 0, 0)

    vertShader = ShaderUtil.createShader("shaders/" + vertex, ARBVertexShader.GL_VERTEX_SHADER_ARB)
    fragShader = ShaderUtil.createShader("shaders/" + fragment, ARBFragmentShader.GL_FRAGMENT_SHADER_ARB)


    val program = ARBShaderObjects.glCreateProgramObjectARB

    if (program == 0) return none

    ARBShaderObjects.glAttachObjectARB(program, vertShader)
    ARBShaderObjects.glAttachObjectARB(program, fragShader)
    ARBShaderObjects.glLinkProgramARB(program)

    //GL20.glBindAttribLocation(program, 0, "in_Position")
    // GL20.glBindAttribLocation(program, 1, "in_Color")

    if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
      System.err.println(ShaderUtil.getLogInfo(program))
      return none
    }
    ARBShaderObjects.glValidateProgramARB(program)

    if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
      System.err.println(ShaderUtil.getLogInfo(program))
      return none
    }
    useShader = true
    val vertexShader = vertShader
    val fragmentShader = fragShader
    (vertexShader, fragmentShader, program)
  }
}
