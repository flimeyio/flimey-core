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

import org.scalatestplus.play.PlaySpec

class CredentialProcessor_UnitSpec extends PlaySpec {

  //because CredentialProcessor is a Trait, we need a class or object to bind it
  object CredentialProcessorWrapper extends CredentialProcessor

  "validateUsername" must {
    "return true if username has the allowed length" in {
      val username = "Bill"
      CredentialProcessorWrapper.validateUsername(username).valid mustBe true
    }
    "return false and give an error if username is to short" in {
      val username = ""
      val result = CredentialProcessorWrapper.validateUsername(username)
      result.valid mustBe false
      result.msg mustBe Some("Username must be shorter longer than 0 and than 64 characters")
    }
    "return false and give an error if username is to long" in {
      //construct a 65 char long username
      val username: String = "a" * 65
      val result = CredentialProcessorWrapper.validateUsername(username)
      result.valid mustBe false
      result.msg mustBe Some("Username must be shorter longer than 0 and than 64 characters")
    }
  }

  //TODO validatePassword

}
