package visitors

import org.antlr.v4.runtime.tree.ParseTree
import TSql._

import scala.collection.mutable

class TSqlVisitor[Void] extends TSqlParserBaseVisitor[Unit] with DependencyVisitor {

	private val tmp_dependencies = new mutable.HashMap[String, mutable.HashSet[String]]()
	private val ctes = new mutable.HashSet[String]()
	private var dependencies = new mutable.HashMap[String, mutable.HashSet[String]]()

	override protected val tokenSeparator: Char = '.'
	override protected val openQuote: Char = '['
	override protected val closeQuote: Char = ']'


	override def standardize(identifier: String): String = {
		if (identifier.split(tokenSeparator).length == 1) {
			if (identifier.startsWith("#"))
				return "[tempdb].[" + identifier + "]"
			return super.standardize("[DBO]." + identifier)
		}
		super.standardize(identifier)
	}

	override def dependenciesMap : collection.mutable.Map[String, List[String]] = {
		dependencies.map(t => t._1 -> t._2.toList)
	}


	/**
	 * Metodo di utilità per stampare in maniera "parsabile" il testo di un albero. A default, la getText ignora gli spazi
	 * @param node il nodo da cui partire
	 * @param cum cumulata e testo iniziale a cui appendere
	 * @return la stringa formata dal testo dei nodi concatenato dal parametro "sep" (default spazio)
	 */
	def stringify(node:ParseTree, cum:String ="", sep:String = " ") : String = {
		if (node == null)
			return ""

		if  (node.getChildCount == 0)
			return cum + node.getText + sep

		var res = ""
		for (i <- 0 to node.getChildCount)
			res += stringify(node.getChild(i), cum)

		res
	}

	// Questo metodo è un po' il cuore di tutto
	// Viene invocato quando si incontra il nome di una tabella (potenziale dipendenza) e deve risalire fino a trovare la tabella impattata
	// Ricordarsi di invocare il metodo <standardize> prima di ritornare
	def parentTableDeclaration(node:ParseTree, level:Int = 0) : String = {
		if (node.getParent == null)
			return null

		node match {
			case p:TSqlParser.Insert_statementContext => {
				if (p.ddl_object()!=null) standardize(p.ddl_object().getText) else null
			}
			case p:TSqlParser.Select_statementContext => {
				val t = if (p.query_expression() != null) {
							if (p.query_expression().query_specification() != null)
								p.query_expression().query_specification().table_name()
							else null
						} else null

				if ( t != null)
					return standardize(t.getText)

				p.parent match {
					case context: TSqlParser.Common_table_expressionContext =>
						ctes += standardize(context.expression_name.getText)
						return standardize(context.expression_name.getText)

					case _: TSqlParser.SubqueryContext => return parentTableDeclaration(node.getParent, level+1) // se è una subquery risali la gerarcia
				}
				null
			}

			case _ => parentTableDeclaration(node.getParent, level+1)
		}
	}


	override def visitSql_clause(ctx: TSqlParser.Sql_clauseContext): Unit = {
		println("Processing query: " + stringify(ctx))
		super.visitSql_clause(ctx)

		//Fine query, quindi risolviamo le CTEs, sostituendole nella mappa
		for (k <- ctes) {
			val tables = tmp_dependencies.keys.filter(s => tmp_dependencies(s).contains(k)) //tutte le tabelle che hanno tra le dipendenze una certa CTE
			for (t <- tables) {
				tmp_dependencies(t).remove(k) // togliamo la cte

				// e aggiungiamo le dipendenze della cte
				tmp_dependencies(t) = tmp_dependencies(t) ++ tmp_dependencies(k)
			}
			tmp_dependencies.remove(k)
		}

		// Uniamo le dipendenze calcolate in questa query con quelle complessive dello script
		// effettuiamo una merge tra le due mappe (in realtà le concateniamo e basta)
		dependencies = dependencies ++ tmp_dependencies

		// Puliamo tutti i buffer temporanei
		ctes.clear()
		tmp_dependencies.clear()
	}


	/**
	 * {@inheritDoc }
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	override def visitSelect_list(ctx: TSqlParser.Select_listContext): Unit = {
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

		if (tableName != null && !tmp_dependencies.contains(tableName))
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

