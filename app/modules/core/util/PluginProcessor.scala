package modules.core.util

import modules.core.model.{Constraint, ConstraintType, PluginSpec, PluginType, PropertyType}

trait PluginProcessor {

  def deriveConstraintsFromPlugin(pluginType: PluginType.Type): Seq[Constraint] = {
    PluginSpec.getSpecFromType(pluginType).map(propertySpec => {
      val (key: String, propertyType: PropertyType.Value) = propertySpec
      Constraint(0, ConstraintType.HasProperty, key, propertyType.name, Option(pluginType), 0)
    }).toSeq
  }

}
