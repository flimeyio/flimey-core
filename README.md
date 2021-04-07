# flimey core

![Scala CI](https://github.com/flimeyio/flimey-core/workflows/Scala%20CI/badge.svg)

**A self hosted business and productivity board tool with organization management and runtime enabled structural modeling.**

## Release Notes

*This project is work in progress. We still need some time to implement major features bevore going in a production release cycle. As of now, the latest somewhat stable beta versions are puplished occasionally via the ``release/latest`` branch. This will always create a new ``latest`` release in our dockerhub repository [flimeylab](https://hub.docker.com/repository/docker/flimeylab/flimey-core). DO ABSOLUTELY NOT USE THOSE BUILDS IN PRODUCTION, THEY ARE JUST MEANT FOR TESTING.*

**To install the container the first time, execute the following two lines. The http site will be available at port 9080. See our wiki (link below) for first usage instructions.**

```
docker volume create flimeydata
docker run -it --name=flimey --mount source=flimeydata,destination=/var/lib/postgresql -p 9080:9080 -p 9443:9443 flimey-cored
```

> :zap: The container can be created without mounting a volume. By doing so THE CONTAINER CAN NOT BE UPDATED IN THE FUTURE WITHOUT DATA LOSS!

> ⚡ A once created flimeydata volume can be mounted to newer container versions (after an update).
However, do not mount the same volume to two running containers at once! Right now, we do not guarantee data compatability with newer versions.

> 🚑 Right now, the flimey docker image works only as long as postgresql v12 is the newest major version. This needs to be fixed in the
run.sh file.

**To stop the running container, execute**
(To avoid the possibility of incomplete workflows, it is recommended to cut the network connection
some seconds before, if multiple users are active during the shutdown. However, the db will always stay valid)

``docker stop flimey``


**To start the container which was already installed, execute**

``docker start flimey``


## About

**This project is work in progress. Do not use in production!**

A detailled documentation can be found [here](https://github.com/flimeyio/flimey-core/wiki).

The development setup/getting started guide can also be found [here](https://github.com/flimeyio/flimey-core/wiki/System-Setup).

Our project kanban with open issues and development progress can be found [here](https://github.com/flimeyio/flimey-core/projects/1).

## Devlopment Stack

* Scala as our main programming language
* Webserver/request handling with Playframework
* Dependency injection with Guice
* HTML templating with Play-Templates (Twirl)
* Visualization with JQuery + D3.js
* Database management with Slick 3 on top of PostgreSQL
* Testing with Scala-Test (scalatestplayplus)
* CI with Github-Actions

## Contribute

Interested in flimey? - Contact us at ``dev@flimey.io`` and we will add you to our team.

Contributing to flimey implies:

* You are always the owner of your code, obligatory license notes in every file
* Write code as much as you want. Several weeks with no commits? No problem at all
* Choose what you can do best, UI, testing, CI/CD, core development - everyone can help
* No knowledge in Scala? Start by writing simple tests or style the UI - just learn Scala by doing
* If flimey somehow becomes a commerical tool in the future or results in a startup, your contributions will be taken into account

### What We Need Right Now

* Support for UI development focusing on responsive design, usability and accessability
* Support for documentation management, to review scaladoc comments, maintain the wiki and create and maintain tutorials for developers and end-users
* Support for testing with focus on unit and integration tests as well als performance and load testing
* Support for the core development with feature implementation, bug fixing and maintaining the exisiting code base
