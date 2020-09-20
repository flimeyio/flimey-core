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

import com.google.inject.Inject
import model.group.Group
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB interface for Groups.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected db config
 * @param executionContext future execution context
 */
class GroupRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val groups = TableQuery[GroupTable]
  val groupMemberships = TableQuery[GroupMembershipTable]
  val assetViewers = TableQuery[AssetViewerTable]

  //put new group
  def add(group: Group): Future[Long] = {
    db.run((groups returning groups.map(_.id)) += group)
  }

  //get group by id
  def getById(id: Long): Future[Option[Group]] = {
    db.run(groups.filter(_.id === id).result.headOption)
  }

  //delete group
  def delete(id: Long): Future[Unit] = {
    db.run((for {
      _ <- assetViewers.filter(_.groupId === id).delete
      _ <- groupMemberships.filter(_.groupId === id).delete
      _ <- groups.filter(_.id === id).delete
    } yield ()).transactionally)
  }


}
