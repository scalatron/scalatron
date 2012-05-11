---
layout: default
title: Introduction To Scala
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_00_00_scala_resources.html' data-next='/tutorial/tutorial_00_10_intro_to_scalatron.html' />

# Introduction To Scala

## What is Scala?

Scala is a programming language invented by Martin Odersky in 2000. It is statically typed
(like Java) and combines functional (FP) and object oriented (OO) features. Just like Java,
Scala compiles into Java byte code and can run on any Java Virtual Machine (JVM). Scala
interoperates well with Java and can serve as a by-file or by-module replacement for Java.
In many, many respects it fixes the things that are awkward or broken in Java.


## Comparison to Java

The syntax of Scala overall is quite similar to Java, but in general it omits elements that
are not strictly necessary and place a burden on the programmer (such as semicolons or
type declarations that are inferable for the compiler) and generally required less
boilerplate code.

The result is that Scala code is often only a third or a quarter of the size of equivalent
Java code.


## Scala Performance

The performance of Scala depends on how you code. Code written in a functional style can be
slower to execute in certain circumstances; code written in an imperative, procedural will
be as fast as in Java.

However, functional code is

* much more concise
* much faster to write
* much easier to compose
* much easier to parallelize

So generally, writing functional code is preferable for the vast majority of code.
Nevertheless, Scala lets you fall back to imperative code for performance critical
sections.

