package complexity_analyser

import java.io.File

import scala.collection.JavaConverters._
import json_parser.{MicroServiceOutputParser, Error, MicroServiceInputParser}

import scala.collection.mutable.ArrayBuffer


/**
  * Simple main to check the access to resources
  */
object App {

  val annotations = new ArrayBuffer[Error]
  val errorList = new ArrayBuffer[String]
  var score = 100

  def main(args: Array[String]):Unit = {
    val mi = MicroServiceInputParser.parseFile(new File(args apply 0))
    val language = mi.getLanguage
    val modelAnswer = new File(mi.getModelAnswer)
    val inputPath = new File(mi.getPath)
    processLanguage(language, modelAnswer, inputPath)

    MicroServiceOutputParser.writeFile(new File(args apply 1), score,
      annotations.asJava, errorList.asJava)
  }

  def processLanguage(language: String, modelAnswer: File, inputPath: File): Unit = {
    language match {
      case "haskell" =>
        val h = new HaskellProcessor(modelAnswer, inputPath)
        h.prepare()
        h.runTests()
        val hBench = h.runBench()
        annotations ++= hBench._1
        score = hBench._2
      case _ => throw new IllegalArgumentException("Wrong language")
    }
  }
}