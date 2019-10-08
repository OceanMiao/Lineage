package visitors
import PlSql._
import org.antlr.v4.runtime.tree.ParseTree

import scala.collection.mutable

class PlSqlVisitor extends PlSqlParserBaseVisitor[Unit] with DependencyVisitor {
	override protected def tokenSeparator: Char = '.'

	override protected def openQuote: Char = '"'

	override protected def closeQuote: Char = '"'


	// Questo metodo permette di associare ad una tabella ciò che è impattato
	// risale l'albero fino a trovare indicazione del che cosa si stesse impattando
	def parentTableDeclaration(node:ParseTree) : String = {
		if (node.getParent == null)
			return null

		standardize(node match {
			// INSERT
			case i:PlSqlParser.Insert_statementContext => {
				if (i.single_table_insert() != null)
					i.single_table_insert().insert_into_clause().general_table_ref().dml_table_expression_clause().tableview_name().getText
				else
					throw new UnsupportedOperationException("Le INSERT in tabelle multiple non sono ancora supportate :( sorry") //TODO fix it
			}

			// UPDATE
			case u:PlSqlParser.Update_statementContext => u.general_table_ref().dml_table_expression_clause().tableview_name().getText

			// CREATE TABLE AS SELECT
			case c:PlSqlParser.Create_tableContext => c.tableview_name().getText

			// CREATE VIEW
			case v:PlSqlParser.Create_viewContext => v.tableview_name().getText

			// CREATE MATERIALIZED VIEW
			case v:PlSqlParser.Create_materialized_viewContext => v.tableview_name().getText


			case _ => parentTableDeclaration(node.getParent) //se non sono in grado di stabilire, risalgo l'albero

		})
	}

	// Intercettiamo le "with"
	override def visitFactoring_element(ctx: PlSqlParser.Factoring_elementContext): Unit = {
		ctes += ctx.query_name().getText
		super.visitFactoring_element(ctx)
	}

	// Risolviamo le "with" a conclusione di una query
	override def visitUnit_statement(ctx: PlSqlParser.Unit_statementContext): Unit = {
		super.visitUnit_statement(ctx)

		resolveCTE()
	}


	/**
	 * {@inheritDoc }
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	override def visitTableview_name(ctx: PlSqlParser.Tableview_nameContext): Unit = {
		val parent = ctx.getParent
		val tableName = parentTableDeclaration(ctx)

		if (tableName == null)
			return

		if (!tmp_dependencies.contains(tableName))
			tmp_dependencies += (tableName -> new mutable.HashSet[String]()) // creiamo la entry

		parent match {
			case _: PlSqlParser.Dml_table_expression_clauseContext => tmp_dependencies(tableName) += standardize(ctx.getText)
			case _ => {}
		}

		super.visitTableview_name(ctx)
	}

}
