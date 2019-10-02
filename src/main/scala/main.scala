import SQL.{SQLScriptParser, SQLServerDialect}
import visitors._
import com.facebook.presto.sql.parser.{IdentifierSymbol, ParsingOptions, SqlParser, SqlParserOptions}
import org.graphstream.graph.implementations._

object main {

	private val DEBUG = false
	private val CSS_PATH = "C:\\Users\\mdivincenzo\\Documents\\Scala\\Lineage\\graph.css"

	// raw sarà la mappa degli archi del grafo: NODO -> LISTA DI NODI CONNESSI
	private def visualizeGraph(raw:Map[String, List[String]], id:String = "dependencies") : Unit = {
		// 1. Estraggo i nodi complessivi da rappresentare, guardando la distinct dall'unione delle liste e delle chiavi
		val nodi = raw.keySet ++ raw.values.flatten.toSet
		val graph = new SingleGraph(id, true, true)

		// 2. Li aggiungo al grafo
		nodi.foreach(n => graph.addNode[SingleNode](n).setAttribute("ui.label", n))

		// 3. Scorro la mappa
		// per ciascuna coppia (NODO; LISTA), scorro la lista, ed aggiungo un nodo
		raw.foreach( (mapEntry) => mapEntry._2.foreach(dep => graph.addEdge[AbstractEdge](mapEntry._1 + dep, dep, mapEntry._1,  true)))

		// Visualizzazione
		graph.addAttribute("ui.stylesheet", "url(file:///"+CSS_PATH+")")
		graph.addAttribute("ui.antialias")
		val x = graph.display()

	}

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
		val dialect = new SQLServerDialect
		val x = new SQLScriptParser(filePath = path, dialect)
		val querys = x.parse()
		x.close()

		val mappa = collection.mutable.Map[String, List[String]]()
		for (q <- querys) {
			val visitor = new DependencyVisitor
			visitor.process(q)
		/*	println(visitor.table + ": " + visitor.dependencies.distinct.toList)*/
			mappa += (dialect.quoted(visitor.table) -> visitor.dependencies.distinct.toList.map(dialect.quoted(_)))
			println("Processed dependencies for table " + visitor.table)
		}

		visualizeGraph(mappa.toMap)

	}
}
