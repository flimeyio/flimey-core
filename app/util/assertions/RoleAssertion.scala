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

package util.assertions

import auth.model.Ticket
import user.model.Role

trait RoleAssertion {

  def assertWorker(implicit ticket: Ticket): Unit = {
    if(!Role.isAtLeastWorker(ticket.authSession.role)) throw new Exception("No Rights!")
  }

  def assertModeler(implicit ticket: Ticket): Unit = {
    if(!Role.isAtLeastModeler(ticket.authSession.role)) throw new Exception("No Rights!")
  }

  def assertAdmin(implicit ticket: Ticket): Unit = {
    if(!Role.isAtLeastAdmin(ticket.authSession.role)) throw new Exception("No Rights!")
  }

  def assertSystem(implicit ticket: Ticket): Unit = {
    if(!Role.isAtLeastSystem(ticket.authSession.role)) throw new Exception("No Rights!")
  }

}
