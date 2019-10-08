package GUI

import SQL.Dialect
import TSql.{TSqlLexer, TSqlParser}
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}
import visitors.TSqlVisitor

import scala.io.Source

object Controller {

	// Non cambiare encoding, a meno di non avere stretta esigenza, questo di default legge anche gli accenti :)
	def parse(path:String, dialect:Dialect, encoding:String = "ISO-8859-1") : collection.mutable.Map[String, List[String]] = {
		val source = Source.fromFile(path, encoding)
		val text = source.getLines().map(s=>s.toUpperCase).mkString("\n")
		source.close

		dialect.parseDependencies(text)

	}

}
