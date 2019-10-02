package SQL

import com.facebook.presto.sql.parser.IdentifierSymbol


abstract class Dialect {

  	val ddlMarkers = List("CREATE", "ALTER", "TRUNCATE")
  	val identifier = IdentifierSymbol.COLON

	val quoted_regex = "\".*\""
	val quoted_open = "\""
	val quoted_close = "\""

	val default_schema: String = quoted_open + "<USER>" + quoted_close

  	//Metodo che riscreverà in modo digerebile alcune peculiarità di alcuni dialetti
  	def normalize(query: String) : List[Query]


  	//La query in esame è un DDL? (create table / truncate table / ....)
  	def isDDL(query:String):Boolean = ddlMarkers.exists(query.contains)

	def quoted(name: String): String = {
		var tokens = name.toUpperCase().split('.')
		tokens = tokens.map(t => if (t.matches(quoted_regex)) t else quoted_open + t + quoted_close)
		if (tokens.length == 1) {
			tokens = Array(default_schema, tokens(0))
		}
		tokens.mkString(".")
	}

}
