package SQL

import com.facebook.presto.sql.parser.IdentifierSymbol


abstract class Dialect {

  val ddlMarkers = List("CREATE", "ALTER", "TRUNCATE")
  val identifier = IdentifierSymbol.COLON

  //Metodo che riscreverà in modo digerebile alcune peculiarità di alcuni dialetti
  def normalize(query: String) : List[Query]


  //La query in esame è un DDL? (create table / truncate table / ....)
  def isDDL(query:String):Boolean = ddlMarkers.exists(query.contains)

}
