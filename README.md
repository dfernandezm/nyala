# Nyala

Nyala is template project to develop microservices or distributed monoliths with Vert.x, using Kotlin and Java.


Run tests:

```
./gradlew clean test
```

Build fatJar:
```

```

Status endpoint:

http://localhost:9014/_status

Main endpoint:

curl -i -XPOST http://localhost:9014/channels

## Main components 

- Kotlin use (ongoing)
- Multiple Verticles deployment with isolated Dependency Injection graphs using Koin
- Communication with Redis (static endpoint)
- RxJava 2 (ongoing)
- Reactive Process output reading using RxJava
- OAuth2 factory (using Google libraries/apps)