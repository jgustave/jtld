#!/bin/bash

java -Djava.library.path=/opt/local/lib:/opt/local -cp ./libs/javacpp.jar:./libs/javacv.jar:./libs/javacv-macosx-x86_64.jar:./target/test.jar com.gong.jtld.Test


