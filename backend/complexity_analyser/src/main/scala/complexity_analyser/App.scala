package complexity_analyser

import java.io.File

import json_parser.MicroServiceInputParser

import scala.io.Source

/**
  * Simple main to check the access to resources
  */
object App {
  def main(args: Array[String]) = {
    val mi = MicroServiceInputParser.parseFile(new File(args apply 0))
    val language = mi.getLanguage
    val modelAnswer = new File(mi.getModelAnswer)
    val inputPath = new File(mi.getPath)
    processLanguage(language, modelAnswer, inputPath)

  }
  def processLanguage(language: String, modelAnswer: File, inputPath: File): Unit = {
    language match {
      case "haskell" =>
        val h = new HaskellProcessor(modelAnswer, inputPath)
        h.prepare()
      case _ => throw new IllegalArgumentException("Wrong language")
    }
  }
}