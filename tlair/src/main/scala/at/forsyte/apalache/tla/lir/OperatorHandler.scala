package at.forsyte.apalache.tla.lir

import at.forsyte.apalache.tla.lir.db.HashMapDB
import at.forsyte.apalache.tla.lir.oper.{TlaBoolOper, TlaOper}
import at.forsyte.apalache.tla.lir.plugins.{Identifier, UniqueDB}

class BodyDB extends HashMapDB[String, (List[FormalParam], TlaEx)] {
  override val name = "bodyDB"

  override def put( key : String,
                    value : (List[FormalParam], TlaEx)
                  ) : Option[(List[FormalParam], TlaEx)] = {
    m_map.put( key, (value._1, value._2.deepCopy( identified = false )) )
  }

  override def update( key : String,
                       value : (List[FormalParam], TlaEx)
                     ) : Unit = {
    m_map.update( key, (value._1, value._2.deepCopy( identified = false )) )
  }

  def params( p_name : String ) : Option[List[FormalParam]] = apply( p_name ).map( _._1 )

  def body( p_name : String ) : Option[TlaEx] = apply( p_name ).map( _._2 )

  def arity( p_name : String ) : Option[Integer] = params( p_name ).map( _.size )
}

class SourceDB extends HashMapDB[UID, UID] {
  override val name : String = "sourceDB"

  override def put( key : UID,
                    value : UID
                  ) : Option[UID] =
    (key, value) match {
      case (UID( x ), UID( y )) if x < 0 || y < 0 => None
      case _ => super.put( key, value )
    }

  override def update( key : UID,
                       value : UID
                     ) : Unit =
    (key, value) match {
      case (UID( x ), UID( y )) if x < 0 || y < 0 => /* return */
      case _ => super.update( key, value )
    }
}

object DummySrcDB extends SourceDB {
  override val name : String = "DummySource"

  override def put( key : UID, value : UID ) : Option[UID] = None

  override def update( key : UID, value : UID ) : Unit = {}

  override def apply( key : UID ) : Option[UID] = None

  override def size( ) : Int = 0

  override def contains( key : UID ) : Boolean = false

  override def remove( key : UID ) : Unit = {}

  override def clear( ) : Unit = {}

  override def print( ) : Unit = {}
}

object OperatorHandler {

  protected def markSrc( p_old : TlaEx,
                         p_new : TlaEx,
                         p_srcDB : SourceDB
                       ) : Unit = {
    Identifier.identify( p_new )
    if ( p_old.ID != p_new.ID ) {
      p_srcDB.update( p_new.ID, p_old.ID )
    }
  }

  def uniqueVarRename( p_decls: Seq[TlaDecl] ) : Seq[TlaDecl] = {
    def renameParam( p_prefix : String )( p_param : FormalParam ) : FormalParam = {
      p_param match {
        case SimpleFormalParam( name ) => SimpleFormalParam( "%s_%s".format( p_prefix, name ) )
        case OperFormalParam( name, arity ) => OperFormalParam( "%s_%s".format( p_prefix, name ), arity )
      }
    }

    def boundVarsRule(p_prefix : String, p_argNames : List[String])( p_ex : TlaEx ) : TlaEx = {
      p_ex match{
        case OperEx(
          oper, // assuming bounded quantification!
          NameEx( varname ),
          set,
          body
        ) if oper == TlaBoolOper.exists || oper == TlaBoolOper.forall =>
          OperEx(
            oper,
            NameEx( "%s_%s".format( p_prefix, varname ) ),
            replaceWithRule( set, boundVarsRule( p_prefix, p_argNames ) ),
            replaceWithRule( body, boundVarsRule( p_prefix, varname :: p_argNames ) )
          )
        case NameEx( name ) =>
          NameEx( if (p_argNames.contains( name) ) "%s_%s".format( p_prefix, name ) else name )
        case _ => p_ex
      }
    }

    def renameBoundVars( p_prefix : String, p_argNames : List[String] )( p_body : TlaEx ) : TlaEx = {
      replaceWithRule( p_body, boundVarsRule( p_prefix, p_argNames ) )
    }

    def addPrefix( p_decl : TlaDecl ) : TlaDecl = {
      p_decl match{
        case TlaOperDecl( name, formalParams, body ) =>
          TlaOperDecl(
            name,
            formalParams.map( renameParam( name ) ),
            renameBoundVars( name, formalParams.map( _.name ) )( body )
          )
        case _ => p_decl
      }
    }
    p_decls.map( addPrefix )
  }

