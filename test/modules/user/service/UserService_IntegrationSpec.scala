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

package modules.user.service

import integrationhelper.FlimeyAuthenticatedIntegrationSpec
import modules.auth.model.Ticket
import modules.user.model.Role

class UserService_IntegrationSpec extends FlimeyAuthenticatedIntegrationSpec {

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  override def afterAll(): Unit = super.afterAll()

  "createUser" should {

    //create a new UserService instance with app configurations
    val userService = injector.instanceOf[UserService]

    "create a new user with correct input without failure" in {

      //get the ticket of the system account to perform admin actions
      implicit val ticket: Ticket = systemUserTicket.get

      val futureRes = userService.createUser("Bill", Role.WORKER.toString)
      whenReady(futureRes) { res =>
        res > 0 mustBe true
      }

    }
  }

}
