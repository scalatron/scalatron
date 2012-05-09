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



## Problems Using The Browser Client

The following browser/platform combinations should work:

* Firefox 12.0 on Windows XP
* Firefox 12.0 on MacOS 10.6.8
* Safari 5.1.5 on MacOS 10.6.8

The following browser/platform combinations are known not to work:

* Internet Explorer 8 on Windows XP
* Internet Explorer 7 on Windows XP
* Firefox 2.0.0.11 on Windows XP
