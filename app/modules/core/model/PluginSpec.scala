/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2021 Karl Kegel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * */

package modules.core.model

object PluginSpec {

  def withNameConstraints: Map[String, PropertyType.Value] = Map(
    "Name" -> PropertyType.StringType
  )

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
    "Priority" -> PropertyType.NumericType
  )

  def getSpecFromType(pluginType: PluginType.Type): Map[String, PropertyType.Value] = pluginType match {
    case t if t == PluginType.WithName => withNameConstraints
    case t if t == PluginType.TimedInterval => timedIntervalConstraints
    case t if t == PluginType.Milestone => milestoneConstraints
    case t if t == PluginType.CostAccumulation => costAccumulationConstraints
    case t if t == PluginType.TimeAccumulation => timeAccumulationConstraints
    case t if t == PluginType.WithPriority => withPriorityConstraints
    case _ => throw new Exception("PluginType has no defined Constraint model")
  }
}
