# OpenIce
Con MongoDB custom

## Run with:
```
./gradlew :interop-lab:demo-apps:run
```

## Requirements: 

- MongoDB running anywhere with replication set
```
mongod --dbpath /var/lib/mongodb/data/db --replSet rs0
```
- The database must exist
- Any file .js must exist
