package lightmind

import java.io.{BufferedReader, FileInputStream, InputStreamReader}

import org.lwjgl.opengl.GL11
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

/**
 * Created by Lukas on 02-07-14.
 */
object ShaderUtil {
  def createShader(filename: String, shaderType: Int): Int = {
    var shader: Int = 0
    try {

      shader = glCreateShader(shaderType)
      if (shader == 0) return 0
      glShaderSource(shader, readFileAsString(filename))
      glCompileShader(shader)

      if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL11.GL_FALSE) throw new RuntimeException("Error creating shader: " + getShaderLogInfo(shader))
      return shader
    }
    catch {
      case exc: Exception => {
        glDeleteShader(shader)
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

  def getShaderLogInfo(obj: Int): String = {
    return glGetShaderInfoLog(obj, GL_INFO_LOG_LENGTH)
  }

  def getProgramLogInfo(obj: Int): String = {
    return glGetProgramInfoLog(obj, GL_INFO_LOG_LENGTH)
  }

}
