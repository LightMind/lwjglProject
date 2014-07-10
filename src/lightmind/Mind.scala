package lightmind

import java.nio.{ByteBuffer, ByteOrder}

import lightmind._
import lightmind.opengl._
import lightmind.terrain.TileManager
import org.lwjgl.BufferUtils
import org.lwjgl.input.Mouse
import org.lwjgl.opengl._
import org.lwjgl.util.glu.GLU

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL14._
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

  val circleVAO = GeometryUtil.initCircleQuad(16)
  val fullscreenVAO = GeometryUtil.initFullscreenQuad()
  val quadVAO = GeometryUtil.makeQuad(ws, hs, adj, sprites16x16)

  val textures = loadTextures

  val fboEnabled = GLContext.getCapabilities().GL_EXT_framebuffer_object
  checkError("getting capabilities")
  val gbuffer1 = TextureUtil.generateTexture(w, h, 0)
  val gbuffer2 = TextureUtil.generateTexture(w, h, 0)
  val gbuffer3 = TextureUtil.generateTexture(w, h, 0)
  checkError("Gbuffers done?")
  val gbufferFBO: Int = initFramebuffer(Array(gbuffer1, gbuffer2, gbuffer3))

  val lightAccumulation = TextureUtil.generateTexture(w, h, 0)
  val lightAccumulationFBO = initFramebuffer(Array(lightAccumulation))

  val programTwo = ShaderUtil.compileShaders("screen2.vert", "screen2.frag")
  val programOne = ShaderUtil.compileShaders("screen1.vert", "screen1.frag")
  val programGBuffer = ShaderUtil.compileShaders("gpass.vert", "gpass.frag")

  initTiles()

  while (!Display.isCloseRequested && !close) {
    t += 0.002f
    drawWithShader()
    Display.update()
    Thread.sleep(15)
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

    glBindFramebuffer(GL_FRAMEBUFFER, gbufferFBO)
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
        tileManager.createTile((i, j), (2, 0))
      }
    }
  }

  def clean() {
    quadVAO.destroy()
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
    val my = Mouse.getY

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
    GL30.glBindVertexArray(quadVAO.id)
    checkError("Binding vertex array vaoID")

    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, quadVAO.indices)
    checkError("Binding indicis for quads")

    glBindFramebuffer(GL_FRAMEBUFFER, gbufferFBO)
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
      GL11.glDrawElements(GL11.GL_TRIANGLES, quadVAO.indicesCount, GL11.GL_UNSIGNED_BYTE, 0)
    }

    checkError("Drawing tiles")

    // using g buffers for light accumulation
    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, lightAccumulationFBO)
    glClear(GL_COLOR_BUFFER_BIT)
    glUseProgram(programOne.program)

    glEnable(GL_BLEND)
    glBlendFunc(GL_ONE, GL_ONE)

    val indi2 = Array[Int](GL_COLOR_ATTACHMENT0)
    val indibuffer2 = toBuffer(indi2)
    glDrawBuffers(indibuffer2)

    // bind the gbuffer to a texture
    setTextureUniform(programOne.program, "g1", gbuffer1.id, 0)
    setTextureUniform(programOne.program, "g2", gbuffer2.id, 2)
    setTextureUniform(programOne.program, "g3", gbuffer3.id, 4)

    val positionOne = glGetUniformLocation(programOne.program, "position")
    val screenOne = glGetUniformLocation(programOne.program, "screen")
    val radius = glGetUniformLocation(programOne.program, "radius")
    val lightColor = glGetUniformLocation(programOne.program, "lightColor")
    val lightIntensity = glGetUniformLocation(programOne.program, "lightIntensity")

    glUniform2f(positionOne, Mouse.getX, h - Mouse.getY)
    glUniform4f(screenOne, w, h, 0, 0)
    glUniform1f(radius, 300)
    glUniform3f(lightColor, 1.0f, 1.0f, 1.0f)
    glUniform1f(lightIntensity, 1.5f)

    // bind circle vao,render lights.
    GL30.glBindVertexArray(circleVAO.id)
    checkError("Binding circle VAO")
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, circleVAO.indices)
    checkError("Bind indices for circle vbo")
    GL11.glDrawElements(GL11.GL_TRIANGLE_FAN, circleVAO.indicesCount, GL_UNSIGNED_BYTE, 0)
    checkError("Done drawing fullscreen quad")


    glUniform4f(screenOne, w, h, 0, 0)
    glUniform1f(radius, 150)

    glUniform1f(lightIntensity, 1.3f)

    for (i <- 100 until w by 200) {
      for (j <- 100 until h by 200) {
        glUniform2f(positionOne, i, j)
        glUniform3f(lightColor, i.toFloat / w, j.toFloat / h, 1.0f)
        GL11.glDrawElements(GL11.GL_TRIANGLE_FAN, circleVAO.indicesCount, GL_UNSIGNED_BYTE, 0)
      }
    }


    // Show light accumulation in fullscreen
    glDisable(GL_BLEND)
    glBindFramebuffer(GL_FRAMEBUFFER, 0)
    glUseProgram(programTwo.program)

    setTextureUniform(programTwo.program, "lightBuffer", lightAccumulation.id, 0)
    setTextureUniform(programTwo.program, "diffuseBuffer", gbuffer1.id, 2)

    GL30.glBindVertexArray(fullscreenVAO.id)
    checkError("Binding circle VAO")
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, fullscreenVAO.indices)
    checkError("Bind indices for circle vbo")
    GL11.glDrawElements(GL11.GL_TRIANGLES, fullscreenVAO.indicesCount, GL11.GL_UNSIGNED_BYTE, 0)
    checkError("Done drawing fullscreen quad")

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