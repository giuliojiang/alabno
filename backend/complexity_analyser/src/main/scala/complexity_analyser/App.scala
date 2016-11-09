package complexity_analyser

import java.io.File

import scala.sys.process.{ProcessLogger, _}
import json_parser.{Error, MicroServiceInputParser, MicroServiceOutputParser}
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

object App {

  private var path: File = _
  private var score: Int = 100
  val annotations = new ArrayBuffer[Error]
  val errorList = new ArrayBuffer[String]

  def main(args: Array[String]) {
    if (args.length != 2) {
      throw new IllegalArgumentException("Complexity <input json> ")
    }
    val parser = MicroServiceInputParser.parseFile(new File(args.apply(0)))

    path = new File(parser.getPath)
    val language = parser.getLanguage
    if (complexity(language, path) != 0) {
      score = 0
    }

    MicroServiceOutputParser.writeFile(new File(args.apply(1)), score,
      annotations.asJava, errorList.asJava)
  }

  private def complexity(language: String, path: File): Int = language match {
    case "haskell" =>
      runBench(path)
      1

    case "java" =>
      0

    case _ => throw new NotImplementedException
  }

  def runBench(path: File) = {
    val lines = new ArrayBuffer[String]()
    val exit = s"ghc -i$path/IC -i$path --make -O Bench -main-is Bench" !
      ProcessLogger(line => lines.append(line))
    print(exit)
  }
}