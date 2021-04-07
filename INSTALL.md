Note: The container can be created without mounting a volume. By doing so THE CONTAINER CAN NOT BE UPDATED IN THE FUTURE WITHOUT DATA LOSS!

Note: A once created flimeydata volume can be mounted to newer container versions (after an update).
However, do not mount the same volume to two running containers at once!

TODO: Right now, the flimey docker image works only as long as postgresql v12 is the newest major version. This needs to be fixed in the
run.sh file.

**To install the container the first time, execute the following two lines**

```
docker volume create flimeydata
docker run -it --name=flimey --mount source=flimeydata,destination=/flimeydata -p 9080:9080 -p 9443:9443 flimey-core
```

**To stop the running container, execute**
(To avoid the possibility of incomplete workflows, it is recommended to cut the network connection
some seconds before, if multiple users are active during the shutdown. However, the db will always stay valid)

``docker stop flimey``


**To start the container which was already installed, execute**

``docker start flimey``
