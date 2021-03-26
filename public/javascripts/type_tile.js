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

function updateTypeTile(typeId){
    //get meta-type-tile with this typeId
    let tile = $("#meta-type-tile-"+typeId)
    let selectedVersion = tile.find(".meta-type-version-select").first().val();

    //get meta-type-version-switch
    let versionViews = tile.find(".meta-type-version-switch .meta-version-view")
    for(let i = 0; i < versionViews.length; i++){
        let versionView = versionViews[i];
        let id = versionView.id;
        if(id === selectedVersion){
            $(versionView).show();
        }else{
            $(versionView).hide();
        }
    }
}