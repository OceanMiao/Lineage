package SQL

import org.antlr.v4.runtime.tree.{AbstractParseTreeVisitor, ParseTree}
import org.antlr.v4.runtime.{CharStream, CharStreams, CommonTokenStream, Lexer, Parser, TokenStream}
import visitors.DependencyVisitor

//TODO: fare qualcosa per SQLServer che permette di chiamare colonne con parole riservate
// solo che poi durante il parse va tutto in malora, chiaramente -_-
abstract class Dialect {

	protected val dependencyVisitor: Class[ _<: AbstractParseTreeVisitor[Unit] with DependencyVisitor]

	protected val lexer : Class[_<:Lexer]

	protected val parser: Class[_<:Parser]

	def parseDependencies(txt:String) :  collection.mutable.Map[String, List[String]] = {
		println("[DEBUG] Instantiating lexer...")
		val lx: Lexer = lexer.getConstructor(classOf[CharStream]).newInstance(CharStreams.fromString(txt.toUpperCase())).asInstanceOf[Lexer]
		println("[DEBUG] Creating token stream...")
		val cts = new CommonTokenStream(lx)
		println("[DEBUG] Parsing Stream...")
		val pr: Parser = parser.getConstructor(classOf[TokenStream]).newInstance(cts).asInstanceOf[Parser]
		val tree = getRoot(pr)
		println("[DEBUG] Processing AST...")
		val visitor = dependencyVisitor.newInstance().asInstanceOf[AbstractParseTreeVisitor[Unit] with DependencyVisitor]
		tree.accept(visitor)
		//println(tree.toStringTree(pr))
		visitor.dependenciesMap
	}

	protected def getRoot(p:Parser) :ParseTree

}
