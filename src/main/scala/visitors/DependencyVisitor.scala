package visitors

import com.facebook.presto.sql.tree._

import scala.collection.mutable


class DependencyVisitor extends DefaultTraversalVisitor[List[String], List[String]] {

	val dependencies: mutable.MutableList[String] = mutable.MutableList[String]()
	val aliases: mutable.Map[String, Set[String]] = mutable.Map[String, Set[String]]()
	var table:String = _

	override def visitTable(node: Table, context: List[String]): List[String] = {
	  	super.visitTable(node, context)
	  	dependencies+=node.getName.toString
	  	dependencies.toList
	}

	override def visitSelect(node: Select, context: List[String]): List[String] = {
		super.visitSelect(node, context)
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

	override def visitAliasedRelation(node: AliasedRelation, context: List[String]): List[String] = {
		//println(node.getAlias, node.getColumnNames, node.getRelation, node)
		if (aliases.contains(node.getAlias.toString))
			aliases(node.getAlias.toString) += node.getRelation.toString
		else
			aliases(node.getAlias.toString) = Set[String](node.getRelation.toString)
		super.visitAliasedRelation(node, context)
	}

	override def visitRelation(node: Relation, context: List[String]): List[String] = {
		if (aliases.contains(node.toString))
			aliases(node.toString) += node.toString
		else
			aliases(node.toString) = Set[String](node.toString)

		super.visitRelation(node, context)
	}

}

