package GUI

import SQL.Dialect
import TSql.{TSqlLexer, TSqlParser}
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}
import visitors.TSqlVisitor

import scala.io.Source

object Controller {

	def parse(path:String, dialect:Dialect) : collection.mutable.Map[String, List[String]] = {
		val source = Source.fromFile(path, "ISO-8859-1")
		val text = source.getLines().map(s=>s.toUpperCase).mkString("\n")
		source.close

		dialect.parseDependencies(text)

	}

}
