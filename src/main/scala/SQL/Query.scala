package SQL
import scala.collection.mutable

class Query(val body:String,
            val dependencies: mutable.MutableList[String]
           )
{
  val isDDL = false
}





