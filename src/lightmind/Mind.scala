package lightmind

import java.nio.{ByteBuffer, ByteOrder}

import lightmind.opengl._
import lightmind.terrain.TileManager
import org.lwjgl.BufferUtils
import org.lwjgl.input.Mouse
import org.lwjgl.opengl._
import org.lwjgl.util.glu.GLU

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

import scala.util.Random

object Mind extends App {
  var x = 0
  val w = 1280
  val h = 720
  var t = 1.0f
  var vaoId = 0
  var vboId = 0
  var vbocId = 0
  var vboiId = 0
  var vbouvId = 0
  var vertexCount = 0
  var indicesCount = 0
  var close = false

  val scalarw = w / 64
  val scalarh = h / 64

  val ws = 64f
  val hs = 64f

  val adj = 0.0f

  val tileManager = new TileManager()
  val r = new Random()

  println("Init Display")
  initDisplay()

  val sampler = initSampler()

  println("Creating full screen quad.")
  val (fullscrenVAO, fullscrenVBO, fullscrenIndicies, fullscreenVBOUV, fullscrenIndicesCount) = initFullscreenQuad()

  println("Loading textures")
  val textures = loadTextures

  def loadTextures = {
    val normalTexId = TextureUtil.loadPNGTexture("res/sprite1-normal.png", 0)
    val texId = TextureUtil.loadPNGTexture("res/sprite1-color.png", 0)
    val specularTexId = TextureUtil.loadPNGTexture("res/sprite1-specular.png", 0)
    val heightmapTexId = TextureUtil.loadPNGTexture("res/sprite1-height.png", 0)
    List(normalTexId, texId, specularTexId, heightmapTexId)
  }


  println("Sprite Map")
  val sprites16x16 = new SpriteMap(16, 16)

  println("generating gbuffer textures")
  val fboEnabled = GLContext.getCapabilities().GL_EXT_framebuffer_object
  checkError("getting capabilities")
  val gbuffer1 = TextureUtil.generateTexture(w, h, 0)
  val gbuffer2 = TextureUtil.generateTexture(w, h, 0)
  val gbuffer3 = TextureUtil.generateTexture(w, h, 0)
  checkError("Gbuffers done?")
  val fbo: Int = initFramebuffer(Array(gbuffer1, gbuffer2, gbuffer3))

  val programOne = compileShaders("screen1.vert", "screen1.frag")
  val programGBuffer = compileShaders("gpass.vert", "gpass.frag")
  makeQuad()
  initTiles()

  while (!Display.isCloseRequested && !close) {
    x = x + 1
    t += 0.002f
    drawWithShader()
    Display.update()
  }

  clean()

  def initFramebuffer(textureIDs: Array[Texture]): Int = {
    val attachments = Array[Int](GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3, GL_COLOR_ATTACHMENT4, GL_COLOR_ATTACHMENT5, GL_COLOR_ATTACHMENT6, GL_COLOR_ATTACHMENT7)
    val buffer =
      if (fboEnabled) {
        val buffer = ByteBuffer.allocateDirect(1 * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
        GL30.glGenFramebuffers(buffer)
        checkError("InitFBO generate framebuffer")
        buffer.get()
      } else {
        println("framebuffers not supported")
        close = true
        -1
      }
    println("buffer: " + buffer)

    for (i <- 0 until textureIDs.length.min(8)) {
      GL30.glBindFramebuffer(GL_FRAMEBUFFER, buffer)
      checkError("InitFBO binding framebuffer")
      GL11.glBindTexture(GL_TEXTURE_2D, textureIDs(i).id)
      checkError("InitFBO binding texture")
      GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, attachments(i),
        GL11.GL_TEXTURE_2D, textureIDs(i).id, 0)
      checkError("InitFBO using texture2D")
    }


    checkError("InitFBO should be done")

    glBindFramebuffer(GL_FRAMEBUFFER, fbo)
    val e = glCheckFramebufferStatus(GL_FRAMEBUFFER)
    if (e != GL_FRAMEBUFFER_COMPLETE)
      println("There is a problem with the FBO")

    GL30.glBindFramebuffer(GL_FRAMEBUFFER, 0)

    buffer
  }

