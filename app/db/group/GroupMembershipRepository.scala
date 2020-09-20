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
import model.group.{Group, GroupMembership}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB interface for GroupMemberships.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected db config
 * @param executionContext future execution context
 */
class GroupMembershipRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val groupMemberships = TableQuery[GroupMembershipTable]
  val groups = TableQuery[GroupTable]

  //get groups of a user
  def get(userId: Long): Future[Seq[Group]] = {
    db.run((for {
      (c, s) <- groupMemberships.filter(_.userId === userId) joinLeft groups on (_.groupId === _.id)
    } yield (c, s)).result).map(res => {
      res.map(_._2).filter(_.isDefined).map(_.get)
    })
  }

  //add user to group
  def add(groupMembership: GroupMembership): Future[Long] = {
    db.run((groupMemberships returning groupMemberships.map(_.id)) += groupMembership)
  }

  //remove user from group
  def delete(userId: Long, groupId: Long): Future[Int] = {
    db.run(groupMemberships.filter(_.userId === userId).filter(_.groupId === groupId).delete)
  }

}
