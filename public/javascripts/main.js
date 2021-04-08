/*
 * This file is part of the flimey-core software.
 * Copyright (C) 2020  Karl Kegel
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

console.log("Hello flimey!")

// Is on the actual page
function timelineRendering() {

    let containers = $('.meta-entity-container');
    for (let containerIndex = 0; containerIndex < containers.length; containerIndex++) {

        let container = $(containers[containerIndex]);
        let entity = $(container.find(".meta-entity")[0]);
        let viewport = $(container.find(".meta-viewport")[0]);

        let entityData = {
            properties: [],
            subEntities: []
        };

        let propertyList = $(entity.find(".meta-entity-properties")[0]);
        let properties = propertyList.find(".meta-property");

        for (let propIndex = 0; propIndex < properties.length; propIndex++) {
            let prop = $(properties[propIndex]);
            let key = $(prop.find(".meta-property-key")[0]).html();
            let value = $(prop.find(".meta-property-value")[0]).html();
            entityData.properties.push({
                key: key,
                value: value
            })
        }

        let subEntityContainer = $($(".meta-sub-entities")[0])
        let subEntities = subEntityContainer.find(".meta-sub-entity")

        for (let subEntityIndex = 0; subEntityIndex < subEntities.length; subEntityIndex++) {
            let subEntityData = {
                properties: []
            };
            let subEntity = $(subEntities[subEntityIndex])
            let subPropertyList = $(subEntity.find(".meta-sub-entity-properties")[0])
            let subProperties = subPropertyList.find(".meta-property")

            for (let subPropIndex = 0; subPropIndex < subProperties.length; subPropIndex++) {
                let subProp = $(subProperties[subPropIndex])
                let subKey = $(subProp.find(".meta-property-key")[0]).html()
                let subValue = $(subProp.find(".meta-property-value")[0]).html()
                subEntityData.properties.push({
                    key: subKey,
                    value: subValue
                })
            }
            entityData.subEntities.push(subEntityData);
        }
        showVisualization(entityData, viewport);
    }
}

function addRenderingDiv(metaViewport) {
    const divCollTimeline = document.createElement('div');
    const divCollectionsTimeline = document.createElement('div');

    divCollTimeline.className = 'collection-timeline';
    divCollectionsTimeline.className = 'coll-tile-timeline';

    metaViewport.append(divCollTimeline);
    metaViewport.append(divCollectionsTimeline);
}

function showVisualization(data, viewport){

    const mock_data = [
        {collectible_name: "Sägen", start: 800, end: 2000},
        {collectible_name: "Malern", start: 900, end: 1700},
        {collectible_name: "Einkaufen", start: 1000, end: 3000},
        {collectible_name: "Versenden", start: 1200, end: 1800}
    ];

    //const project_collection = {collectible_name: "", start: 600, end: 5000};

    data = parseData(data);
    let collectibles = data.collectibles
    let project_collection = data.collection;

    console.log("data", data);
    console.log("viewport", viewport);
    console.log("collection", project_collection);

    addRenderingDiv(viewport);

    const width = $(viewport.find('.coll-tile-timeline')[0]).width();
    render(project_collection, collectibles, width);
    //TODO
}

function parseData(data) {

    // Helping Function
    // returns 0 if property does not exist
    const getPropertyValue = (props, propString) => {
        for (let i = 0; i < props.length; i += 1) {
            if (props[i].key === propString) {
                return props[i].value;
            }
        }
        return 0;
    };

    // turns the date into milliseconds
    // no considering of the date time
    const dateIntoMilliseconds = stringDate => {
        return stringDate.toDate("dd.mm.yyyy hh:ii:ss").getTime();
    };


    let parsedData = [];
    let collectibles = data.subEntities;

    // Iteration Entities
    for (let index = 0; index < collectibles.length; index += 1) {
        let entityProps = collectibles[index].properties;
        let entityStartDate = getPropertyValue(entityProps, 'Start Date');
        let entityEndDate = getPropertyValue(entityProps, 'End Date');
        
        if (entityStartDate !== 0 && entityEndDate !== 0) {
            parsedData.push({
                collectible_name: getPropertyValue(entityProps, 'Name'),
                start: dateIntoMilliseconds(entityStartDate),
                end: dateIntoMilliseconds(entityEndDate)});
        }
    }

    const project_collection = {
        collectible_name: getPropertyValue(data.properties, 'Name'),
        start: dateIntoMilliseconds(getPropertyValue(data.properties, 'Start Date') + " 00:00:00"),
        end: dateIntoMilliseconds(getPropertyValue(data.properties, 'End Date') + " 00:00:00")
    };
    return {collection: project_collection, collectibles: parsedData};
}


$(document).ready(() => {
    timelineRendering();
})

let scroll_open = true;

document.addEventListener('scroll', function (event) {
    let pos = $("body").scrollTop()
    if (pos > 10 && scroll_open) {
        scroll_open = false;
        $(".main-content").addClass("main-content-scrolled");
        $(".navbar-global").addClass("navbar-global-scrolled");
        $(".navbar-layout").addClass("navbar-layout-scrolled");
        console.log("set closed");
    } else if (pos < 10 && !scroll_open) {
        scroll_open = true
        $(".main-content").removeClass("main-content-scrolled");
        $(".navbar-global").removeClass("navbar-global-scrolled");
        $(".navbar-layout").removeClass("navbar-layout-scrolled");
        console.log("set open");
    }
}, true);