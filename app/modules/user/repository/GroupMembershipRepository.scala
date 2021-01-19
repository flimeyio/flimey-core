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

package modules.user.repository

import com.google.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import modules.user.model.{Group, GroupMembership, User}
import play.db.NamedDatabase

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB interface for GroupMemberships.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected db config
 * @param executionContext future execution context
 */
class GroupMembershipRepository @Inject()(@NamedDatabase("flimey_data") protected val dbConfigProvider: DatabaseConfigProvider)(
  implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  val groupMemberships = TableQuery[GroupMembershipTable]
  val groups = TableQuery[GroupTable]
  val users = TableQuery[UserTable]

  /**
   * Get all Groups a User is member of.
   *
   * @param userId id of the User
   * @return all groups the User is member of.
   */
  def get(userId: Long): Future[Seq[Group]] = {
    db.run((for {
      (c, s) <- groupMemberships.filter(_.userId === userId) joinLeft groups on (_.groupId === _.id)
    } yield (c, s)).result).map(res => {
      res.map(_._2).filter(_.isDefined).map(_.get)
    })
  }

  /**
   * Get all Users who are members of a given Group.
   *
   * @param groupId id of the Group
   * @return all Users who are member of the Group
   */
  def getMembersOfGroup(groupId: Long): Future[Seq[User]] = {
    db.run((for {
      (c, s) <- groupMemberships.filter(_.groupId === groupId) joinLeft users on (_.userId === _.id)
    } yield (c, s)).result).map(res => {
      res.map(_._2).filter(_.isDefined).map(_.get)
    })
  }

  /**
   * Add a new GroupMembership.
   * I.e. add a User as member to an existing Group.
   * The id must be set to 0 to enable auto increment.
   *
   * @param groupMembership new GroupMembership object
   * @return id of new GroupMembership entity
   */
  def add(groupMembership: GroupMembership): Future[Long] = {
    db.run((groupMemberships returning groupMemberships.map(_.id)) += groupMembership)
  }

  /**
   * Delete a GroupMembership.
   * I.e. remove a User from a Group
   *
   * @param userId id of the User to remove
   * @param groupId id of the Group where to remove the User from
   * @return status future
   */
  def delete(userId: Long, groupId: Long): Future[Int] = {
    db.run(groupMemberships.filter(_.userId === userId).filter(_.groupId === groupId).delete)
  }

}
