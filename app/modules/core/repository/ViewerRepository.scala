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

package modules.core.repository

import com.google.inject.Inject
import modules.asset.repository.AssetTable
import modules.core.model.Viewer
import modules.user.model.Group
import modules.user.repository.GroupTable
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB interface for Viewer relations.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected db config
 * @param executionContext future execution context
 */
class ViewerRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val viewers = TableQuery[ViewerTable]
  val groups = TableQuery[GroupTable]
  val assets = TableQuery[AssetTable]

  /**
   * Add a new Viewer.<br />
   * Id must be set to 0 to enable auto increment.
   * Duplications lead to an database exception
   *
   * @param viewer new Viewer object with id = 0
   * @return Future[Long]
   */
  def add(viewer: Viewer): Future[Long] = {
    db.run((viewers returning viewers.map(_.id)) += viewer)
  }

  /**
   * Delete a Viewer by its unique combination of targetId and viewerId.
   *
   * @param targetId id of the target entity
   * @param viewerId id of the viewing Group
   * @return Future[Long]
   */
  def delete(targetId: Long, viewerId: Long): Future[Int] = {
    db.run(viewers.filter(_.targetId === targetId).filter(_.viewerId === viewerId).delete)
  }

  /**
   * Get all Groups which have an Viewer relation to a specified Entity.
   *
   * @param entityId id of the Asset which Viewers shall be fetched
   * @return Future Seq[(Group, Viewer)]
   */
  def getAllViewingGroups(entityId: Long): Future[Seq[(Group, Viewer)]] = {
    db.run((for {
      (c, s) <- groups join viewers.filter(_.targetId === entityId) on (_.id === _.viewerId)
    } yield (c, s)).result)
  }

}
