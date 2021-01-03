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

package collection.model

/**
 * Collection is a subtype of Subject and represent the accumulation of several other Subjects.
 * In other words, a Collection is a Subject tree node.
 * <p> Has a repository representation.
 *
 * @param id unique identifier
 * @param subjectId id of the parent Subject
 * @param typeId id of the parent SubjectType
 */
case class Collection(id: Long, subjectId: Long, typeId: Long)
