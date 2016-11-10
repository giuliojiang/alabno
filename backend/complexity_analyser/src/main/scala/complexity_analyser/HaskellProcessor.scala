package complexity_analyser

import java.io.File
import java.nio.file.Files.copy
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

import scala.collection.mutable.ArrayBuffer
import scala.sys.process._

class HaskellProcessor(modelAnswer: File, studentSubmission: File) {
  private final val BENCH_NAME = "/Bench.hs"
  private final val MATCH_BMARK = "benchmarking tests/[a-zA-Z0-9]+".r
  private final val MATCH_MEAN = "[0-9]+\\.[0-9]+".r
  private final def benchFlags(o: File) = s"--regress cycles:time --output=$o/res.html"

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
      val (name, modY) = e
      val (_, studY) = zippedMeanStud.apply(i)
      buff += ((name, modY - studY))
    }
    buff
  }

  private def genListBenchNameMean(outcome: String) = {
    val names = MATCH_BMARK.findAllMatchIn(outcome).map(_.toString())
    val details = MATCH_BMARK.split(outcome)
    val means = details.flatMap(_.split("\n")).filter(_.trim.startsWith("y"))
    val doubles = means.map(e => MATCH_MEAN.findFirstIn(e).get.toDouble)
    names.toSeq.zip(doubles)
  }

  def runBench() = {
    compileClassOnBoth("Bench")
    val benchOutcomeStudent = s"$studentSubmission/Bench ${benchFlags(studentSubmission)}" !!
    val benchOutcomeModel = s"$modelAnswer/Bench ${benchFlags(modelAnswer)}" !!
    val zippedMeanModel = genListBenchNameMean(benchOutcomeModel)
    val zippedMeanStud = genListBenchNameMean(benchOutcomeStudent)
    produceDelta(zippedMeanModel, zippedMeanStud).foreach(println)
  }

}