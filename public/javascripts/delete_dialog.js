/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2021 Tom-Maurice Schreiber
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


// Show Dialog
function showConfirmDelDialog(btnDelete) {
    let dialogConfirmDel = getChildElement("del-confirm-dialog", btnDelete.parentNode);

    // Show-Dialog Button is the Delete Button
    let showDialogBtn = getChildElement("btn-showDialog", btnDelete.parentNode);

    console.log(btnDelete.parentNode);
    dialogConfirmDel.hidden = false;
    showDialogBtn.hidden = true;

    // buttons with class button can not be hidden
    //showDialogBtn.classList.remove("button");
}

// Cancel Delete
function cancelDelete(element) {
    let dialogConfirmDel = getChildElement("del-confirm-dialog", element.parentNode.parentNode);
    let showDialogBtn = getChildElement("btn-showDialog", dialogConfirmDel.parentNode);

    dialogConfirmDel.hidden = true;
    showDialogBtn.hidden = false;

    //showDialogBtn.classList.add("button")
}

// find specific child element by className
// className: string
function getChildElement(className, parent) {
    for (let i = 0; i < parent.children.length; i++) {
        if (parent.children[i].classList.contains(className)) {
            return parent.children[i];
        }
    }
}