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

package modules.collection.model

/**
 * The type a [[Subject]] can have. A SubjectType consists always of multiple [[SubjectConstraint]]s which are associated
 * to the parent SubjectType.
 * <p> Has a repository representation.
 *
 * @param id     unique identifier
 * @param value  unique name of the type
 * @param active flag if the type is active i.e. if instances of this type can be created and edited
 */
case class SubjectType(id: Long, value: String, active: Boolean)
