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

package modules.user.repository

import integrationhelper.FlimeyIntegrationSpec
import modules.user.model.{Role, User}

class UserRepository_IntegrationSpec extends FlimeyIntegrationSpec {

  override def beforeAll(): Unit = super.beforeAll()

  override def afterAll(): Unit = super.afterAll()

  "addUser" should {
    "return the id of the inserted user" in {
      val userRepository = injector.instanceOf[UserRepository]
      val user = User(0, "Bill", Option("a@b.de"), Option("12345678"), Role.WORKER, None, true, true)
      val futureRes = userRepository.add(user)
      whenReady(futureRes) { res =>
        res > 0 mustBe true
      }
    }
  }

  "getUsers" should {
    val userRepository = injector.instanceOf[UserRepository]

    "return a not authenticated system user" in {
      whenReady(userRepository.getAllWithPendingAuthentication) { res =>
        res.length mustBe 1
        val user = res.head
        user.username mustBe "System"
        user.email mustBe None
        user.role mustBe Role.SYSTEM
      }
    }
    "return an authenticated user we previously added" in {
      whenReady(userRepository.getAllAuthenticated) { res =>
        res.length mustBe 1
        val user = res.head
        user.username mustBe "Bill"
        user.email mustBe Some("a@b.de")
        user.role mustBe Role.WORKER
      }
    }
  }


}
