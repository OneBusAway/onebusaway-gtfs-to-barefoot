# onebusaway-gtfs-to-barefoot
This is an application that takes a GTFS file and produces a https://github.com/bmwcarit/barefoot map file. 

It does this by making a Barefoot Road for each of the shapes in shapes.txt of the GTFS.

This map file can then be used to run a barefoot server that can be used for map matching raw AVL. 


The intention is to integrate a map matching server into www.transitclock.org as an advanced option and use a barefoot server as the first implementation.


# Build
```
mvn install
```

# Run
```
java -jar target/onebusaway-barefoot-gtfs-mapcreator.jar
```

This will give the command line options.



