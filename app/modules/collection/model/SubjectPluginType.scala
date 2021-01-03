/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2021  Karl Kegel
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

package modules.collection.model

/**
 * Enumeration representing the possible Plugin types of a [[Subject]]
 */
object SubjectPluginType extends Enumeration {

  type Type = Value

  import scala.language.implicitConversions
  protected case class Val(name: String) extends super.Val
  implicit def valueToType(x: Value): Val = x.asInstanceOf[Val]

  val TimedInterval: Val = Val("Derives From")
  val Milestone: Val = Val("Has Property")
  val TimeAccumulation: Val = Val("Must Be Defined")
  val CostAccumulation: Val = Val("Cost Accumulation")
  val WithPriority: Val = Val("Prioritizing")

}
