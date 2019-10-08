package visitors

import org.antlr.v4.runtime.tree.ParseTree

import scala.collection.mutable

trait DependencyVisitor {

	protected def tokenSeparator:Char
	protected def openQuote:Char
	protected def closeQuote:Char
	protected def quotedRegex: String = "\\" + openQuote + "[A-z]([A-z]|[0-9]|\\_)*\\" + closeQuote


	protected val tmp_dependencies = new mutable.HashMap[String, mutable.HashSet[String]]()
	protected val ctes = new mutable.HashSet[String]()
	protected var dependencies = new mutable.HashMap[String, mutable.HashSet[String]]()

	def dependenciesMap : collection.mutable.Map[String, List[String]] = dependencies.map(t => t._1 -> t._2.toList)


	def standardize(identifier:String): String = {
		if (identifier == null) return null

		val tokens = identifier.split(tokenSeparator)
		tokens.map(t => if (!t.matches(quotedRegex)) openQuote + t + closeQuote else t).mkString(".")
	}


	/**
	 * Metodo di utilità per stampare in maniera "parsabile" da un umano il testo di un albero. A default, la getText ignora gli spazi
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



	def resolveCTE() : Unit  = {
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
}
