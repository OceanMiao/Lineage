package visitors

import com.facebook.presto.sql.tree._

class TestVisitor extends DefaultTraversalVisitor[List[String], List[String]] {


  override def visitTable(node: Table, context: List[String]): List[String] = {
	  val cont = context.::(node.getName.toString)
	  super.visitTable(node, cont)
  }

  override def visitJoin(node: Join, context: List[String]): List[String] = {
	  val res = super.visitJoin(node, context)
	  print(context)
	  res
  }

}

