package at.forsyte.apalache.tla.lir

import at.forsyte.apalache.tla.lir.oper.{FixedArity, OperArity}

/**
  * Convenient shortcuts and definitions. Import them, when needed.
  */
package object convenience {
  /**
    * This is just a short-hand to Builder, so one can type more naturally, e.g., tla.plus(tla.int(2), tla.int(3))
    */
  val tla : Builder.type = Builder

  // TODO: remove? Just call UID(id)?
  implicit def makeUID( id : Int ) : UID = UID( id )

  // TODO: remove? Just call EID(id)
  implicit def makeEID( id : Int ) : EID = EID( id )

  // TODO: remove? Just call FixedArity(n)
  implicit def opArity( n : Int ) : OperArity = FixedArity( n )
}