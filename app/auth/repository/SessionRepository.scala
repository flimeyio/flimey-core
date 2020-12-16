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

package auth.repository

import auth.model.{Access, AuthSession}
import com.google.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

/**
 * DB interface for Sessions and temporary Access right management.
 * Provided methods are UNSAFE and must only be used by service classes!
 *
 * @param dbConfigProvider injected db config
 * @param executionContext future execution context
 */
class SessionRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val sessions = TableQuery[AuthSessionTable]
  val accesses = TableQuery[AccessTable]

  /**
   * Add an AuthSession with Accesses to the db.
   * The AuthSession and all Accesses must have id set to 0 to enable auto increment.
   * During insert, the newly created id of the AuthSession will be mapped to all Access entities.
   *
   * @param session AuthSession of a User
   * @param rights  access rights associated to the session
   * @return new session id future
   */
  def add(session: AuthSession, rights: Seq[Access]): Future[Long] = {
    db.run((for {
      sessionId <- (sessions returning sessions.map(_.id)) += session
      _ <- accesses ++= rights.map(p => Access(0, sessionId, p.groupId, p.groupName))
    } yield sessionId).transactionally)
  }

  /**
   * Get an AuthSession with all associated Access objects.
   *
   * @param id of the AuthSession
   * @return AuthSession with rights or None
   */
  def getComplete(id: Long): Future[Option[(AuthSession, Seq[Access])]] = {
    db.run((for {
      (c, s) <- sessions.filter(_.id === id) joinLeft accesses on (_.id === _.sessionId)
    } yield (c, s)).result).map(res => {
      if (res.isEmpty) {
        None
      } else {
        //btw headOption is valid here, because the result map can only have one entry, because the db query filters by prim key
        res.groupBy(_._1).mapValues(values => values.map(_._2).filter(_.isDefined).map(_.get)).headOption
      }
    })
  }

  /**
   * Delete an AuthSession.
   * This operation also deletes all associated Access entities.
   *
   * @param id of the AuthSession
   * @return Unit
   */
  def delete(id: Long): Future[Unit] = {
    db.run((for {
      _ <- accesses.filter(_.sessionId === id).delete
      _ <- sessions.filter(_.id === id).delete
    } yield ()).transactionally)
  }

  /**
   * Delete all AuthSession of a specified User.
   * This operation also deletes all associated Access entities.
   *
   * @param userId id of the User
   * @return Unit
   */
  def deleteAll(userId: Long): Future[Unit] = {
    db.run((for {
      _ <- accesses.filter(_.sessionId in sessions.filter(_.userId === userId).map(_.id)).delete
      _ <- sessions.filter(_.userId === userId).delete
    } yield ()).transactionally)
  }

}
