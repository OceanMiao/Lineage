package SQL
import scala.collection.mutable

class DDLQuery(override val body: String,
               override val dependencies: mutable.MutableList[String],
               affectedStructure:String)
  extends Query(body, dependencies)
{
  override val isDDL: Boolean = true
}
