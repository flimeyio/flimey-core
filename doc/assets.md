# Assets

## Outline

Assets are a basic element of the flimey system. There, assets can be used to store any kind of static data or datasets
like it would be done within a database. By default, flimey defines no specific types of assets by itself. However, a user
with *modeler* rights can define any kind of asset types for system wide use.

## Asset Types

An asset type is a named model of constraints created by a *modeler* user. 

For example the system should be able to save contact information of customers as assets. Therefor an asset type with the
name "Contact" could be created. Then within an editor only the *modeler* can see, a number of constraints (or properties)
for the "Contact" can bes specified. Such a specification can look like the following:

```
TYPE OF             asset
HAS PROPERTY        Name        string
HAS PROPERTY        Mobile      numeric
HAS PROPERTY        Address     string
MUST BE DEFINED     Name        surename
```

With those constraints, a "Contact" asset has the fields *Name*, *Mobile* and *Address*. The *Name* and *Address* are of
the type string, so they can be filled with any textual value. The *Mobile* field must be a numeric value, so it can contain
only numbers and punctuation. Additionally, the *Name* must always be defined, so this field is not allowed to be empty.

## Asset Management

In the asset section, the user can search for, filter, create and delete assets of every defined type.

Note that the visibility of assets and edit rights are not the same for every user but are dependent of the group. See 
*groups.md* for more information.  

##For Developers

## #Usecases

ID|Name|Description|Status
---|---|---|---
A01|Create AssetType|a modeler can create a new asset type|working
A02|Edit AssetType|a modeler can edit an asset type (enable/disable, add/delete constraints)|works partly
A03|Delete AssetType|a modeler can delete an asset type|works partly
A04|Add Asset|a user can create an asset of an existing type|working
A05|Edit Asset|a user can edit existing assets (change their property values)|working
A06|Delete Asset|a user can delete an existing asset (and all attachments)|works partly
A07|View Assets|a user can view all assets of a given type|works
A08|Search Assets|a user can search for assets by given queries|open