  def initSampler() = {
    val sampler = glGenSamplers()
    glSamplerParameteri(sampler, GL_TEXTURE_WRAP_S, GL_REPEAT)
    glSamplerParameteri(sampler, GL_TEXTURE_WRAP_T, GL_REPEAT)
    glSamplerParameteri(sampler, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glSamplerParameteri(sampler, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    checkError("Creating sampler.")
    sampler
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
    GL15.glDeleteBuffers(vbocId)

    // Delete the index VBO
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
    GL15.glDeleteBuffers(vboiId)

    // Delete the VAO
    GL30.glBindVertexArray(0)
    GL30.glDeleteVertexArrays(vaoId)

    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    glDeleteBuffers(fullscrenVBO)
    glDeleteBuffers(fullscreenVBOUV)
    glDeleteBuffers(fullscrenIndicies)

    glDeleteVertexArrays(fullscrenVAO)

    programGBuffer.destroy()
    programOne.destroy()

    glBindTexture(GL_TEXTURE_2D, 0)
    textures.foreach(_.destroy())

    gbuffer1.destroy()
    gbuffer2.destroy()
    gbuffer3.destroy()

    glDeleteSamplers(sampler)

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
    checkError("Creating fullscren VBO")

    val fullscreenVBOUV = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, fullscreenVBOUV)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, uvBuffer, GL15.GL_STATIC_DRAW)
    GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 0, 0)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
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

  def setTextureUniform(program: Int, name: String, textureID: Int, index: Int) {
    val location = glGetUniformLocation(program, name)
    glUniform1i(location, index)
    glActiveTexture(GL_TEXTURE0 + index)
    checkError("Active texture " + index)
    GL11.glBindTexture(GL_TEXTURE_2D, textureID)
    checkError("Binding texture " + textureID)
    GL33.glBindSampler(index, sampler)
    checkError("Binding sampler")
  }

  def drawWithShader() {
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
    checkError("clearing color buffer")

    ARBShaderObjects.glUseProgramObjectARB(programGBuffer.program)
    checkError("Using g buffer program")

    val locTime: Int = GL20.glGetUniformLocation(programGBuffer.program, "time")
    GL20.glUniform1f(locTime, t)

    checkError("Setting time uniform")

    val screenInfo = GL20.glGetUniformLocation(programGBuffer.program, "screen")
    GL20.glUniform4f(screenInfo, w, h, 0f, 0f)

    checkError("Setting screen uniform")

    val uvScalars = GL20.glGetUniformLocation(programGBuffer.program, "uvScalars")
    GL20.glUniform2f(uvScalars, sprites16x16.sizeWidth, sprites16x16.sizeHeight)

    checkError("Setting uvScalars uniform")

    val names = List("norm", "tex", "specular", "heightMap")

    for (i <- 0 until 4) {
      setTextureUniform(programGBuffer.program, names(i), textures(i).id, i * 2)
    }
    checkError("setting texture uniforms")


    // Bind to the VAO that has all the information about the quad vertices
    GL30.glBindVertexArray(vaoId)
    checkError("Binding vertex array vaoID")

    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId)
    checkError("Binding indicis for quads")

    glBindFramebuffer(GL_FRAMEBUFFER, fbo)
    checkError("Binding framebuffer")

