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

package modules.subject.service

import modules.core.model.{ConstraintType, PropertyType}

/**
 * Object with static helper functionality for Constraints used by Collections.
 */
object CollectionConstraintSpec {

  val COLLECTION: String = "collection"

  /**
   * Sequence of possible parent types.
   */
  val canDeriveFrom: Seq[String] = Seq[String](COLLECTION)

  /**
   * Sequence of possible property data types.
   */
  val hasPropertyTypes: Seq[String] = PropertyType.values.map(_.name).toSeq

  /**
   * Sequence of allowed constraint types of an asset
   */
  val allowedConstraintTypes: Seq[ConstraintType.Type] = Seq(
    ConstraintType.MustBeDefined,
    ConstraintType.HasProperty,
    ConstraintType.DerivesFrom,
    ConstraintType.CanContain,
    ConstraintType.UsesPlugin
  )
}
