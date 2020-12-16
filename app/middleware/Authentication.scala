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

import auth.model.Ticket
import play.api.mvc._

import scala.concurrent.Future

/**
 * Authentication trait to provide unspecific functionality for authentication management in Controllers and
 * other middleware classes.
 */
trait Authentication {

  /**
   * Add the session key to the response headers.
   * This method performs no service or repository actions.
   *
   * @param result
   * @param key
   * @return
   */
  def grantAuthentication (result: Result, key: String): Future[Result] = {
    Future.successful(result.withSession("key" -> key))
  }

  /**
   * Extracts the session key from the request headers.<br />
   * Note: the extracted session key should always be the "CompoundSessionKey"
   *
   * @param request http request
   * @tparam T request type (can be let as AnyType here)
   * @return CompoundSessionKey of the request or None
   */
  def getSessionKey[T] (request: Request[T]): Option[String] = request.session.get("key")

  /**
   * Remove the session key from the response headers.
   * This method performs no service or repository actions.
   *
   * @param result
   * @return
   */
  def removeAuthentication (result: Result): Future[Result] = {
    Future.successful(result.withNewSession)
  }

  /**
   * Extracts the ticket object from the current AuthenticatedRequest
   *
   * @param ticketBlock a result function which needs a Ticket object
   * @param request the implicit AuthenticatedRequest (contains the ticket)
   * @tparam A request type
   * @return ticketBlock with applied ticket parameter
   */
  def withTicket[A] (ticketBlock: Ticket => Future[Result])(implicit request: AuthenticatedRequest[A]): Future[Result] = {
    ticketBlock(request.ticket)
  }
}