# flimey core

![Scala CI](https://github.com/flimeyio/flimey-core/workflows/Scala%20CI/badge.svg)

## About

This project is work in progress. Do not use in production! 

**A detailled documentation and outline is coming soon.**

## Devtest Instructions

If you want to try the current compiling version (see CI of the master branch), a rough feature outline is stated under ``doc/``

**Setup**

1.  Clone this repository (--recursive, else no CSS will be available!)
2.  Go to ``conf/db.template.conf`` and complete the MySql setup stated there. If you won't setup an own DB, please contact @KKegel to request a working dev connection.
3.  Somehow run the project with sbt. 
4.  The flimey app will be available in your browser. If no authentication is enabled, you will be the only user and have full admin rights.

**What can be tested?**

Savely working functionality of the current working draft:

* Model -> AssetType creation/modification/deletion
* Assets -> Asset selection/view/creation/modification/deletion

How to configure a AssetType is also described under ``doc/``
