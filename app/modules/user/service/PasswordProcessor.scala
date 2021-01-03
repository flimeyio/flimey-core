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

import org.mindrot.jbcrypt.BCrypt

/**
 * Trait which provides functionality for encrypting and comparing passwords.
 */
trait PasswordProcessor {

  /**
   * Hash an unencrypted password.
   *
   * @param password unencrypted password
   * @return password hash
   */
  def hashPassword(password: String): String = {
    BCrypt.hashpw(password, BCrypt.gensalt());
  }

  /**
   * Compare a unencrypted password the a password hash.
   *
   * @param hashedPassword encrypted password
   * @param password unencrypted password
   * @return true, if passwords match
   */
  def checkPassword(hashedPassword: String, password: String): Boolean = {
    BCrypt.checkpw(password, hashedPassword)
  }

}
