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

package db.auth

import com.google.inject.Inject
import model.auth.{Access, AuthSession}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SessionRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val sessions = TableQuery[AuthSessionTable]
  val accesses = TableQuery[AccessTable]

  //add new session
  def add(session: AuthSession, rights: Seq[Access]): Future[Unit] = {
    db.run((for {
      key <- (sessions returning sessions.map(_.id)) += session
      _ <- accesses ++= rights.map(p => Access(0, key, p.groupId, p.groupName))
    } yield ()).transactionally)
  }

  //get session by session key (just with role...)
  def getHeader(key: String): Future[Option[AuthSession]] = {
    db.run(sessions.filter(_.session === key).result.headOption)
  }

  def getComplete(id: Long): Future[(Option[AuthSession], Seq[Access])] = {
    db.run((for {
      (c, s) <- sessions.filter(_.id === id) joinLeft accesses on (_.id === _.sessionId)
    } yield (c, s)).result).map(res => {
      if (res.isEmpty) {
        (None, Seq())
      } else {
        res.groupBy(_._1.id).mapValues(values => (values.map(_._1).headOption, values.map(_._2.get))).values.head
      }
    })
  }

  def delete(id: Long): Future[Unit] = {
    db.run((for {
      _ <- accesses.filter(_.sessionId === id).delete
      _ <- sessions.filter(_.id === id).delete
    } yield ()).transactionally)
  }

}
