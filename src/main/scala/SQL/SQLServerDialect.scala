package SQL
import org.antlr.v4.runtime.{Lexer, Parser}
import org.antlr.v4.runtime.tree.{AbstractParseTreeVisitor, ParseTree}
import visitors._


class SQLServerDialect extends Dialect {
	override val description: String = "T-SQL (SQL Server)"
	override protected val dependencyVisitor: Class[_ <: AbstractParseTreeVisitor[Unit] with DependencyVisitor] = classOf[TSqlVisitor]
	override protected val lexer: Class[_ <: Lexer] = classOf[TSql.TSqlLexer]
	override protected val parser: Class[_ <: Parser] = classOf[TSql.TSqlParser]

	override protected def getRoot(p: Parser): ParseTree = p.asInstanceOf[TSql.TSqlParser].tsql_file()


}
