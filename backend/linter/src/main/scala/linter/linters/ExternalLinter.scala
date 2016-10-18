package linter.linters

import java.io.File

import linter.{Language, LinterError}

import scala.sys.process._

/**
  * Example class to showcase inheritance
  *
  * @param file     The file(s) to be checked - Can be a directory
  * @param language The language of the file(s)
  */
class ExternalLinter(file: File, language: Language.Value) extends BaseLinter(file, language) {

  // Keep those lazy so that they are initialised only if we actually need them
  private lazy val digitsMatch = "([0-9])+".r
  private lazy val hLinter = new HLinter
  private lazy val jLinter = new JLinter

  /**
    * This method will call the proper external linter based on the language
    * For Haskell, HLint will be called
    *
    * @return The list of mistakes found in the file(s) by the external linter
    */
  override def parseFiles: Seq[LinterError] = language match {
    case Language.Haskell => hLinter.check
    case Language.Java => jLinter.check
    case _ => throw new IllegalArgumentException("Wrong language given")
  }

  /*
   * External linters should mixin with this class
   */
  private trait Linter {
    def check: Seq[LinterError] = Seq()
  }

  /*
   * TODO: implement
   */
  private class JLinter extends Linter {
    override def check: Seq[LinterError] = super.check
  }

  /*
   * HLinter: Used to run HLint on haskell files, parse them and return a Seq[LinterError]
   */
  private class HLinter extends Linter {
    private lazy val pathMatch = "(.*?.hs)".r
    private lazy val posMatch = s":$digitsMatch:$digitsMatch".r
    private lazy val reasonMatch = ":(.*?\n)".r

    /*
     * Checks for mistakes using HLint
     */
    override def check: Seq[LinterError] = {
      val res = s"hlint ${file.getPath} --no-exit-code" !!
      val fileFinder = s"$pathMatch$posMatch$reasonMatch".r
      fileFinder.findAllIn(res).toArray.map(matchHaskellMistake)
    }

    private def matchHaskellMistake(string: String) = {
      // We can skip the checking because we know this exists
      val path = pathMatch.findFirstIn(string).get
      val positions = posMatch.findFirstIn(string).get
      val d = digitsMatch.findAllMatchIn(positions).toArray
      val digits = d.map(_.toString).map(Integer.decode)
      val reason = reasonMatch.findFirstIn(string).get

      new LinterError(reason, path, digits.apply(0), digits.apply(1))
    }
  }

}