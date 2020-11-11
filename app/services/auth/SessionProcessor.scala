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

package services.auth

/**
 * Trait which provides some unspecific session (key string) processing functionality.
 */
trait SessionProcessor {

  /**
   * Create a CompoundKey which identifies Session and User.
   *
   * @param sessionKey (random) key of the Session.
   * @param sessionId primary key of the Session entity.
   * @return combined keys
   */
  def createCompoundKey(sessionKey: String, sessionId: Long): String = sessionKey + ":" + sessionId.toString

  /**
   * Split the CompoundKey back to its parts.
   *
   * @param compoundKey combined session keys
   * @return (random session key, session primary key)
   */
  def resolveCompoundKey(compoundKey: String): (String, Long) = {
    val parts = compoundKey.split(":".r.regex)
    (parts(0), parts(1).toLong)
  }

}
