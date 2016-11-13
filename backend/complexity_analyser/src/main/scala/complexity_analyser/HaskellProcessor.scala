package complexity_analyser

import java.io.File
import java.nio.file.Files.copy
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

import json_parser.Error

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.sys.process._

class HaskellProcessor(modelAnswer: File, studentSubmission: File) {
  private final val NUMBER = "[0-9]+".r
  private final val BENCH_NAME = "/Bench.hs"
  private final val MATCH_BMARK = """benchmarking tests/(\w+)""".r
  private final val MATCH_MEAN = s"$NUMBER\\.$NUMBER+".r
  private final val TestScore = new mutable.HashMap[String, Int]

  private final val TestLine = """(\w+\d?): (\d+) / (\d+)""".r
  private final val FunctionMap = new mutable.HashMap[String, (Int, String)]
  private final val FunctionLine = """(\w+\d?)\s+::\s+.+""".r

  /**
    * Copies Bench.hs to both model solution and student submission
    */
  def prepare(): Unit = {
    val bench = new File(getClass.getResource(BENCH_NAME).toURI)
    if (!bench.exists()) throw new Exception("Missing resource Bench.hs")
    if (!modelAnswer.isDirectory) throw new Exception("Model solution should be a directory")
    if (!studentSubmission.isDirectory) throw new Exception("Student submission should be a directory")
    val mod = new File(modelAnswer.toPath.toString + BENCH_NAME)
    val stud = new File(studentSubmission.toPath.toString + BENCH_NAME)
    studentSubmission.listFiles().filter(hFilter).foreach(findFunctions)
    copy(bench.toPath, mod.toPath, REPLACE_EXISTING)
    copy(bench.toPath, stud.toPath, REPLACE_EXISTING)
  }

  def findFunctions(file: File) = {
    for ((l, i) <- Source.fromFile(file).getLines().zipWithIndex) {
      l match {
        case FunctionLine(n) =>
          FunctionMap += ((n, (i + 1, file.toString)))
        case _ => None
      }
    }
  }

  private def hFilter(f: File) = f.isFile && f.getName.endsWith(".hs") && f.getName != "Tests.hs"

  def runTests() = {
    compileClassOnBoth("Tests")
    val testOutcomeStudent = s"$studentSubmission/Tests".!!
    val testOutcomeModel = s"$modelAnswer/Tests".!!
    testOutcomeModel.split("\n").foreach(findMaxScoreHeader)
    calculateTestScores(findStudentScore(testOutcomeStudent))
  }

  private def findMaxScoreHeader(line: String): Unit = {
    line match {
      case TestLine(name, _, max) =>
        TestScore += ((name, Integer.decode(max)))
      case _ => None
    }
  }

  private def findStudentScore(line: String) = {
    val buff = new ArrayBuffer[(String, Integer, Int)]()
    val lines = line.split("\n")
    for (l <- lines) {
      l match {
        case TestLine(name, score, m) =>
          val max = TestScore.getOrElse(name, m.toInt)
          buff += ((name, score.toInt, max))
        case _ => None
      }
    }
    buff
  }

  private def calculateTestScores(testsResult: Seq[(String, Integer, Int)]) = {
    var score = 100
    val buff = new ArrayBuffer[Error]
    for ((name, studScore, maxScore) <- testsResult) {
      if (studScore != maxScore) {
        score -= 10 * (1 - (studScore / maxScore))
        val (line, file) = FunctionMap.getOrElse(name, (0, studentSubmission.getName))
        buff += new Error(s"Student passes $studScore/$maxScore tests for $name", file, line, 0, "tests")
      }
    }
    (buff, score)
  }

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

  private def produceDelta(zippedMeanModel: Seq[(String, Double)], zippedMeanStud: Seq[(String, Double)]) = {
    val buff = new ArrayBuffer[(String, Double)]
    for ((e, i) <- zippedMeanModel.zipWithIndex) {
      val (name, modMean) = e
      val (_, studMean) = zippedMeanStud.apply(i)
      buff += ((name, modMean - studMean))
    }
    buff
  }

  private def genListBenchNameMean(outcome: String) = {
    val names = MATCH_BMARK.findAllMatchIn(outcome).map(_.group(1))
    val details = MATCH_BMARK.split(outcome)
    val means = details.flatMap(_.split("\n")).filter(_.trim.startsWith("mean"))
    val doubles = means.map(convertToNS)
    names.toSeq.zip(doubles)
  }

  private def convertToNS(meanLine: String) = {
    val double = MATCH_MEAN.findFirstIn(meanLine).get.toDouble
    val factor = meanLine match {
      case m if m.contains("ns") => 1
      case m if m.contains("μs") => 1000
      case m if m.contains("ms") => 1000 * 1000
      case m if m.contains(" s") => 1000 * 1000 * 1000
    }
    double * factor
  }

  def calculateScore(deltas: ArrayBuffer[(String, Double)]) = {
    var score = 100
    val annotations = new ArrayBuffer[Error]
    for ((n, v) <- deltas) {
      val diff = Math.abs(v).round
      if (diff > 50) {
        if (score > 0) {
          score -= (v / 8).toInt
        }
        val (line, file) = FunctionMap.getOrElse(n, (0, studentSubmission.getName))
        annotations.append(new Error(s"Function $n is inefficient -> $diff ns diff!",
          file, line, 0, "complexity"))
      }
    }
    (annotations, score)
  }

}
