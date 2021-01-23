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
import modules.user.model.User
import play.db.NamedDatabase

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB interface for Users.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected db config
 * @param executionContext future execution context
 */
class UserRepository @Inject()(@NamedDatabase("flimey_data") protected val dbConfigProvider: DatabaseConfigProvider)(
  implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  val users = TableQuery[UserTable]
  val groupMemberships = TableQuery[GroupMembershipTable]

  /**
   * Add a new User to the db.
   * The id must be set to 0 to enable auto increment.
   *
   * @param user new User entity
   * @return new id future
   */
  def add(user: User): Future[Long] = {
    db.run((users returning users.map(_.id)) += user)
  }

  /**
   * Update an existing User.
   *
   * @param user User entity to update
   * @return result future
   */
  def update(user: User): Future[Int] = {
    db.run(users.filter(_.id === user.id).update(user))
  }

  /**
   * Delete an existing User.
   * This operation also deletes all GroupMemberships of the User.
   *
   * @param id of the User to be deleted
   * @return Unit
   */
  def delete(id: Long): Future[Unit] = {
    db.run((for {
      _ <- groupMemberships.filter(_.userId === id).delete
      _ <- users.filter(_.id === id).delete
    } yield ()).transactionally)
  }

  /**
   * Get a User by its primary key (id)
   *
   * @param id of the User to fetch
   * @return the found User or None
   */
  def getById(id: Long): Future[Option[User]] = {
    db.run(users.filter(_.id === id).result.headOption)
  }

  /**
   * Get a User by its email.
   * EMail is a unique field, so always exactly one or no User is found.
   *
   * @param email of the User to to fetch
   * @return the found User or None
   */
  def getByEMail(email: String): Future[Option[User]] = {
    db.run(users.filter(_.email === email).result.headOption)
  }

  /**
   * Get a User by its authentication key.
   * The auth key is a unique field, so always exactly one or no User is found.
   *
   * @param key authentication key of the unauthenticated User to fetch
   * @return the found User or None
   */
  def getByKey(key: String): Future[Option[User]] = {
    db.run(users.filter(_.key === key).result.headOption)
  }

  /**
   * Get all Users which are not authenticated.
   * I.e. get all Users where an auth key is defined.
   *
   * @return Users with defined authentication key
   */
  def getAllWithPendingAuthentication: Future[Seq[User]] = {
    db.run(users.filter(_.key.isDefined).result)
  }

  /**
   * Get all Users which are authenticated.
   * I.e. get all Users which can log in and be member of Groups
   *
   * @return Users with undefined authentication key
   */
  def getAllAuthenticated: Future[Seq[User]] = {
    db.run(users.filter(_.key.isEmpty).result)
  }

}
