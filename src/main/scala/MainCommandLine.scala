import SQL.SQLServerDialect
import visitors._
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream, ParserRuleContext}
import org.antlr.v4.runtime.tree.{ErrorNode, ParseTree, ParseTreeListener, ParseTreeWalker, TerminalNode}
import org.graphstream.graph.implementations._
import TSql._
import PlSql._

object MainCommandLine  {

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
		graph.addAttribute("ui.stylesheet", "url(file:///" + CSS_PATH + ")")
		graph.addAttribute("ui.antialias")
		val x = graph.display()

	}

	def main(args: Array[String]) : Unit = {

		/*val lexer = new TSqlLexer (CharStreams.fromString("select a=sum(x) over (partition by pluto), b=2*c into yuo from pluto left join pluto e on id=x".toUpperCase()))
		val cts = new CommonTokenStream(lexer)

		val parser = new TSqlParser(cts)

		val tree = parser.tsql_file
		tree.accept(new TSqlVisitor)*/

		val lexer = new PlSqlLexer (CharStreams.fromString("create table pippo as with pluto as (select * from x) select y from pluto;".toUpperCase()))
		val cts = new CommonTokenStream(lexer)

		val parser = new PlSqlParser(cts)

		val tree = parser.sql_script()
		tree.accept(new PlSqlVisitor)

	}


}
