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
  val w = 1280
  val h = 720
  var t = 1.0f
  var close = false

  val scalarw = w / 64
  val scalarh = h / 64

  val ws = 64f
  val hs = 64f

  val adj = 0.0f

  val tileManager = new TileManager()
  val r = new Random()

  initDisplay()

  val sampler = initSampler()

  val sprites16x16 = new SpriteMap(16, 16)

  val fullscreenVAO = GeometryUtil.initFullscreenQuad()
  val (vaoId, vboId, vertexCount, vbouvId, vboiId, indicesCount) = GeometryUtil.makeQuad(ws, hs, adj, sprites16x16)

  val textures = loadTextures

  val fboEnabled = GLContext.getCapabilities().GL_EXT_framebuffer_object
  checkError("getting capabilities")
  val gbuffer1 = TextureUtil.generateTexture(w, h, 0)
  val gbuffer2 = TextureUtil.generateTexture(w, h, 0)
  val gbuffer3 = TextureUtil.generateTexture(w, h, 0)
  checkError("Gbuffers done?")
  val fbo: Int = initFramebuffer(Array(gbuffer1, gbuffer2, gbuffer3))
  val programOne = ShaderUtil.compileShaders("screen1.vert", "screen1.frag")
  val programGBuffer = ShaderUtil.compileShaders("gpass.vert", "gpass.frag")

  initTiles()

  while (!Display.isCloseRequested && !close) {
    t += 0.002f
    drawWithShader()
    Display.update()
  }

  clean()

  def loadTextures = {
    val normalTexId = TextureUtil.loadPNGTexture("res/sprite1-normal.png", 0)
    val texId = TextureUtil.loadPNGTexture("res/sprite1-color.png", 0)
    val specularTexId = TextureUtil.loadPNGTexture("res/sprite1-specular.png", 0)
    val heightmapTexId = TextureUtil.loadPNGTexture("res/sprite1-height.png", 0)
    List(normalTexId, texId, specularTexId, heightmapTexId)
  }

  def quit() {
    close = true
  }

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

    // Delete the index VBO
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
    GL15.glDeleteBuffers(vboiId)

    // Delete the VAO
    GL30.glBindVertexArray(0)
    GL30.glDeleteVertexArrays(vaoId)

    fullscreenVAO.destroy()

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
    val lightBuffer = toBuffer(lights)

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

    glUseProgram(programGBuffer.program)
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
    val indibuffer = toBuffer(indi)
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

    glUseProgram(programOne.program)

    // bind the gbuffer to a texture
    setTextureUniform(programOne.program, "g1", gbuffer1.id, 0)
    setTextureUniform(programOne.program, "g2", gbuffer2.id, 2)
    setTextureUniform(programOne.program, "g3", gbuffer3.id, 4)

    GL30.glBindVertexArray(fullscreenVAO.id)
    checkError("Binding fullscreen VAO")
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, fullscreenVAO.indices)
    checkError("Bind indices for fullscreen vbo")
    GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_BYTE, 0)
    checkError("Done drawing fullscreen quad")
    // Put everything back to default (deselect)
    //  GL20.glDisableVertexAttribArray(0)
    //  GL20.glDisableVertexAttribArray(1)

    GL30.glBindVertexArray(0)

    glUseProgram(0)
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
    println("OpenGL version: " + GL11.glGetString(GL11.GL_VERSION))
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

}