package lightmind

import org.lwjgl.opengl.{GL11, ARBShaderObjects}
import java.io.{InputStreamReader, BufferedReader, FileInputStream}

/**
 * Created by Lukas on 02-07-14.
 */
object ShaderUtil {
  def createShader(filename: String, shaderType: Int): Int = {
    var shader: Int = 0
    try {
      shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType)
      if (shader == 0) return 0
      ARBShaderObjects.glShaderSourceARB(shader, readFileAsString(filename))
      ARBShaderObjects.glCompileShaderARB(shader)
      if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE) throw new RuntimeException("Error creating shader: " + getLogInfo(shader))
      return shader
    }
    catch {
      case exc: Exception => {
        ARBShaderObjects.glDeleteObjectARB(shader)
        throw exc
      }
    }
  }

  private def readFileAsString(filename: String): String = {
    val source: StringBuilder = new StringBuilder
    val in: FileInputStream = new FileInputStream(filename)
    var exception: Exception = null
    var reader: BufferedReader = null
    try {
      reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))
      var innerExc: Exception = null
      try {
        var line: String = null
        while ((({
          line = reader.readLine;
          line
        })) != null) source.append(line).append('\n')
      }
      catch {
        case exc: Exception => {
          exception = exc
        }
      }
      finally {
        try {
          reader.close
        }
        catch {
          case exc: Exception => {
            if (innerExc == null) innerExc = exc
            else exc.printStackTrace
          }
        }
      }
      if (innerExc != null) throw innerExc
    }
    catch {
      case exc: Exception => {
        exception = exc
      }
    }
    finally {
      try {
        in.close
      }
      catch {
        case exc: Exception => {
          if (exception == null) exception = exc
          else exc.printStackTrace
        }
      }
      if (exception != null) throw exception
    }
    return source.toString
  }

  def getLogInfo(obj: Int): String = {
    return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB))
  }

}
