package SQL

import com.facebook.presto.sql.parser.IdentifierSymbol

// Classe che racchiude tutte le peculiarità del codice SQL - SQLServer
// Es.: l'avere nomi qualificati racchiusi tra "[" e "]"
//      lo specificare variabili con "@" e il doverne fare "DECLARE" e "SET"
//      l'avere tabelle temporanee che comincino con "#" / "##"
//      il dover specificare separatamente la COLLATION
class SQLServerDialect extends Dialect {


  	override val ddlMarkers: List[String] = List("CREATE", "ALTER", "TRUNCATE", "INTO", "DECLARE", "SET")
	override val identifier = IdentifierSymbol.AT_SIGN

	override val quoted_regex = "\\[.*?\\]"
	override val quoted_open =  "["
	override val quoted_close = "]"

	override val default_schema: String = "[DBO]"

	private def _normalize(singleQuery: String) : Query = {
		var rewrite_it = singleQuery.replace("##", "[tempdb].").replace("#", "[tempdb].")

		// Signore, prega per la nostra anima che cerca di parsare con regex cose che non dovrebbero essere fatte con regex
		val quotedRegex = "[A-z]([A-z]|[0-9]|\\_)*?\\.([A-z]|[0-9]|\\_)*"
		val variableRegex = "\\@([A-z]|[0-9]|\\_)*"
		val quotedStarRegex = "[A-z]([A-z]|[0-9]|\\_)*\\.\\*"

		rewrite_it = rewrite_it.replaceAll(quotedStarRegex, " \"$1\"")
		rewrite_it = rewrite_it.replaceAll(quotedRegex, " \"$0\" " )
		rewrite_it = rewrite_it.replaceAll(variableRegex, " \"$0\" " )

		// Rimozione di eventuali OPTION, che non influenzano i dati e non sono SQL parsabile da nessun tool -_-
		rewrite_it = rewrite_it.replaceAll("OPTION(\\ )?\\(.*\\)$", "")

		// Pulizia dei collate
		//TODO: generalizzare a qualsiasi collation, e non ad una sola cablata barbaramente nel codice
		rewrite_it = rewrite_it.replace("COLLATE LATIN1_GENERAL_CI_AS", "")

		val intoRegex = "INTO.*?FROM".r.unanchored
		val intoMatches = intoRegex.findAllIn(rewrite_it)
		var q:Query = null
		if (intoMatches.hasNext) {
		  val matched = intoMatches.next()
		  rewrite_it = rewrite_it.replaceAll(intoRegex.regex, " FROM ")
		  rewrite_it = "CREATE TABLE " + matched.replace("INTO", "").replace("FROM", "") + " as " + rewrite_it
		  q = new DDLQuery(rewrite_it, null, matched)
		}
		else {
		  q = new Query(rewrite_it, null)
		}

		return q
	  }


	override def normalize(query: String): List[Query] = {
		query.split(';').toList.map(_.trim).filterNot(s=>s.contains("DECLARE") || s.contains("SET") || s.startsWith("IF")).map(_normalize)
	}


}
