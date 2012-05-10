---
layout: default
title: Scalatron Troubleshooting
subtitle: Organizer Documentation
---

# About Scalatron

Scalatron is an educational resource for groups of programmers that want to learn more about
the Scala programming language or want to hone their Scala programming skills. It is based on
Scalatron BotWar, a competitive multi-player programming game in which coders pit bot programs
(written in Scala) against each other.

The documentation, tutorial and source code are intended as a community resource and are
in the public domain. Feel free to use, copy, and improve them!



# Summary

A lot of things can go wrong in life, and running Scalatron is one of them. Whatever happens,
though, you're probably not the only one experiencing it and so there's hope that by collecting
problematic experiences and solutions some of your pain can be alleviated. Here is a list of
things known to go wrong with Scalatron, and sometimes how to fix them:



## Problems Starting The Server

**On Windows, double-clicking `/bin/Scalatron.jar` pops up an error dialog "Could not find the main class. Program will exit."**

Probable Cause:

* Very likely your Java Runtime Environment is too old.
* Scalatron requires Version 1.6 or newer is required.

Steps to Solve:

* Go to [the Java download site](http://www.java.com/download/)
* Download a more recent Java Runtime Environment
* Install it and try again.


**On Windows, running `java -jar /bin/Scalatron.jar` displays a "UnsupportedClassVersionError"**

Probable Cause:

* Very likely your Java Runtime Environment is too old.
* Scalatron requires Version 1.6 or newer is required.

Steps to Solve:

* Go to [the Java download site](http://www.java.com/download/)
* Download a more recent Java Runtime Environment
* Install it and try again.



## Performance Problems

**Server hangs intermittently**

Probable Cause:

* Very likely Java has too little memory and spends too much time running garbage collection.

Steps to Solve:

* give the Scalatron server more memory - as much as you can, in fact. Try starting it with the following options:

    java -Xmx2G -server -jar Scalatron.jar



**Compilations are slow or time out**

Probable Cause:

* The load on the server is too high, causing compilations to take too long.
* This generally means that the ratio of users and bots to available CPU cycles is too high.


Steps to Solve:

* Run the server on a computer with more and/or faster CPU cores. Scalatron will use as many cores as you can provide.
* Ask users to write better-behaved bots - they should not spawn mini-bots with abandon.
* Spread your users across two servers, using one as a testing server (for frequent compilations) and
  one as a tournament server (for competitive runs of debugged bots).
* Provide more memory to the Scalatron server (see tips for "Server hangs intermittently").
* Throttle the simulation using the `-maxfps` command line option, e.g. using `maxfps 20'.
  This will keep the simulator from eating up all available CPU cycles.



## Problems Using The Browser Client

The following browser/platform combinations should work:

* Firefox 12.0 on Windows XP
* Firefox 12.0 on MacOS 10.6.8
* Safari 5.1.5 on MacOS 10.6.8

The following browser/platform combinations are known not to work:

* Internet Explorer 8 on Windows XP
* Internet Explorer 7 on Windows XP
* Firefox 2.0.0.11 on Windows XP
