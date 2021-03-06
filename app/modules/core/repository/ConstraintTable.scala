/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2020-2021 Karl Kegel
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

package modules.core.repository

import modules.core.model.Constraint
import slick.jdbc.PostgresProfile.api._

/**
 * Slick framework db mapping for Constraints.
 * see evolutions/default for schema creation.
 *
 * @param tag for mysql
 */
class ConstraintTable(tag: Tag) extends Table[Constraint](tag, "type_constraint") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def c = column[String]("c")

  def v1 = column[String]("v1")

  def v2 = column[String]("v2")

  def byPlugin = column[Option[String]]("by_plugin")

  def typeVersionId = column[Long]("type_version_id")

  override def * =
    (id, c, v1, v2, byPlugin, typeVersionId) <> (Constraint.tupledRaw, Constraint.unapplyToRaw)

}
