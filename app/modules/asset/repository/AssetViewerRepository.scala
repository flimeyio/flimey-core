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

package modules.asset.repository

import com.google.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery
import modules.user.model.{Group, Viewer}
import modules.user.repository.GroupTable

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB interface for Asset Viewer relations.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected db config
 * @param executionContext future execution context
 */
class AssetViewerRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val assetViewers = TableQuery[AssetViewerTable]
  val groups = TableQuery[GroupTable]
  val assets = TableQuery[AssetTable]

  /**
   * Add a new AssetViewer.<br />
   * Id must be set to 0 to enable auto increment.
   * Duplications lead to an database exception
   *
   * @param viewer new Viewer object with id = 0
   * @return Future[Long]
   */
  def add(viewer: Viewer): Future[Long] = {
    db.run((assetViewers returning assetViewers.map(_.id)) += viewer)
  }

  /**
   * Delete an AssetViewer by its unique combination of targetId and viewerId.
   *
   * @param targetId id of the target Asset
   * @param viewerId id of the viewing Group
   * @return Future[Long]
   */
  def delete(targetId: Long, viewerId: Long): Future[Int] = {
    db.run(assetViewers.filter(_.targetId === targetId).filter(_.viewerId === viewerId).delete)
  }

  /**
   * Get all Groups which have an AssetViewer relation to an specified Asset.
   *
   * @param assetId id of the Asset which Viewers shall be fetched
   * @return Future Seq[(Group, Viewer)]
   */
  def getAllViewingGroups(assetId: Long): Future[Seq[(Group, Viewer)]] = {
    db.run((for {
      (c, s) <- (assets.filter(_.id === assetId) join assetViewers on (_.id === _.targetId)) join groups on (_._2.viewerId === _.id)
    } yield (c, s)).result).map(res => {
      res.map(data => (data._2, data._1._2))
    })
  }

}
