SCALATRON - Learn Scala With Friends
http://scalatron.wordpress.com - Twitter: @scalatron / #scalatron
This work is licensed under the Creative Commons Attribution 3.0 Unported License.

Organizer Documentation - Scalatron CLI (Command Line Interface)
Version 0.9.9 -- updated 2012-04-26



# About Scalatron

Scalatron is an educational resource for groups of programmers that want to learn more about
the Scala programming language or want to hone their Scala programming skills. It is based on
Scalatron BotWar, a competitive multi-player programming game in which coders pit bot programs
(written in Scala) against each other.

The documentation, tutorial and source code are intended as a community resource and are
in the public domain. Feel free to use, copy, and improve them!



# Overview

Scalatron is a server application running on a central computer. This server hosts both a tournament loop
that continuously simulates short rounds of the Scalatron BotWar game and a web server that exposes a
browser-based user interface and a RESTful API.

The Scalatron CLI (Command Line Interface) exposes a subset of the functionality of the RESTful web API via
a command line tool. For more information about the RESTful web API, see the *Scalatron APIs* documentation.

Both the CLI and RESTful API are a work in progress.


# Syntax

## Invocation

The Scalatron CLI is a Java application and as such must be launched via the Java Runtime Environment's
`java` interpreter. Two invocation options are available

    java -jar ScalatronTest.jar -help

displays help information, while

    java -jar ScalatronTest.jar [-key value] [-key value] [...]

performs an operation. In this case, `[-key value]` corresponds to one of the following key/value pairs:

    -verbose yes|no     print verbose output (default: no)
    -api <string>       the relative path of the server api (default: /api)
    -port <int>         the port the server is listening on (default: 8080)
    -hostname <name>    the hostname of the server (default: localhost)
    -user <name>        the user name to log on as (default: Administrator)
    -password <string>  the password to use for log on (default: empty password)
    -cmd <command>

where <command> may require addition parameters:

    users                       lists all users; does not require logon

    createUser                  create new user; as Administrator only
        -targetUser <name>      the user name for the new user (required)
        -newPassword <string>   the password for the new user (default: empty password)

    deleteUser                  deletes an existing user (along with all content!); Administrator only
        -targetUser <name>      the name of the user to delete (required)

    setUserAttribute            sets a configuration attribute for a user; as user or Administrator
        -targetUser <name>      the name of the user to set attribute on (default: name of '-user' option)
        -key <name>             the key of the attribute to set
        -value <name>           the value of the attribute to set

    getUserAttribute            gets a configuration attribute from a user; as user or Administrator
        -targetUser <name>      the name of the user to get attribute from (default: name of '-user' option)
        -key <name>             the key of the attribute to set

    sources                     gets the source files from a user's server workspace; as user only
        -targetDir <path>       the path of the local directory where the source files should be stored

    updateSources               updates a source files in the user's server workspace; as user only
        -sourceDir <path>       the path of the local directory where the source files can be found

    build                       builds the source files currently in the user's server workspace; as user only

    versions                    lists all versions available in the user workspace; as user only

    createVersion               creates a new version in the user's server workspace; as user only
        -sourceDir <path>       the path of the local directory where the source files can be found
        -label <name>           the label to apply to the versions (default: empty)

    benchmark                   runs standard isolated-bot benchmark on given source files; as user only
        -sourceDir <path>       the path of the local directory where the source files can be found


## Examples:

    java -jar ScalatronCLI.jar -cmd users

    java -jar ScalatronCLI.jar -user Administrator -password a -cmd createUser -targetUser Frankie

    java -jar ScalatronCLI.jar -user Administrator -password a -cmd setUserAttribute -targetUser Frankie -key theKey -value theValue

    java -jar ScalatronCLI.jar -user Administrator -password a -cmd getUserAttribute -targetUser Frankie -key theKey

    java -jar ScalatronCLI.jar -user Frankie -password a -cmd sources -targetDir /tempsrc

    java -jar ScalatronCLI.jar -user Frankie -password a -cmd updateSources -sourceDir /tempsrc

    java -jar ScalatronCLI.jar -user Frankie -password a -cmd build

    java -jar ScalatronCLI.jar -user Frankie -password a -cmd versions

    java -jar ScalatronCLI.jar -user Frankie -password a -cmd createVersion -sourceDir /tempsrc -label "updated"

    java -jar ScalatronCLI.jar -user Frankie -password a -cmd benchmark -sourceDir /tempsrc




