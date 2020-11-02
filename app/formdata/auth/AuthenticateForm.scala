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

package formdata.auth

import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._

object AuthenticateForm {

  case class Data(email: String, key: String, password: String, agree: Boolean)

  val form: Form[Data] = Form(
    mapping(
      "email" -> email.verifying(emailAddress),
      "key" -> text,
      "password" -> text,
      "agree" -> boolean
    )(Data.apply)(Data.unapply)
  )

}
