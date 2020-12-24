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

package asset.service

/**
 * Trait which provides some unspecific string processing functionality
 */
trait StringProcessor {

  /**
   * Transforms a string value to lower case and removes all tabs and space characters
   *
   * @param value raw string
   * @return refactored string
   */
  def toLowerCaseNoSpaces(value: String): String = {
    value.toLowerCase.replaceAll("(( )*|\t*)".r.regex, "")
  }

  /**
   * Checks if a given string contains a numeric value.
   * For more flexibility, this function does not make a difference between '.' (dot) and ',' (colon).
   *
   * @param value to check
   * @return result
   */
  def isNumericString(value: String): Boolean = {
    value.matches("^(([0-9]+(\\.|\\,)?)+)$".r.regex)
  }

  /**
   * Splits a string containing of an Int list into single Int values.<br />
   * The string must have the form "v1,v2,v3,..."
   * @param value
   * @return
   */
  def splitNumericList(value: String): Seq[Int] = {
    value.split(",").map(_.toInt)
  }

}
