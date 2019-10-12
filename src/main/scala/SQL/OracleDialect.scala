package SQL
import org.antlr.v4.runtime.{Lexer, Parser}
import org.antlr.v4.runtime.tree.{AbstractParseTreeVisitor, ParseTree}
import visitors.{DependencyVisitor, PlSqlVisitor}

class OracleDialect extends Dialect {

	override val description: String = "PL/SQL (Oracle)"

	override protected val dependencyVisitor: Class[_ <: AbstractParseTreeVisitor[Unit] with DependencyVisitor] = classOf[PlSqlVisitor]
	override protected val lexer: Class[_ <: Lexer] = classOf[PlSql.PlSqlLexer]
	override protected val parser: Class[_ <: Parser] = classOf[PlSql.PlSqlParser]

	override protected def getRoot(p: Parser): ParseTree = p.asInstanceOf[PlSql.PlSqlParser].sql_script()
}
