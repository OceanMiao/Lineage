import SQL.{SQLScriptParser, SQLServerDialect}
import visitors._
import com.facebook.presto.sql.parser.{IdentifierSymbol, ParsingOptions, SqlParser, SqlParserOptions}

object main {

	private val DEBUG = false

	def main(args: Array[String]) : Unit = {
		if (DEBUG) {
			val sql_debug = ""
			val parser = new SqlParser((new SqlParserOptions).allowIdentifierSymbol(IdentifierSymbol.AT_SIGN))
			val parsed = parser.createStatement(sql_debug, ParsingOptions.builder().
				setDecimalLiteralTreatment(ParsingOptions.DecimalLiteralTreatment.AS_DECIMAL).
				build())
			println(parsed)
			System.exit(0)
		}

		val path = "C:\\Users\\mdivincenzo\\Documents\\Scala\\Lineage\\02_Proc_Modulo_Creazione_Parco_osservato.sql"
		val x = new SQLScriptParser(filePath = path, new SQLServerDialect())
		val querys = x.parse()
		val visitor = new TestVisitor
		for (q <- querys) {
			val result = visitor.process(q, List[String]())
			println(result)
		}

    x.close()

	}
}
