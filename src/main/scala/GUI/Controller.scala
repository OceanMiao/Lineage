package GUI

import java.awt.{BorderLayout, Cursor, Event}
import java.awt.event.{ActionEvent, ActionListener, MouseWheelEvent, MouseWheelListener}
import java.io.File

import GUI.DataLineage._
import SQL.Dialect
import javax.swing.{JDialog, JFileChooser, JLabel, JProgressBar, SwingUtilities, JOptionPane}
import javax.swing.filechooser.FileFilter
import org.graphstream.graph.implementations.MultiNode
import org.graphstream.ui.view.ViewerListener
import javax.swing.plaf.ProgressBarUI

import scala.io.Source

class Controller extends ActionListener with ViewerListener with MouseWheelListener {

    // Chiusura della finestra con il grafo
	override def viewClosed(viewName: String): Unit = DataLineage.loop = false

	// Metodo invocato quando si pusha su un nodo
	override def buttonPushed(id: String): Unit = {}

	// Metodo invocato quando si alza il click su un nodo
	override def buttonReleased(id: String): Unit = {
		val node = DataLineage.graph.getNode[MultiNode](id)
		if (node.hasAttribute("ui.class") && node.getAttribute[String]("ui.class") == "clicked") {
			node.removeAttribute("ui.class")
			node.addAttribute("ui.class", "")
			Controller.updateDetailPanel(null)
			return
		}
		if (!node.hasAttribute("ui.class") || (node.hasAttribute("ui.class") && node.getAttribute[String]("ui.class")=="")) {
			DataLineage.graph.getNodeSet[MultiNode].forEach(n => n.removeAttribute("ui.class"))
			node.addAttribute("ui.class", "clicked")
			Controller.updateDetailPanel(node.getId)
		}


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

			case DataLineage.search => {
				// Filtraggio del grafo
				val getMe = tf.getSelectedItem.toString
				updateGraph(graphMap.toMap.filter( t => t._1 == getMe), false)
			}

			case DataLineage.reset => updateGraph(graphMap.toMap)

			case DataLineage.clearMenu => {
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

		DataLineage.frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR))
		var res = collection.mutable.Map[String, List[String]]()
		try {
			res = dialect.parseDependencies(text)
		}
		catch {
			case e:Exception => JOptionPane.showMessageDialog(DataLineage.frame, "Error while processing " + path +
				"\n" + e.getMessage + "\nPlease, verify that you are passing a file written in " + dialect.description + " dialect.",
				"Parsing error",
				JOptionPane.ERROR_MESSAGE)
		}

		DataLineage.frame.setCursor(Cursor.getDefaultCursor)
		return res

	}

	def importDialect(d:Dialect) : Unit = {
		// File chooser button
		val fc = new JFileChooser()
		fc.setFileFilter(new FileFilter {
			override def accept(f: File): Boolean = f.getName().toLowerCase().endsWith(".sql") || f.isDirectory
			override def getDescription: String = "Only SQL files"
		})
		fc.setDialogTitle("Choose a " + d.description + " file to open")
		fc.setControlButtonsAreShown(true)
		fc.showOpenDialog(frame)
		if (fc.getSelectedFile != null) {
			val parsedMap = Controller.parse(fc.getSelectedFile.getAbsolutePath, d).toSeq
			graphMap = collection.mutable.Map[String, List[String]]((graphMap.toSeq ++ parsedMap).groupBy(_._1).mapValues(_.map(_._2).flatten.toList).toSeq: _*)
			updateGraph(graphMap.toMap)
		}
	}

	def updateDetailPanel(table:String) : Unit = {
		DataLineage.eastPanel.setVisible(false)

		if (table != null) {
			DataLineage.detailLabel.setText(table)
			DataLineage.detailLabel.setFont(DataLineage.boldFont)
			//TODO: fetch the list of columns and put them in the list
			// also, make the list look decent
			//MainGUI.selectedList.setListData(Array[String]("a", "b", "c"))
			DataLineage.eastPanel.setVisible(true)
		}

		DataLineage.frame.validate()

	}


}
