/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2021 Karl Kegel
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

package integrationhelper

import modules.auth.model.Ticket
import modules.auth.service.AuthService
import modules.user.service.UserService

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * A helper superclass to write integration tests which need access to the database.
 * <p> (See [[integrationhelper.FlimeyIntegrationSpec]])
 * <p> Additionally this class does:
 * <p> 1. authenticate the SYSTEM user
 * <p> 2. creates a ticket which can be used to access authenticated methods
 *
 * <p> <strong> Before and after each test suite, the database will be reset automatically! </strong>
 */
abstract class FlimeyAuthenticatedIntegrationSpec extends FlimeyIntegrationSpec {

  protected var systemUserTicket: Option[Ticket] = None

  override def beforeAll(): Unit = {
    super.beforeAll()

    val userService = injector.instanceOf[UserService]
    val authService = injector.instanceOf[AuthService]

    //authenticate the system user and wait until it is ready
    val authRes = userService.authenticateUser("root", "admin@flimey.io", "12345678", agree = true)
    Await.ready(authRes, Duration.Inf)

    //create a session and wait until it is ready
    val sessionRes = authService.createSession("admin@flimey.io", "12345678")
    val sessionCompoundKey = Await.ready(sessionRes, Duration.Inf).value.get.get

    //get the ticket of the user and wait until it is ready
    val ticketRes = authService.getTicket(sessionCompoundKey)
    val ticket = Await.ready(ticketRes, Duration.Inf).value.get.get

    systemUserTicket = Some(ticket)
  }

}