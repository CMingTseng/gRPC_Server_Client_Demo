# gRPC Server Client Demo 

# New Features!
  - This project base form https://github.com/lovoo/grpc-android-demo &  https://github.com/kantoniak/grpc-example
  - gradle version to 4.4 
  - build gradle to 3.1.4
  - protobuf version to 0.8.6
  - Java Server
  - Android Client
 
## Submodules
* `protosfolder` contains proto definitions and generated classes
* `javaserver` serves as a gRPC endpoint , can auto generated proto java
* `app` is an android application (must fixed do not auto generated proto java)

=================

# Init
-----
  - The init dependencies library io.grpc is version 0.7.1.So the protobuf do not match 1.14.0.
  - The Android Client can not auto generated proto java . Need use libs generated by protosfolder  build.gradle
  - Th Java Server need re-write @ 1.14.0 

# About
-----

As there seems to be currently no ready-to-go example for an Android application communicating with
a GRPC server which doesn't involve setting up a boatload of tools we decided to create this simple
demo project which contains a simple GRPC Java server and a matching Android client.

# Changing the protocol
---------------------

The setup contains working code generation so feel free to change the proto file in lib_hello_grpc.
Upon the next build the necessary classes are generated by the gradle grpc plugin. You'll just have
to adapt the server and client code to the newly generated GRPC java classes. The compiler will 
point you to the locations you have to change by means of compilitation errors.

It is advisable to clean the lib_hello_grpc module by calling `./gradlew :lib_hello_grpc:clean` after
having removed or changed the name of a service or a message. This will remove all stale generated
classes and prevent compilation errors within the other generated classes.

# Setup
-----

The following documentation assumes that you've installed Android Studio. Import the project via
gradle.

Running the server
------------------

By default the server runs on port 8080, but you may change that via command line arguments or
directly in the code.

Run the server from Android Studio by right-clicking the `Server.java` in the `lib_hello_grpc`
module. This also creates a suitable run configuration which you can use to run the server again
later. After importing the project here will be errors which are automatically resolved during the first
build via code generation. Make sure, that only one server instance runs at a time (at least if you
attempt to use the same port).

Incoming requests are logged on stdout.

# Running the client
------------------

While importing the project to Android Studio via gradle the IDE automatically generates a run
configuration for the app which you can use to install it on your device or emulator.

In the app first type in the hostname and the port of your server. Then click on the `HELLO SERVER!`
button to execute a request against your GRPC server instance. In case of success the server will
reply with `Hello Android`.

If you did everything correctly it should look something like this:

<img src="https://raw.githubusercontent.com/Lovoo/grpc-android-demo/master/grpc-android-anim.gif" width="540" height="960" />


## Android client

To connect with running local gRPC instance you will have to forward back device localhost port to PC port:
```bash
adb reverse tcp:8800 tcp:8800
```

## API server

Server can be run straight from gradle, or as a part of the distribution.

### Gradle CLI

```bash
cd <root folder>
gradle :api-server:build  # Only builds
gradle :api-server:run    # Builds and starts the server
```

### Running as distribution
```bash
cd <root folder>
gradle :api-server:distZip
cp api-server/build/distributions/api-server.zip <destination>
```
```bash
cd <destination>
unzip api-server.zip
cd api-server
./bin/api-server
```

### Running as distribution from Android Studio

Gradle cannot handle two projects with common dependency at the same time. You can use `api-server` run configuration (or type `gradle :api-server:runServer`) to start server from Android Studio.

### CLI for RPCs

You can use [Polyglot](https://github.com/grpc-ecosystem/polyglot) to test calls. Just send JSON-formatted input to the script:

```bash
echo {} | java -jar polyglot.jar --command=call --endpoint=localhost:8800 \
    --full_method="com.kantoniak.examplegrpc.proto.EntryService/GetAll" \
    --proto_discovery_root="<project-root>/proto/src/main/proto"
```

```bash
java -jar polyglot.jar --command=list_services \
    --proto_discovery_root="<project-root>/proto/src/main/proto"
```

## Dependency on protos

If your module needs to use classes generated from proto, remember to add
```groovy
apply from: project(':proto').file("proto-deps.gradle")
```
```groovy
dependencies {
    implementation project(":proto")
}
```
to `build.gradle` of the module.

# Licence
-------

    Copyright (c) 2015, LOVOO GmbH
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this
      list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.

    * Neither the name of LOVOO GmbH nor the names of its
      contributors may be used to endorse or promote products derived from
      this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
    FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
    DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOEVER
    CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
    OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
    OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.