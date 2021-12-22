#!/bin/bash

java -cp ".:./json.jar" OttRunner $1 $2
exec bash
