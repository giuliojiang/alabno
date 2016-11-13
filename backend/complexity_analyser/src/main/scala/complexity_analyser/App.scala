package complexity_analyser

import java.io.File

import scala.collection.JavaConverters._
import json_parser.{MicroServiceOutputParser, Error, MicroServiceInputParser}

import scala.collection.mutable.ArrayBuffer


/**
  * Simple main to check the access to resources
  */
object App {
  private val errorList = new ArrayBuffer[String]

  def main(args: Array[String]):Unit = {
    val mi = MicroServiceInputParser.parseFile(new File(args apply 0))
    val language = mi.getLanguage
    val modelAnswer = new File(mi.getModelAnswer)
    val inputPath = new File(mi.getPath)
    val (annotations, score) = processLanguage(language, modelAnswer, inputPath)

    MicroServiceOutputParser.writeFile(new File(args apply 1), score,
      annotations.asJava, errorList.asJava)
  }

  def processLanguage(language: String, modelAnswer: File, inputPath: File) = {
    language match {
      case "haskell" =>
        val h = new HaskellProcessor(modelAnswer, inputPath)
        h.prepare()
        h.runTests()
        h.runBench()
      case _ => throw new IllegalArgumentException("Wrong language")
    }
  }
}