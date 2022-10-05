package at.forsyte.apalache.io.lir

import at.forsyte.apalache.tla.lir.TlaModule
import org.scalatest.funsuite.AnyFunSuite

import java.io.{PrintWriter, StringWriter}

trait TestCounterexampleWriterBase extends AnyFunSuite {

  /**
   * Write a counterexample and compare the output to the expected result.
   *
   * @param kind
   *   counterexample kind ("tla", "json", "itf.json")
   * @param rootModule
   *   the module that produced the counterexample
   * @param notInvariant
   *   the invariant violation (as an expression)
   * @param states
   *   a list of states: state 0 is the constant initializer, state 1 is the initial state, etc.
   * @param expected
   *   the expected output as a string
   */
  def compare(
      kind: String,
      rootModule: TlaModule,
      notInvariant: NotInvariant,
      states: Counterexample.States,
      expected: String): Unit = {

    val cx = Counterexample(rootModule, states, notInvariant)
    val stringWriter = new StringWriter()
    val printWriter = new PrintWriter(stringWriter)
    val writer = CounterexampleWriter(kind, printWriter)
    writer.write(cx)
    printWriter.flush()
    val dateErasure = stringWriter.toString.replaceFirst(
        "Created by Apalache on [A-Za-z 0-9:]*( \\*\\))?([\n\"])",
        "Created by Apalache on DATETIME$1$2",
    )
    assert(dateErasure == expected)
  }

}
