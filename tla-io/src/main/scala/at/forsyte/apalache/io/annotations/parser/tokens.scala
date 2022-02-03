package at.forsyte.apalache.io.annotations.parser

import scala.util.parsing.input.Positional

/**
 * A token that can be used in annotation.
 */
sealed trait AnnotationToken extends Positional

/**
 * An identifier
 *
 * @param name the name associated with the identifier
 */
case class IDENT(name: String) extends AnnotationToken {
  override def toString: String = name
}

/**
 * Annotation name that start with "@". We introduce a special token for that instead of using
 * a token for "@" and IDENT. The reason is that we want to ignore standalone "@" in the parser.
 * See: https://github.com/informalsystems/apalache/issues/757
 *
 * @param name the name associated with the identifier
 */
case class AT_IDENT(name: String) extends AnnotationToken {
  override def toString: String = '@' + name
}

/**
 * A string according to the TLA+ syntax, that is a sequence of characters between quotes, "...".
 *
 * @param text the contents of the string
 */
case class STRING(text: String) extends AnnotationToken {
  override def toString: String = '"' + text + '"'
}

/**
 * A string that appears between ":" and ";".
 *
 * @param text the contents of the string
 */
case class INLINE_STRING(text: String) extends AnnotationToken {
  override def toString: String = text
}

/**
 * A number
 *
 * @param num the value of the number
 */
case class NUMBER(num: BigInt) extends AnnotationToken {
  override def toString: String = num.toString()
}

/**
 * A Boolean value, FALSE or TRUE.
 *
 * @param bool string representation of a Boolean value: "FALSE" or "TRUE"
 */
case class BOOLEAN(bool: Boolean) extends AnnotationToken {
  override def toString: String = bool.toString
}

/**
 * Comma ",".
 */
case class COMMA() extends AnnotationToken {
  override def toString: String = "','"
}

/**
 * Dot ".". We don't really use dots, but they are useful, e.g., for parsing 1.23.
 */
case class DOT() extends AnnotationToken {
  override def toString: String = "'.'"
}

/**
 * Left parenthesis "(".
 */
case class LPAREN() extends AnnotationToken {
  override def toString: String = "'('"
}

/**
 * Right parenthesis ")".
 */
case class RPAREN() extends AnnotationToken {
  override def toString: String = "')'"
}

/**
 * Semicolon ";".
 */
case class SEMI() extends AnnotationToken {
  override def toString: String = "';'"
}
