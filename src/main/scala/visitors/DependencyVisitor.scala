package visitors

import com.facebook.presto.sql.tree._

import scala.collection.mutable


class DependencyVisitor extends DefaultTraversalVisitor[List[String], List[String]] {

	val dependencies: mutable.MutableList[String] = mutable.MutableList[String]()
	var table:String = _

	override def visitTable(node: Table, context: List[String]): List[String] = {
	  	super.visitTable(node, context)
	  	dependencies+=node.getName.toString
	  	dependencies.toList
	}

	override def visitJoin(node: Join, context: List[String]): List[String] = {
	  super.visitJoin(node, context)
	}

	override def visitCreateTable(node: CreateTable, context: List[String]): List[String] = {
		table = node.getName.toString
		super.visitCreateTable(node, context)
	}

	override def visitCreateTableAsSelect(node: CreateTableAsSelect, context: List[String]): List[String] = {
		table = node.getName.toString
		super.visitCreateTableAsSelect(node, context)

	}

}

