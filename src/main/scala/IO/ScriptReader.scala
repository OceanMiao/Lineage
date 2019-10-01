package IO
import SQL.{Dialect, Query}

import scala.io.Source

class ScriptReader(filePath : String) {
  private val source = Source.fromFile(filePath)
  private val lines = source.getLines()


  // Occorre spezzare il file in singole query
  // Oracle, richiede che le query siano terminate da ";", quindi in quel caso non ci sarebbe problema
  // Ma SQLServer accetta anche query consecutive non divise da ";", lo capisce lui dove finisce una ed inizia l'altra..
  // ...siccome il nostro parser invece non ce la fa, è necessario capirlo noi e quindi rimediare.
  // Il metodo parse deve restituire una lista di singole query
  //TODO: gesitre correttamente che in SQLServer non è necessario il ";"
  // Valutare se convenga implementare qualcosa in pattern visitor, rimandando al singolo dialect il come spezzare il file in query
  def parse(dialect : Dialect) : List[Query] = {
    val nocomment = lines.map(_.replaceAll("--.*", "")).filter(_.length > 0).map(_.trim).mkString(" ").toUpperCase().replaceAll("\\/\\*.*?\\*\\/", "")
    dialect.normalize(nocomment)
  }

  def close() : Unit = source.close()
}
