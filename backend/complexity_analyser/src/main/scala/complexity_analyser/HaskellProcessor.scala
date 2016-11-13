package complexity_analyser

import java.io.File
import java.nio.file.Files.copy
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

import json_parser.Error

import scala.collection.mutable.ArrayBuffer
import scala.sys.process._

class HaskellProcessor(modelAnswer: File, studentSubmission: File) {
  private final val BENCH_NAME = "/Bench.hs"
  private final val MATCH_BMARK = "benchmarking tests/[a-zA-Z0-9]+".r
  private final val MATCH_MEAN = "[0-9]+\\.[0-9]+".r

  /**
    * Copies Bench.hs to both model solution and student submission
    */
  def prepare(): Unit = {
    val bench = new File(getClass.getResource(BENCH_NAME).toURI)
    if (!bench.exists()) throw new Exception("Missing resource Bench.hs")
    if (!modelAnswer.isDirectory) throw new Exception("Model solution should be a directory")
    if (!studentSubmission.isDirectory) throw new Exception("Student submission should be a directory")
    val mod = new File(modelAnswer.toPath.toString + BENCH_NAME).toPath
    val stud = new File(studentSubmission.toPath.toString + BENCH_NAME).toPath
    copy(bench.toPath, mod, REPLACE_EXISTING)
    copy(bench.toPath, stud, REPLACE_EXISTING)
  }

  def runTests() = {
    compileClassOnBoth("Tests")
    val testOutcomeStudent = s"$studentSubmission/Tests".!!
    val testOutcomeModel = s"$modelAnswer/Tests".!!
    testOutcomeModel.equals(testOutcomeStudent)
  }

  def runBench() = {
    compileClassOnBoth("Bench")
    val benchOutcomeStudent = s"$studentSubmission/Bench ${benchFlags(studentSubmission)}" !!
    val benchOutcomeModel = s"$modelAnswer/Bench ${benchFlags(modelAnswer)}" !!
    val zippedMeanModel = genListBenchNameMean(benchOutcomeModel)
    val zippedMeanStud = genListBenchNameMean(benchOutcomeStudent)
    val deltas = produceDelta(zippedMeanModel, zippedMeanStud)
    calculateScore(deltas)
  }

  private final def benchFlags(o: File) = s"--output=$o/res.html"

  private def compileClassOnBoth(name: String) = {
    val linesModel = new ArrayBuffer[String]()
    val linesStudent = new ArrayBuffer[String]
    val exitModel = s"ghc -i$modelAnswer/IC -i$modelAnswer --make -O $name -main-is $name" !
      ProcessLogger(line => linesModel.append(line))
    val exitStudent = s"ghc -i$studentSubmission/IC -i$studentSubmission " +
      s"--make -O $name -main-is $name" !
      ProcessLogger(line => linesStudent.append(line))
    if (exitModel != 0 || exitStudent != 0) throw new Exception(s"Student or Model " +
      s"$name solution didn't compile")
    (linesModel, linesStudent)
  }

  private def produceDelta(zippedMeanModel: Seq[(String, Double)], zippedMeanStud: Seq[(String, Double)]) = {
    val buff = new ArrayBuffer[(String, Double)]
    for ((e, i) <- zippedMeanModel.zipWithIndex) {
      val (name, modMean) = e
      val (_, studMean) = zippedMeanStud.apply(i)
      buff += ((name, modMean - studMean))
    }
    buff
  }

  private def convertToNS(meanLine: String) = {
    val double = MATCH_MEAN.findFirstIn(meanLine).get.toDouble
    val factor = meanLine match {
      case m if m.contains("ns") => 1
      case m if m.contains("μs") => 1000
      case m if m.contains("ms") => 1000 * 1000
      case m if m.contains("s")  => 1000 * 1000 * 1000
    }
    double * factor
  }

  private def genListBenchNameMean(outcome: String) = {
    println(outcome)
    val names = MATCH_BMARK.findAllMatchIn(outcome).map(_.toString())
    val details = MATCH_BMARK.split(outcome)
    val means = details.flatMap(_.split("\n")).filter(_.trim.startsWith("mean"))
    val doubles = means.map(convertToNS)
    names.toSeq.zip(doubles)
  }

  def calculateScore(deltas: ArrayBuffer[(String, Double)]) = {
    var score = 100
    val annotations = new ArrayBuffer[Error]
    for ((n, v) <- deltas) {
      val diff = Math.abs(v).round
      if (diff > 50) {
        score -= (v / 8).toInt
        annotations.append(new Error(s"Function $n is inefficient -> $diff ns diff!",
          studentSubmission.getName, 0, 0, "complexity"))
      }
    }
    (annotations, score)
  }

}
