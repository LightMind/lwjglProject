package lightmind

import org.lwjgl.input.Mouse
import org.lwjgl.opengl._
import org.lwjgl.BufferUtils
import org.lwjgl.util.glu.GLU

object Mind extends App {
  var x = 0
  val w = 1280
  val h = 720
  var t = 1.0f
  var program = 0
  var useShader = false
  var vaoId = 0
  var vboId = 0
  var vbocId = 0
  var vboiId = 0
  var vbouvId = 0
  var vertexCount = 0
  var indicesCount = 0
  var normalTexId = 0
  var texId = 0
  var specularTexId = 0
  var vertexShader = 0
  var fragmentShader = 0

  val scalarw = w / 64
  val scalarh = h / 64

  val ws = 64f
  val hs = 64f

  val adj = 0.0f

  initDisplay()

  normalTexId = TextureUtil.loadPNGTexture("res/sprite1-normal.png", 0)
  texId = TextureUtil.loadPNGTexture("res/sprite1-color.png", 0)
  specularTexId = TextureUtil.loadPNGTexture("res/sprite1-specular.png", 0)

  val sprites16x16 = new SpriteMap(16, 16)

  compileShaders()
  makeQuad()

  while (!Display.isCloseRequested) {
    x = x + 1
    t += 0.002f
    drawWithShader()
    Display.update()
  }

  clean()

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
    GL20.glDetachShader(program, vertexShader)
    GL20.glDetachShader(program, fragmentShader)

    GL20.glDeleteShader(vertexShader)
    GL20.glDeleteShader(fragmentShader)
    GL20.glDeleteProgram(program)

    Display.destroy()
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
      ARBShaderObjects.glUseProgramObjectARB(program)

      val locTime: Int = GL20.glGetUniformLocation(program, "time")
      GL20.glUniform1f(locTime, t)

      val screenInfo = GL20.glGetUniformLocation(program, "screen")
      GL20.glUniform4f(screenInfo, w, h, 0f, 0f)

      val lights = GL20.glGetUniformLocation(program, "lights")
      val (l, c) = getLights()
      GL20.glUniform2(lights, l)

      val uvScalars = GL20.glGetUniformLocation(program, "uvScalars")
      GL20.glUniform2f(uvScalars, sprites16x16.sizeWidth, sprites16x16.sizeHeight)

      val normLocation = GL20.glGetUniformLocation(program, "norm")
      val texLocation = GL20.glGetUniformLocation(program, "tex")
      val specLocation = GL20.glGetUniformLocation(program, "specular")

      GL20.glUniform1i(normLocation, 0)
      GL20.glUniform1i(texLocation, 2)
      GL20.glUniform1i(specLocation, 4)

      GL13.glActiveTexture(GL13.GL_TEXTURE0 + 0)
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalTexId)
      GL33.glBindSampler(0, GL11.GL_NEAREST)

      GL13.glActiveTexture(GL13.GL_TEXTURE0 + 2)
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId)
      GL33.glBindSampler(2, GL11.GL_NEAREST)

      GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4)
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, specularTexId)
      GL33.glBindSampler(4, GL11.GL_NEAREST)
    }

    // Bind to the VAO that has all the information about the quad vertices
    GL30.glBindVertexArray(vaoId)

    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId)

    val posLocation = GL20.glGetUniformLocation(program, "position")
    val uvPosition = GL20.glGetUniformLocation(program, "uvPosition")

    for (i <- 0 until scalarw.toInt) {
      for (j <- 0 until scalarh.toInt) {
        GL20.glUniform2f(uvPosition, 2f, 0f)
        GL20.glUniform2f(posLocation, i * ws, j * hs)
        GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_BYTE, 0)
      }
    }

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

  def compileShaders() {
    var vertShader: Int = 0
    var fragShader: Int = 0


    try {
      vertShader = ShaderUtil.createShader("shaders/screen.vert", ARBVertexShader.GL_VERTEX_SHADER_ARB)
      fragShader = ShaderUtil.createShader("shaders/screen.frag", ARBFragmentShader.GL_FRAGMENT_SHADER_ARB)
    }
    catch {
      case exc: Exception => {
        exc.printStackTrace
        return
      }
    }
    finally {
      if (vertShader == 0 || fragShader == 0) return
    }

    program = ARBShaderObjects.glCreateProgramObjectARB

    if (program == 0) return

    ARBShaderObjects.glAttachObjectARB(program, vertShader)
    ARBShaderObjects.glAttachObjectARB(program, fragShader)
    ARBShaderObjects.glLinkProgramARB(program)

    GL20.glBindAttribLocation(program, 0, "in_Position")
    GL20.glBindAttribLocation(program, 1, "in_Color")

    if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
      System.err.println(ShaderUtil.getLogInfo(program))
      return
    }
    ARBShaderObjects.glValidateProgramARB(program)

    if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
      System.err.println(ShaderUtil.getLogInfo(program))
      return
    }
    useShader = true
    vertexShader = vertShader
    fragmentShader = fragShader
  }
}