  def extract( p_decl : TlaDecl,
               p_db : BodyDB
                 ) : Unit = {
    p_decl match {
        /**
          * What to do when extracting the same operator > 1 times? Currently, we skip the 2nd+.
          * Jure, 1.12.2017
          * */
      case decl : TlaOperDecl if !p_db.contains( p_decl.name ) => {
        Identifier.identify( p_decl )
        p_db.update( decl.name, (decl.formalParams, decl.body) )
      }
      case _ =>
    }
  }

  def extract( p_spec : TlaSpec,
               p_db : BodyDB
             ) : Unit = SpecHandler.sideeffectDecl( p_spec, extract( _, p_db ) )

  def replaceAll( p_tlaEx : TlaEx,
                  p_replacedEx : TlaEx,
                  p_newEx : TlaEx,
                  p_srcDB : SourceDB = DummySrcDB
                ) : TlaEx = {
    def swap( arg : TlaEx ) : TlaEx =
      if ( arg == p_replacedEx ) {
        val ret = p_newEx.deepCopy( identified = false )
        Identifier.identify( ret )
        p_srcDB.update( ret.ID, arg.ID )
        ret
      }
      else arg

    SpecHandler.getNewEx( p_tlaEx, swap, markSrc( _, _, p_srcDB ) )
  }

  def replaceWithRule( p_ex : TlaEx,
                       p_rule : TlaEx => TlaEx,
                       p_srcDB : SourceDB = DummySrcDB
                     ) : TlaEx = {
    SpecHandler.getNewEx( p_ex, p_rule, markSrc( _, _, p_srcDB ) )
  }

  def undoReplace( p_ex : TlaEx,
                   p_srcDB : SourceDB
                 ) : TlaEx = {
    if ( p_srcDB.contains( p_ex.ID ) ) {
      UniqueDB( p_srcDB( p_ex.ID ).get ).get
    }
    else p_ex
  }

  def unfoldOnce( p_ex : TlaEx,
                  p_bdDB : BodyDB,
                  p_srcDB : SourceDB = DummySrcDB
                ) : TlaEx = {

    /**
      * Bug( Jure: 1.12.2017 ): inlining did not check if provided argument count matches arity,
      * because the parser would reject such TLA code. However, manual examples produced
      * demonstrated lack of exceptions thrown when the number of args provided exceeded the arity.
      *
      * This has been rectified by a check in lambda.
      * */

    def subAndID( p_operEx : TlaEx ) : TlaEx = {

      def lambda( name : String, args : TlaEx* ) : TlaEx = {
        val pbPair = p_bdDB( name )
        if ( pbPair.isEmpty ) return p_operEx

        var (params, body) = pbPair.get
        if( params.size != args.size )
          throw new IllegalArgumentException(
            "Operator %s with arity %s called with %s argument%s".format( name, params.size, args.size, if(args.size != 1) "s" else "" )
          )

        params.zip( args ).foreach(
          pair => body = replaceAll( body, NameEx( pair._1.name ), pair._2, p_srcDB )
        )
        Identifier.identify( body )
        p_srcDB.update( body.ID, p_operEx.ID )
        /* return */ body
      }

      p_operEx match {
        case OperEx( op : TlaUserOper, args@_* ) => lambda( op.name, args : _* )
        case OperEx( TlaOper.apply, NameEx( name ), args@_* ) => lambda( name, args : _* )
        case _ => p_operEx
      }
    }

    val ret = SpecHandler.getNewEx( p_ex, subAndID )
    Identifier.identify( ret )
    p_srcDB.update( ret.ID, p_ex.ID )
    ret
  }

  def unfoldMax( p_ex : TlaEx,
                 p_bdDB : BodyDB,
                 p_srcDB : SourceDB = DummySrcDB
               ) : TlaEx = {
    var a = p_ex
    var b = unfoldOnce( p_ex, p_bdDB, p_srcDB )
    while ( a != b ) {
      a = b
      b = unfoldOnce( b, p_bdDB, p_srcDB )
    }
    Identifier.identify( b )
    p_srcDB.update( b.ID, p_ex.ID )
    b
  }

}
