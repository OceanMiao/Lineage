package GUI

import java.awt.Event
import java.awt.event.{ActionEvent, ActionListener, MouseWheelEvent, MouseWheelListener}
import java.io.File

import GUI.MainGUI._
import SQL.Dialect
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter
import org.graphstream.graph.implementations.MultiNode
import org.graphstream.ui.view.ViewerListener

import scala.io.Source

class Controller extends ActionListener with ViewerListener with MouseWheelListener {

    // Chiusura della finestra con il grafo
	override def viewClosed(viewName: String): Unit = MainGUI.loop = false

	// Metodo invocato quando si pusha su un nodo
	override def buttonPushed(id: String): Unit = {}

	// Metodo invocato quando si alza il click su un nodo
	override def buttonReleased(id: String): Unit = {
		val node = MainGUI.graph.getNode[MultiNode](id)
		if (node.hasAttribute("ui.class") && node.getAttribute[String]("ui.class") == "clicked") {
			node.removeAttribute("ui.class")
			node.addAttribute("ui.class", "")
			return
		}
		if (!node.hasAttribute("ui.class") || (node.hasAttribute("ui.class") && node.getAttribute[String]("ui.class")==""))
			node.addAttribute("ui.class", "clicked")

	}

	override def mouseWheelMoved(mwe: MouseWheelEvent): Unit = {
		if (Event.ALT_MASK != 0) if (mwe.getWheelRotation > 0) {
			val new_view_percent = graphPanel.getCamera.getViewPercent + 0.05
			graphPanel.getCamera.setViewPercent(new_view_percent)
		}
		else if (mwe.getWheelRotation < 0) {
			val current_view_percent = graphPanel.getCamera.getViewPercent
			if (current_view_percent > 0.05) graphPanel.getCamera.setViewPercent(current_view_percent - 0.05)
		}
	}

	// Listener per i vari componenti java della UI (bottoni, menù...)
	override def actionPerformed(e: ActionEvent): Unit = {
		/*  MI DISPIACE che sia codice così "javoso" qua, si potrebbe rifattorizzare */
		e.getSource match {

			case MainGUI.search => {
				// Filtraggio del grafo
				val getMe = tf.getSelectedItem.toString
				updateGraph(graphMap.toMap.filter( t => t._1 == getMe), false)
			}

			case MainGUI.reset => updateGraph(graphMap.toMap)

			case MainGUI.clearMenu => {
				graphMap = collection.mutable.Map[String, List[String]]()
				updateGraph(graphMap.toMap)
			}
			case _ => {}
		}
	}
}


object Controller {

	// Non cambiare encoding, a meno di non avere stretta esigenza, questo di default legge anche gli accenti :)
	def parse(path:String, dialect:Dialect, encoding:String = "ISO-8859-1") : collection.mutable.Map[String, List[String]] = {
		val source = Source.fromFile(path, encoding)
		val text = source.getLines().map(s => s.toUpperCase).mkString("\n")
		source.close

		dialect.parseDependencies(text)

	}

	def importDialect(d:Dialect) : Unit = {
		// File chooser button
		val fc = new JFileChooser()
		fc.setFileFilter(new FileFilter {
			override def accept(f: File): Boolean = f.getName().toLowerCase().endsWith(".sql") || f.isDirectory
			override def getDescription: String = "Only SQL files"
		})
		fc.setDialogTitle("Choose a " + d.description + " file to open")
		fc.showOpenDialog(frame)
		if (fc.getSelectedFile != null) {
			val parsedMap = Controller.parse(fc.getSelectedFile.getAbsolutePath, d).toSeq
			graphMap = collection.mutable.Map[String, List[String]]((graphMap.toSeq ++ parsedMap).groupBy(_._1).mapValues(_.map(_._2).flatten.toList).toSeq: _*)
			updateGraph(graphMap.toMap)
		}
	}

}
