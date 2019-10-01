package SQL

class AnsiDialect extends Dialect {
  override def normalize(query:String): List[Query] = query.split(';').map(s => new Query(s, null)).toList
}
