package complexity_analyser

import java.io.File

import scala.io.Source

/**
  * Simple main to check the access to resources
  */
object App {
  def main(args: Array[String]) = {
    val bench = new File(getClass.getResource("/Bench.hs").toURI)
    for(line <- Source.fromFile(bench).getLines()) {
      println(line)
    }
  }
}