    val indi = Array[Int](GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2)
    val indibuffer = bufferUtil(indi)
    glDrawBuffers(indibuffer)

    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)
    checkError("Clearing framebuffer")
    GL11.glDisable(GL11.GL_DEPTH_TEST)
    checkError("Disable depth test (again)")

    val posLocation = GL20.glGetUniformLocation(programGBuffer.program, "position")
    val uvPosition = GL20.glGetUniformLocation(programGBuffer.program, "uvPosition")

    for (tile <- tileManager.tiles) {
      val (i, j) = tile.texture
      val (x, y) = tile.pos
      GL20.glUniform2f(uvPosition, i, j)
      GL20.glUniform2f(posLocation, x, y)
      GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_BYTE, 0)
    }

    checkError("Drawing tiles")

    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)

    ARBShaderObjects.glUseProgramObjectARB(programOne.program)

    // bind the gbuffer to a texture
    setTextureUniform(programOne.program, "g1", gbuffer1.id, 0)
    setTextureUniform(programOne.program, "g2", gbuffer2.id, 2)
    setTextureUniform(programOne.program, "g3", gbuffer3.id, 4)

    GL30.glBindVertexArray(fullscrenVAO)
    checkError("Binding fullscreen VAO")
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, fullscrenIndicies)
    checkError("Bind indices for fullscreen vbo")
    GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_BYTE, 0)
    checkError("Done drawing fullscreen quad")
    // Put everything back to default (deselect)
    //  GL20.glDisableVertexAttribArray(0)
    //  GL20.glDisableVertexAttribArray(1)
    //  GL20.glDisableVertexAttribArray(2)
    GL30.glBindVertexArray(0)

    ARBShaderObjects.glUseProgramObjectARB(0)
  }

  def checkError(msg: String) {
    val e = glGetError()
    if (e == GL_INVALID_ENUM) {
      println(msg + " :" + " invalid enum")
      close = true
    }
    if (e == GL_INVALID_VALUE) {
      println(msg + " :" + " invalid value")
      close = true
    }
    if (e == GL_INVALID_OPERATION) {
      println(msg + " :" + " invalid operation")
      close = true
    }
    if (e == GL_INVALID_FRAMEBUFFER_OPERATION) {
      println(msg + " :" + " invalid framebuffer not done")
      close = true
    }
    if (e == GL_OUT_OF_MEMORY) {
      println(msg + " :" + " Out of memory!")
      close = true
    }
  }

  def initDisplay() {
    val pf = new PixelFormat()
    val ca = new ContextAttribs(3, 3).withForwardCompatible(true).withProfileCore(true)

    Display.setDisplayMode(new DisplayMode(w, h))
    Display.setTitle("2d light")
    Display.create(pf, ca)
    Display.sync(60)
    GL11.glDisable(GL11.GL_DEPTH_TEST)
    checkError("Disable depth test")
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

  def compileShaders(vertex: String, fragment: String): ShaderProgram = {
    val none = new ShaderProgram(0, 0, 0)

    val vertShader = ShaderUtil.createShader("shaders/" + vertex, ARBVertexShader.GL_VERTEX_SHADER_ARB)
    val fragShader = ShaderUtil.createShader("shaders/" + fragment, ARBFragmentShader.GL_FRAGMENT_SHADER_ARB)

    val program = ARBShaderObjects.glCreateProgramObjectARB

    if (program == 0) return none

    ARBShaderObjects.glAttachObjectARB(program, vertShader)
    ARBShaderObjects.glAttachObjectARB(program, fragShader)
    ARBShaderObjects.glLinkProgramARB(program)

    if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
      System.err.println(ShaderUtil.getLogInfo(program))
      return none
    }
    ARBShaderObjects.glValidateProgramARB(program)

    if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
      System.err.println(ShaderUtil.getLogInfo(program))
      return none
    }
    val vertexShader = vertShader
    val fragmentShader = fragShader
    new ShaderProgram(program, vertexShader, fragmentShader)
  }

  def bufferUtil(values: Array[Int]) = {
    val b = BufferUtils.createIntBuffer(values.length)
    b.put(values)
    b.flip()
    b
  }

  def bufferUtil(values: Array[Float]) = {
    val b = BufferUtils.createFloatBuffer(values.length)
    b.put(values)
    b.flip()
    b
  }
}