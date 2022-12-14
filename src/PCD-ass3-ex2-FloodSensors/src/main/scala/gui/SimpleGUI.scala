package gui

import area.AreaUtils.*
import area.FireStation
import gui.MyColors.*
import java.awt.event.ActionListener
import javax.swing.border.Border
import javax.swing.*
import java.awt.*
import scala.collection.mutable

class SimpleGUI(val width: Int, val height: Int, idArea: Area):
  self => // self-types, used to take the val reference inside the inner class
  private case class GUISensor(
      id: ID,
      x: Int,
      y: Int,
      label: JLabel,
      var waterLevel: WaterLevel = -1,
      var alarm: Boolean = false,
      area: Area
  )
  // sizes
  private final val hunit: Int = areaHeightUnit // alias
  private final val wunit: Int = areaWidthUnit // alias
  private final val stationPanelSize: Int = 250
  private val elementWidth = width / 40 + height / 40

  // panels
  private val frame = JFrame()
  private val canvas = Environment()
  private val fireStationPanel = new FireStationJPanel(stationPanelSize, 200, idArea)
  frame.setTitle(s"Fire Station GUI of area $idArea")
  frame.setSize(width + stationPanelSize, height)
  frame.setVisible(true)
  canvas.setVisible(true)
  fireStationPanel.setVisible(true)
  if idArea == 1 then frame.setLocation(elementWidth, hunit)
  else frame.setLocation(width + stationPanelSize, hunit)
  canvas.setSize(width, height)
  fireStationPanel.setSize(200, height)
  frame.getContentPane.setLayout(BorderLayout(0, 0))
  frame.getContentPane.add(canvas, BorderLayout.CENTER)
  frame.getContentPane.add(fireStationPanel, BorderLayout.EAST)

  // gui area state
  var activeSensors: Set[ID] = Set.empty
  private val sensors: mutable.LinkedHashMap[ID, GUISensor] = mutable.LinkedHashMap()

  def updateWaterLevel(sensorId: ID, waterLevel: WaterLevel, alarm: Boolean = false): Unit =
    sensors(sensorId).waterLevel = waterLevel
    if alarm then sensors(sensorId).alarm = true
    else sensors(sensorId).alarm = false
    render()


  def addNewSensor(sensorId: ID, x: Int, y: Int, area: Area): Unit =
    sensors.put(sensorId, GUISensor(sensorId, x, y, createLabel(), area = area))
    render()

  def setActiveSensors(set: Set[ID]): Unit =
    activeSensors = set
    render()

  def render(): Unit = SwingUtilities.invokeLater { () =>
    canvas.invalidate()
    canvas.repaint()
    fireStationPanel.update()
  }

  def addActionListenerForButton(actionListener: ActionListener): Unit =
    fireStationPanel.button.addActionListener(actionListener)

  private class Environment() extends JPanel:
    override def getPreferredSize = new Dimension(self.width, self.height)
    override def paintComponent(graphics: Graphics): Unit =
      graphics.clearRect(0, 0, self.width, self.height)

      fillArea(BLUE, 0, 0, wunit * 3, self.height)
      fillArea(GREEN, wunit * 3, 0, wunit * 2, self.height)

      graphics.setColor(Color.BLACK)
      self.sensors.values
        .filter(s => self.activeSensors.contains(s.id))
        .foreach(s => drawSensor(Color.WHITE, s.x, s.y, s.id))
      self.sensors.values
        .filter(s => !self.activeSensors.contains(s.id))
        .foreach(s => drawSensor(Color.GRAY, s.x, s.y, s.id))
      self.sensors.values
        .filter(s => self.activeSensors.contains(s.id))
        .filter(s => s.alarm)
        .foreach(s => drawSensor(Color.RED, s.x, s.y, s.id))

      def fillArea(c: Color, x: Int, y: Int, w: Int, h: Int): Unit =
        graphics.setColor(c)
        graphics.fillRect(x, y, w, h)

      def drawSensor(c: Color = Color.WHITE, x: Int, y: Int, id: ID): Unit =
        graphics.setColor(c)
        graphics.fillOval(x, y, elementWidth, elementWidth)
        graphics.setColor(Color.BLACK)
        graphics.drawOval(x, y, elementWidth, elementWidth)
        graphics.drawString(id.toString, x + elementWidth / 3, y + elementWidth / 4 * 3)

  private class FireStationJPanel(width: Int, height: Int, area: Area) extends JPanel:
    this.setBackground(Color.GREEN)
    this.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2))
    this.setLayout(BoxLayout(this, BoxLayout.Y_AXIS))
    val statePanel: JPanel = JPanel()
    statePanel.setBackground(Color.WHITE)
    this.add(statePanel)
    this.add(createLabel("Alarm will activates after water level 100"))
    val stateLabel: JLabel = createLabel(s"Area $area state: OK")
    this.add(stateLabel)
    val button: JButton = createButton("Disable alarm ( -30 water lvl)")
    this.add(button)

    override def getPreferredSize = new Dimension(width, height)

    def update(): Unit =
      statePanel.removeAll()
      val areaSensors = self.sensors.values.filter(_.area == idArea)
      areaSensors
        .foreach(s =>
          statePanel.add(s.label)
          if self.activeSensors.contains(s.id) then
            s.label.setText(s"Sensor ${s.id} detect ${s.waterLevel} water level")
          else s.label.setText(s"Sensor ${s.id} probably is offline")
        )
      if areaSensors.nonEmpty && areaSensors.forall(_.alarm) then
        this.setBackground(MyColors.RED)
        stateLabel.setText(s"Area $area state: Alarm")
      else
        this.setBackground(Color.GREEN)
        stateLabel.setText(s"Area $area state: OK")


    def createButton(name: String): JButton =
      val b = new JButton(name):
        override def getMaximumSize: Dimension = Dimension(self.width, 20)
      b.setAlignmentX(Component.CENTER_ALIGNMENT)
      b

  def createLabel(text: String = "default text"): JLabel =
    val label = JLabel(text)
    val size = 5
    label.setAlignmentX(Component.CENTER_ALIGNMENT)
    label.setBorder(BorderFactory.createEmptyBorder(size, size, size, size))
    label
