---
layout: default
title: String Cheat Sheet
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_80_example_03.html' data-next='/tutorial/tutorial_90_cheatsheet_01_map.html' />

# Cheat Sheet: `String`

## Constructing a String

    val a = "A"
    val ab = "A" + "B"
    val a1b = "A" + 1 + "B"

## Taking a String apart

To break a string into segments at all occurrences of a character:

    val segments = string.split('=')

To retrieve the character at a specific index within the string:

    val c = string.charAt(5)
    val c = string(5)

## Comparing Strings

    string.compareTo("other")
    string.compareToIgnoreCase("other")

## Miscellaneous

    string.isEmpty
    string.head     // first character
    string.last     // last character
    string.filter(c => c == 'A')
    string.foreach(c => println(c))
    string.map(c => c+1)
    "abc".zipWithIndex // => ('a',0), ('b',1), ('c',2)
