package GUI

import java.awt.event.{ActionEvent, ActionListener, MouseWheelEvent, MouseWheelListener}
import java.awt.{BorderLayout, Event, GridBagLayout, Color}
import java.io.File

import SQL.{AnsiDialect, Dialect, SQLScriptParser, SQLServerDialect}
import javax.swing.{SwingUtilities, _}
import javax.swing.filechooser.FileFilter
import org.graphstream.graph.implementations._
import org.graphstream.ui.swingViewer._
import org.graphstream.ui.view._
import visitors.DependencyVisitor

object MainGUI  extends App with ActionListener with ViewerListener {

	private val search_icon = "C:\\Users\\mdivincenzo\\Documents\\Scala\\Lineage\\resources\\search.png"
	private val CSS_PATH = "C:\\Users\\mdivincenzo\\Documents\\Scala\\Lineage\\graph.css"
	private val dialects = Map[String, Dialect](("SQLServer" -> new SQLServerDialect),("ANSI" -> new AnsiDialect))

	private var graphMap: collection.mutable.Map[String, List[String]] = collection.mutable.Map[String, List[String]]()
	private var viewer:Viewer = _
	private var graphPanel:ViewPanel = _
	private var graph:MultiGraph = _
	private var fromViewer:ViewerPipe= _

	// Per gestire il loop eventi dal grafo
	private var loop:Boolean = true

	override def viewClosed(viewName: String) = loop = false

	// Metodo invocato quando si pusha su un nodo
	override def buttonPushed(id: String) = {}

	// Metodo invocato quando si alza il click su un nodo
	override def buttonReleased(id: String): Unit = {
		val node = graph.getNode[MultiNode](id)
		if (node.hasAttribute("ui.class") && node.getAttribute[String]("ui.class") == "clicked") {
			node.removeAttribute("ui.class")
			node.addAttribute("ui.class", "")
			return
		}
		if (!node.hasAttribute("ui.class") || (node.hasAttribute("ui.class") && node.getAttribute[String]("ui.class")==""))
			node.addAttribute("ui.class", "clicked")

	}

	override def actionPerformed(e: ActionEvent): Unit = {
		/*  MI DISPIACE che sia codice così "javoso" qua, si potrebbe rifattorizzare */
		if (e.getSource == fileChoose) {
			// File chooser button
			val fc = new JFileChooser()
			fc.setFileFilter(new FileFilter {
				override def accept(f: File): Boolean = f.getName().toLowerCase().endsWith(".sql") || f.isDirectory

				override def getDescription: String = "Only SQL files"
			})
			fc.setDialogTitle("Choose an SQL file to open")
			fc.showOpenDialog(frame)
			if (fc.getSelectedFile != null) {
				val parsedMap = Controller.parse(fc.getSelectedFile.getAbsolutePath, dialects(dialectChooser.getSelectedItem.toString)).toSeq
				graphMap = collection.mutable.Map[String, List[String]]((graphMap.toSeq ++ parsedMap).groupBy(_._1).mapValues(_.map(_._2).flatten.toList).toSeq: _*)
				updateGraph(graphMap.toMap)
			}
		}

		if (e.getSource == search) {
			// Filtraggio del grafo
			val getMe = tf.getSelectedItem.toString
			updateGraph(graphMap.toMap.filter( t => t._1 == getMe), false)
		}

		if (e.getSource == reset) {
			// Reset del filtro
			updateGraph(graphMap.toMap)
		}
	}

	// raw sarà la mappa degli archi del grafo: NODO -> LISTA DI NODI CONNESSI
	private def createGraph(raw:Map[String, List[String]], id:String = "dependencies") : Viewer = {
		// 1. Estraggo i nodi complessivi da rappresentare, guardando la distinct dall'unione delle liste e delle chiavi
		val nodi = raw.keySet ++ raw.values.flatten.toSet
		graph = new MultiGraph("embedded")

		// 2. Li aggiungo al grafo
		nodi.foreach(n => graph.addNode[SingleNode](n).setAttribute("ui.label", n))

		// 3. Scorro la mappa
		// per ciascuna coppia (NODO; LISTA), scorro la lista, ed aggiungo un nodo
		raw.foreach( (mapEntry) => mapEntry._2.foreach(dep => graph.addEdge[AbstractEdge](mapEntry._1 + dep, dep, mapEntry._1,  true)))

		// Visualizzazione
		graph.addAttribute("ui.stylesheet", "url(file:///" + CSS_PATH + ")")
		graph.addAttribute("ui.antialias")

		new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD)

	}


	private def updateGraph(graphMap:Map[String, List[String]], filterCombobox:Boolean = true): Unit = {
		if (graphPanel != null) {
			frame.remove(graphPanel)
			frame.remove(topLine)
			frame.remove(bottomLine)
		}
		viewer = createGraph(graphMap)
		graphPanel = viewer.addDefaultView(false)
		graphPanel.addMouseWheelListener(new MouseWheelListener {
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
		})

		frame.getContentPane.add(BorderLayout.CENTER, graphPanel)
		graphPanel.setLayout(new BorderLayout)
		graphPanel.add(BorderLayout.PAGE_START, topLine)
		if (graphMap.keySet.size > 0)
			graphPanel.add(BorderLayout.SOUTH, bottomLine)

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
		viewer.enableAutoLayout()

		if (filterCombobox) {
			tf.removeAllItems()
			graphMap.keySet.toList.sorted.foreach(tf.addItem(_))
		}

		fromViewer = viewer.newViewerPipe
		fromViewer.addViewerListener(this)
		fromViewer.addSink(graph)


		frame.setSize(1000, 800)
		SwingUtilities.updateComponentTreeUI(frame)

	}


	val frame = new JFrame("Lineage")

	val fileChoose = new JButton ("Import script")
	fileChoose.addActionListener(this)


	val dialectChooser = new JComboBox[String](dialects.keySet.toArray)

	val topLine: JPanel = new JPanel // the panel is not visible in output
	topLine.add(fileChoose)
	topLine.add(dialectChooser)
	topLine.setBackground(Color.WHITE)


	//Creazione Barra in basso
	val bottomLine: JPanel = new JPanel // the panel is not visible in output
	val label: JLabel = new JLabel("Search for table")
	val tf: JComboBox[String] = new JComboBox[String](graphMap.keys.toArray)
	tf.setEditable(true)
	val search: JButton = new JButton("Search")
	val reset: JButton = new JButton("Reset")
	//search.setIcon(new ImageIcon(search_icon))
	search.addActionListener(this)
	reset.addActionListener(this)

	bottomLine.add(label)
	bottomLine.add(tf)
	bottomLine.add(search)
	bottomLine.add(reset)

	this.updateGraph(graphMap.toMap)

	frame.setVisible(true)

	while (loop) { if (fromViewer != null) fromViewer.pump() }


}