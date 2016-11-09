package complexity_analyser

import java.io.File
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.Files.copy

class HaskellProcessor(modelAnswer: File, studentSubmission: File) {
  private final val BENCH_NAME = "/Bench.hs"

  /**
    * Copies Bench.hs to both model solution and student submission
    */
  def prepare(): Unit = {
    val bench = new File(getClass.getResource(BENCH_NAME).toURI)
    if (!bench.exists()) throw new Exception("Missing resource Bench.hs")
    if (!modelAnswer.isDirectory) throw new Exception("Model solution should be a directory")
    if (!studentSubmission.isDirectory) throw new Exception("Student submission should be a directory")
    val mod =new File(modelAnswer.toPath.toString + BENCH_NAME).toPath
    val stud = new File(studentSubmission.toPath.toString + BENCH_NAME).toPath
    copy(bench.toPath, mod, REPLACE_EXISTING)
    copy(bench.toPath, stud, REPLACE_EXISTING)
  }
}
