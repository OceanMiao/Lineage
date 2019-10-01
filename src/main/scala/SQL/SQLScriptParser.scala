package SQL
import IO.ScriptReader
import com.facebook.presto.sql.tree.Statement
import com.facebook.presto.sql.parser.{IdentifierSymbol, ParsingOptions, SqlParser, SqlParserOptions}

class SQLScriptParser(val filePath:String,  dialect: Dialect = new AnsiDialect()) {

  private val reader =  new ScriptReader(filePath)

  val querys : List[Query] = reader.parse(dialect)

  private def parse(query:Query) : Statement = {
    val parser = new SqlParser((new SqlParserOptions).allowIdentifierSymbol(dialect.identifier))
    parser.createStatement(query.body, ParsingOptions.builder().
                                                  setDecimalLiteralTreatment(ParsingOptions.DecimalLiteralTreatment.AS_DECIMAL).
                                                  build())

  }

  def parse(): List[Statement] = this.querys.map(this.parse(_))


  def close() : Unit = reader.close()

}
