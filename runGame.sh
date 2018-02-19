#!/bin/bash

javac ActimelBot.java
javac RandomBot.java
./halite -d "50 50" -s 2017 "java ActimelBot" "java RandomBot"
