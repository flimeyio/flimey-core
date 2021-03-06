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

/**
 * The ConstraintType enum provides naming definitions for all possible Constraint types.
 * <p> Note that every concrete entity subclass can define its own subset of these Constraint types.
 */
object ConstraintType extends Enumeration {

  type Type = Value

  import scala.language.implicitConversions

  protected case class Val(name: String) extends super.Val

  implicit def valueToType(x: Value): Val = x.asInstanceOf[Val]

  val HasProperty: Val = Val("Has Property")
  val MustBeDefined: Val = Val("Must Be Defined")
  val CanContain: Val = Val("Can Contain")
  val UsesPlugin: Val = Val("Uses Plugin")

  def find(name: String): Option[ConstraintType.Type] = values find (t => t.name == name)

}
