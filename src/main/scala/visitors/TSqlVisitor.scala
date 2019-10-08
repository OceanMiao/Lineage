package visitors

import org.antlr.v4.runtime.tree.ParseTree
import TSql._

import scala.collection.mutable

class TSqlVisitor extends TSqlParserBaseVisitor[Unit] with DependencyVisitor {

	override protected val tokenSeparator: Char = '.'
	override protected val openQuote: Char = '['
	override protected val closeQuote: Char = ']'


	override def standardize(identifier: String): String = {
		if (identifier == null) return null

		if (identifier.split(tokenSeparator).length == 1) {
			if (identifier.startsWith("#"))
				return "[tempdb].[" + identifier + "]"
			return super.standardize("[DBO]." + identifier)
		}
		super.standardize(identifier)
	}


	// Questo metodo è un po' il cuore di tutto
	// Viene invocato quando si incontra il nome di una tabella (potenziale dipendenza) e deve risalire fino a trovare la tabella impattata
	// Ricordarsi di invocare il metodo <standardize> prima di ritornare
	def parentTableDeclaration(node:ParseTree) : String = {
		if (node.getParent == null)
			return null


		node match {
			// INSERT
			case p:TSqlParser.Insert_statementContext => {
				if (p.ddl_object()!=null) standardize(p.ddl_object().getText) else null
			}

			// UPDATE
			case p:TSqlParser.Update_statementContext => {
				val fullName = p.ddl_object().full_table_name()
				var res: String = if (fullName.database != null) fullName.database.getText  + tokenSeparator else ""
				res += (if (fullName.schema != null) fullName.schema.getText + tokenSeparator else "")
				standardize(res + (if (fullName.table != null) fullName.table.getText else ""))

			}

			// SUBQUERY + SELECT INTO + VIEWS
			case p:TSqlParser.Select_statementContext => {
				val t = if (p.query_expression() != null) {
							if (p.query_expression().query_specification() != null)
								p.query_expression().query_specification().table_name()
							else null
						} else null

				if (t != null)
					return standardize(t.getText) // SELECT INTO

				// SUBQUERY
				p.parent match {
					case context: TSqlParser.Common_table_expressionContext =>
						ctes += standardize(context.expression_name.getText)
						standardize(context.expression_name.getText)

					case _: TSqlParser.SubqueryContext => standardize(parentTableDeclaration(node.getParent)) // se è una subquery risali la gerarcia

					// CREATE VIEW
					case p:TSqlParser.Create_viewContext => {
						if (p.simple_name().schema != null)
							standardize(p.simple_name().schema.getText + tokenSeparator + p.simple_name().name.getText)
						else
							standardize(p.simple_name().name.getText)
					}
					case _ => null
				}
			}
			// Per tutti gli altri casi, si risale l'albero fino a trovare uno dei nodi di interesse
			case _ => parentTableDeclaration(node.getParent)
		}
	}


	override def visitSql_clause(ctx: TSqlParser.Sql_clauseContext): Unit = {
		println("Processing query: " + stringify(ctx))
		super.visitSql_clause(ctx)

		resolveCTE()
	}


	/**
	 * {@inheritDoc }
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	override def visitSelect_list(ctx: TSqlParser.Select_listContext): Unit = {
		//todo: tenere traccia dei calcoli
		super.visitSelect_list(ctx)
	}

	override def visitSelect_list_elem(ctx: TSqlParser.Select_list_elemContext): Unit = {
		//println(stringify(ctx))
		super.visitSelect_list_elem(ctx)
	}


	/**
	 * {@inheritDoc }
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	override def visitTable_name(ctx: TSqlParser.Table_nameContext): Unit = {
		val parent = ctx.getParent
		val tableName = parentTableDeclaration(ctx)

		if (tableName == null)
			return

		if (!tmp_dependencies.contains(tableName))
			tmp_dependencies += (tableName -> new mutable.HashSet[String]()) // creiamo la entry

		// Teniamo solo i nomi di tabelle che appartengono a certi contesti, scartiamo cose non utili
		parent match {
			case _: TSqlParser.Table_name_with_hintContext => {
				tmp_dependencies(tableName) += standardize(ctx.getText) //lo aggiungiamo alle dipendenze
			}
			case _ => {}
		}

		super.visitTable_name(ctx)
	}

	override def visitFull_table_name(ctx: TSqlParser.Full_table_nameContext): Unit = {
		val tableName = parentTableDeclaration(ctx)
		if (tableName == null)
			return

		if (!tmp_dependencies.contains(tableName))
			tmp_dependencies += (tableName -> new mutable.HashSet[String]()) // creiamo la entry
		else
			tmp_dependencies(tableName) += standardize(ctx.getText) //lo aggiungiamo alle dipendenze

		super.visitFull_table_name(ctx)
	}


}

