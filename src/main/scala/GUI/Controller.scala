package GUI

import java.util

import SQL.{Dialect, SQLScriptParser}
import com.facebook.presto
import com.facebook.presto.sql.analyzer.Analyzer
import com.facebook.presto.sql.parser.StatementSplitter.Statement
import com.facebook.presto.sql.tree.Node
import visitors.DependencyVisitor

object Controller {

	def parse(path:String, dialect:Dialect) : collection.mutable.Map[String, List[String]] = {
		val x = new SQLScriptParser(filePath = path, dialect)
		val querys = x.parse()
		x.close()

		val mappa = collection.mutable.Map[String, List[String]]()
		for (q <- querys) {
			val visitor = new DependencyVisitor
			visitor.process(q)
			/*	println(visitor.table + ": " + visitor.dependencies.distinct.toList)*/
			mappa += (dialect.quoted(visitor.table) -> visitor.dependencies.distinct.toList.map(dialect.quoted))
			println("Processed dependencies for table " + visitor.table)
			//println(visitor.aliases)
		}

		mappa
	}

}
