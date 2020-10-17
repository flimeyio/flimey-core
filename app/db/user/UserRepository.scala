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

package db.user

import com.google.inject.Inject
import db.group.GroupMembershipTable
import model.user.User
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB interface for Users.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected db config
 * @param executionContext future execution context
 */
class UserRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val users = TableQuery[UserTable]
  val groupMemberships = TableQuery[GroupMembershipTable]

  //put new user
  def add(user: User): Future[Long] = {
    db.run((users returning users.map(_.id)) += user)
  }

  //update user data
  def update(user: User): Future[Int] = {
    db.run(users.filter(_.id === user.id).update(user))
  }

  //delete user
  def delete(id: Long): Future[Unit] = {
    db.run((for {
      _ <- groupMemberships.filter(_.userId === id).delete
      _ <- users.filter(_.id === id).delete
    } yield ()).transactionally)
  }

  //get user by id
  def getById(id: Long): Future[Option[User]] = {
    db.run(users.filter(_.id === id).result.headOption)
  }

  //get user by email
  def getByEMail(email: String): Future[Option[User]] = {
    db.run(users.filter(_.email === email).result.headOption)
  }

}
