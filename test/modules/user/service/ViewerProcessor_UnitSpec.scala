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

package modules.user.service

import modules.core.model.{Viewer, ViewerRole}
import modules.user.model.{Group, ViewerCombinator}
import org.scalatestplus.play.PlaySpec

class ViewerProcessor_UnitSpec extends PlaySpec {

  private object ViewerProcessorWrapper extends ViewerProcessor

  private val aGroup = Group(1, "A")
  private val bGroup = Group(2, "B")
  private val cGroup = Group(3, "C")
  private val dGroup = Group(4, "D")

  "getViewerChanges" must {
    "return the correct update values" in {

      val oldMaintainers = Set(aGroup)
      val oldEditors = Set(bGroup)
      val oldViewers = Set(cGroup)

      val maintainers: Set[String] = Set(aGroup.name)
      val editors: Set[String] = Set()
      val viewers: Set[String] = Set(bGroup.name, dGroup.name)

      val oldViewerCombinator = ViewerCombinator(oldViewers, oldEditors, oldMaintainers)
      val allGroups = Seq(aGroup, bGroup, cGroup, dGroup)
      val result = ViewerProcessorWrapper.getViewerChanges(maintainers, editors, viewers, oldViewerCombinator, allGroups, 1)
      val (toDeleteIds, toInsertViewers) = result
      toDeleteIds mustBe Set(bGroup.id, cGroup.id)
      toInsertViewers mustBe Set(Viewer(0, 1, bGroup.id, ViewerRole.VIEWER), Viewer(0, 1, dGroup.id, ViewerRole.VIEWER))
    }
  }

}
