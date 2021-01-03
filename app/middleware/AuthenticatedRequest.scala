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

package middleware

import modules.auth.model.Ticket
import play.api.mvc._

/**
 * The AuthenticationRequest is a custom request wrapper to bind the authentication Ticket.
 *
 * @param ticket generated authentication Ticket
 * @param request original http request
 * @tparam A request type
 */
class AuthenticatedRequest[A](val ticket: Ticket, request: Request[A]) extends WrappedRequest[A](request)

