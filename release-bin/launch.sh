#!/bin/bash

java -Xms128m -Xmx4G -XX:ReservedCodeCacheSize=512m -XX:+UseZGC -XX:SoftRefLRUPolicyMSPerMB=50 -XX:CICompilerCount=2 -XX:-OmitStackTraceInFastThrow -XX:CompileCommand=exclude,com/intellij/openapi/vfs/impl/FilePartNodeRoot,trieDescend -ea -Dsun.io.useCanonCaches=false -Dsun.java2d.metal=true -Dsun.java2d.metal.displaySync=false -Djbr.catch.SIGABRT=true -Djdk.attach.allowAttachSelf=true -Djdk.module.illegalAccess.silent=true -Dkotlinx.coroutines.debug=off -jar EchoInMirror.jar
