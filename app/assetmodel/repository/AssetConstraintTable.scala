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

package assetmodel.repository

import assetmodel.model.AssetConstraint
import slick.jdbc.MySQLProfile.api._

/**
 * Slick framework db mapping for Asset associated Constraints.
 * see evolutions/default for schema creation.
 * @param tag for mysql
 */
class AssetConstraintTable(tag: Tag) extends Table[AssetConstraint](tag, "asset_constraint") {

  def id = column[Long]("id", O.PrimaryKey,O.AutoInc)
  def c = column[String]("c")
  def v1 = column[String]("v1")
  def v2 = column[String]("v2")
  def typeId = column[Long]("type_id")

  override def * =
    (id, c, v1, v2, typeId) <>(AssetConstraint.tupled, AssetConstraint.unapply)

}
