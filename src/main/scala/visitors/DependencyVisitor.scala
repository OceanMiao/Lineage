package visitors

trait DependencyVisitor {

	protected def tokenSeparator:Char
	protected def openQuote:Char
	protected def closeQuote:Char
	protected def quotedRegex: String = "\\" + openQuote + "[A-z]([A-z]|[0-9]|\\_)*\\" + closeQuote

	def dependenciesMap : collection.mutable.Map[String, List[String]]

	def standardize(identifier:String): String = {
		val tokens = identifier.split(tokenSeparator)
		tokens.map(t => if (!t.matches(quotedRegex)) openQuote + t + closeQuote else t).mkString(".")
	}
}
