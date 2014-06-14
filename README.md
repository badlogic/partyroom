Party Room
==========

Lightweight Turntable.fm clone that uses Youtube.

Compiling & Running
===================
You’ll need Java/JDK 8 and Maven 3+ to compile and run Party Room. To compile, change into the root directory of the project, then do: 

```
mvm clean package
```

This will generate a the file `target/partyroom.jar`. To start the Party Room server do:

```
java -jar target/partyroom.jar server config/config.yml
```

This will start the party room server which you can then visit at [http://localhost:7778](http://localhost:7778).

All user accounts will be stored in memory and vanish if the server is restarted. The same is true for rooms.

Please get your own Youtube API key and set it in config.yml. You can also secure the server via the config.yml file by configuring it to use SSL. See the [Dropwizard](http://www.dropwizard.io) documentation.
