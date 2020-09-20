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

package middleware

import model.group.Group
import model.user.User
import play.api.mvc.{AnyContent, Request, Result}

import scala.concurrent.Future

trait Authentication {

  def withAuthentication (result: Result, user: User, groups: Seq[Group]): Future[Result] = {
    //TODO generate key and add db session entry
    Future.successful(result.withSession("key" -> "foobar"))
  }

  def withoutAuthentication (result: Result): Future[Result] = {
    //TODO clear db session entry
    Future.successful(result.withNewSession)
  }

  def hasSession (implicit request: Request[AnyContent]): Boolean = {
    request.session.get("key").isDefined
  }

//  def getAuthentication (implicit request: Request[AnyContent]): Future[] = {
//    //TODO ... add also implicit fallback which is the defined on top of every controller
//  }
}
