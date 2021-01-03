/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2020  Karl Kegel
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

package modules.asset.service

/**
 * Object with static helper functionality for Constraints used by AssetTypes.
 */
object AssetConstraintHelper {

  /**
   * Enum of available Constraint rules.
   */
  object ConstraintType extends Enumeration {

    import scala.language.implicitConversions
    protected case class Val(short: String, name: String) extends super.Val
    implicit def valueToType(x: Value): Val = x.asInstanceOf[Val]

    val DerivesFrom: Val = Val("DF", "Derives From")
    val HasProperty: Val = Val("HP", "Has Property")
    val MustBeDefined: Val = Val("MD", "Must Be Defined")

  }

  /**
   * Sequence of possible parent types.
   */
  val canDeriveFrom: Seq[String] = Seq[String]("asset")

  /**
   * Enum of available Property data types.
   */
  object PropertyType extends Enumeration {

    import scala.language.implicitConversions
    protected case class Val(name: String) extends super.Val
    implicit def valueToType(x: Value): Val = x.asInstanceOf[Val]

    val StringType: Val = Val("string")
    val NumericType: Val = Val("number")
  }
  /**
   * Sequence of possible property data types.
   */
  val hasPropertyTypes: Seq[String] = PropertyType.values.map(_.name).toSeq

}
