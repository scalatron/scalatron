---
layout: default
title: Map Cheat Sheet
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_90_cheatsheet_00_string.html' />

# Cheat Sheet: `Map`

Note that this cheat sheet covers only the immutable variant of `Map`.

## Constructing a Map

Constructing a `Map` from key/value pairs:

    val map = Map("A" -> 1, "B" -> 2, "C" -> 3)
    
which uses syntactic sugar for tuples and is identical to:
    
    val map = Map(("A",1), ("B",2), ("C",3))

Converting a `List` of `Tuple2` into a `Map`:

    val map = List(("A",1), ("B",2), ("C",3)).toMap

Constructing an empty map:

    val map = Map.empty[String,Int]


## Updating a Map

    val newMap = map.updated(key, value)


## Lookups

If the key must exist (exception thrown if not):

    val value = map.apply(key)
     
which is identical to:
     
    val value = map(key)
     
If the key may or may not exist: 
     
    val valueOpt = map.get(key) // returns Option[]


## Keys and Values

To obtain an iterable collection of the map's values:

    val values = map.values

To obtain an iterable collection of the map's keys:

    val keys = map.keys


## Transformations

To apply a transformation function to every element, obtaining a new, transformed `Map`:
 
    val newMap = map.map(entry => (entry._1, entry._2)) 

To apply a transformation function to all values, obtaining an Iterable collection:
 
    val newMap = map.values.map(value => { (*...*/ }) 


