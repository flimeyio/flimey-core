package modules.core.model

object PluginSpec {

  def timedIntervalConstraints: Map[String, PropertyType.Value] = Map(
    "Start Date" -> PropertyType.DateTimeType,
    "End Date" -> PropertyType.DateTimeType
  )

  def milestoneConstraints: Map[String, PropertyType.Value] = Map(
    "Final Date" -> PropertyType.DateTimeType
  )

  def costAccumulationConstraints: Map[String, PropertyType.Value] = Map(
    "Expected Costs" -> PropertyType.NumericType,
    "Actual Costs" -> PropertyType.NumericType
  )

  def timeAccumulationConstraints: Map[String, PropertyType.Value] = Map(
    "Expected Time" -> PropertyType.NumericType,
    "Actual Time" -> PropertyType.NumericType
  )

  def withPriorityConstraints: Map[String, PropertyType.Value] = Map(
    "Final Date" -> PropertyType.DateTimeType
  )

  def getSpecFromType(pluginType: PluginType.Type): Map[String, PropertyType.Value] = pluginType match {
    case t if t == PluginType.TimedInterval => timedIntervalConstraints
    case t if t == PluginType.Milestone => milestoneConstraints
    case t if t == PluginType.CostAccumulation => costAccumulationConstraints
    case t if t == PluginType.TimeAccumulation => timeAccumulationConstraints
    case t if t == PluginType.WithPriority => withPriorityConstraints
    case _ => throw new Exception("PluginType has no defined Constraint model")
  }
}
