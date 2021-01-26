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

package modules.user.service

import modules.util.messages.{ERR, OK, Status}

trait CredentialProcessor {

  /**
   * Check if a username is valid.
   * A valid username has less than 64 characters.
   *
   * @param candidate username
   * @return
   */
  def validateUsername(candidate: String): Status = {
    if(candidate.length < 1 || candidate.length > 64){
      ERR("Username must be shorter longer than 0 and than 64 characters")
    }else{
      OK()
    }
  }

  /**
   * Check if a password is valid.
   * A valid password has between 8 and 65 characters.
   * <br />
   * It will be hashed anyway, so min length is just a protection against guessing.
   * Max length is just to set a transfer limit.
   *
   * @param candidate unencrypted password
   * @return
   */
  def validatePassword(candidate: String): Status = {
    if(candidate.length < 8 || candidate.length > 64){
      ERR("Password must have a length between 8 and 64 characters")
    }else{
      OK()
    }
  }

}
