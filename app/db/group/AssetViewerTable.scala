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

package db.group

import model.group.AssetViewer
import slick.jdbc.MySQLProfile.api._

class AssetViewerTable(tag: Tag) extends Table[AssetViewer](tag, "asset_viewer") {

  def id = column[Long]("id", O.PrimaryKey,O.AutoInc)
  def assetId = column[Long]("asset_id")
  def groupId = column[Long]("group_id")

  override def * = (id, assetId, groupId) <> (AssetViewer.tupled, AssetViewer.unapply)

}