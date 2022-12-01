package gui

import area.AreaUtils.Area

import java.awt.{Color, Component, Dimension, Font, Graphics}
import javax.swing.border.Border
import javax.swing.{BorderFactory, BoxLayout, JButton, JLabel, JPanel, SwingConstants}

class FireStationJPanel(width: Int, height: Int, area: Area) extends JPanel:
  self =>
  this.setBackground(Color.LIGHT_GRAY)
  this.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2))
  this.setLayout(BoxLayout(this, BoxLayout.Y_AXIS))
  val statePanel = JPanel()
  statePanel.setBackground(Color.WHITE)
  this.add(statePanel)
  this.add(createLabel(s"Area $area state: OK"))
  this.add(createButton("Disable alarm"))
  var map: Map[Int, JLabel] = Map()

  override def getPreferredSize = new Dimension(width, height)

  def printWaterLevel(sensorId: Int, waterLevel: Int): Unit =
    if !map.contains(sensorId) then
      map = map + (sensorId -> createLabel())
      statePanel.removeAll()
      map = Map(map.toSeq.sortWith(_._1 < _._1):_*)
      map.values.foreach(statePanel.add(_))

    map(sensorId).setText(s"Sensor $sensorId register $waterLevel water level")

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
    statePanel.add(label)
    label

