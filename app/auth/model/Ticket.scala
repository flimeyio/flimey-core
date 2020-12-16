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

package auth.model

import group.model.Group

/**
 * The Ticket data class.<br />
 * A Ticket is a non persistent. It stores data at runtime to provide all authentication based information of
 * a authenticated User.
 *
 * @param authSession AuthSession object of the Users current session
 * @param groups all Groups the User is Member.
 */
case class Ticket(authSession: AuthSession, groups: Seq[Group])
