/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatronRemote.impl

/** Wrapper for JSon object parsed by util.parsing.json.JSON.parseFull() */
case class JSonOpt(opt: Option[Any]) {
    /** { "a" : "1", "b" : "2" } => Map("a" -> "1", "b" -> "2") */
    def asMap: JSonMap = opt match {
        case None => throw new IllegalStateException("invalid data (no JSON found)")
        case Some(map: Map[String, Any]) => JSonMap(map, this)
        case _ => throw new IllegalStateException("invalid data (expected Map): " + opt)
    }
}

/** Wrapper for Map in JSon. Retains 'outer' for error output. */
case class JSonMap(map: Map[String, Any], outer: JSonOpt) {
    /** Retrieves a String value for the given key. Throws if not present or not a String. */
    def asString(key: String): String = map.get(key) match {
        case None => throw new IllegalStateException("invalid data (key not found: '" + key + "'): " + outer)
        case Some(s: String) => s
        case _ => throw new IllegalStateException("invalid data (value is not a String for key: '" + key + "'): " + outer)
    }

    /** Retrieves a Boolean value for the given key. Throws if not present or not a Boolean. */
    def asBoolean(key: String): Boolean = map.get(key) match {
        case None => throw new IllegalStateException("invalid data (key not found: '" + key + "'): " + outer)
        case Some(b: Boolean) => b
        case _ => throw new IllegalStateException("invalid data (value is not a Boolean for key: '" + key + "'): " + outer)
    }

    /** Retrieves an Int value for the given key. Throws if not present or not an Int. */
    def asInt(key: String): Int = map.get(key) match {
        case None => throw new IllegalStateException("invalid data (key not found: '" + key + "'): " + outer)
        case Some(b: Int) => b
        case Some(b: Double) => b.toInt
        case _ => throw new IllegalStateException("invalid data (value is not an Int for key: '" + key + "'): " + outer)
    }

    /** Retrieves a List value (JSON Array) for the given key. Throws if not present or not a List. */
    def asList[A](key: String): List[A] = map.get(key) match {
        case None => throw new IllegalStateException("invalid data (key not found: '" + key + "'): " + outer)
        case Some(list: List[A]) => list
        case _ => throw new IllegalStateException("invalid data (value is not a List for key: '" + key + "'): " + outer)
    }

    /** Retrieves a Map value (JSON Map) for the given key that is expected to contain a key/value Map.
      * Throws if not present or not a Map. Example:
      * json = { "resources" : { "name" : "A", "url" : "a"} }
      * jsonOpt.asMap.asMap("resources") => Map("name" -> "A", "url" -> "a")
      */
    def asStringMap(key: String): Map[String, String] = map.get(key) match {
        case None => throw new IllegalStateException("invalid data (key not found: '" + key + "'): " + outer)
        case Some(innerMap: Map[String,Any]) => innerMap.map(entry => { (entry._1, entry._2.asInstanceOf[String]) }).toMap
        case _ => throw new IllegalStateException("invalid data (value is not a Map for key: '" + key + "'): " + outer)
    }


    /** Retrieves a List value (JSON Array) for the given key that is expected to contain a
      * key/value Map. Throws if not present or not a List. Example:
      * json = { "resources" : [ { "name" : "A", "url" : "a"}, { "name" : "B", "url" : "b"}, ... } ] }
      * jsonOpt.asMap.asListOfKVStrings("resources","name","url") => Map("A" -> "a", "B" -> "b")
      */
    def asKVStrings(listKey: String, nameKey: String, valueKey: String): Map[String, String] =
        asList[Map[String, String]](listKey)
        .map(item => item match {
            case map: Map[String, Any] =>
                val name = map(nameKey).asInstanceOf[String] // CBB: use match / case
                val url = map(valueKey).asInstanceOf[String] // CBB: use match / case
                (name, url)
            case _ => throw new IllegalStateException("invalid data (expected Map): " + outer)
        }).toMap

    /** Retrieves a List value (JSON Array) for the given key that is expected to contain a
      * key/value Map. Throws if not present or not a List. Example:
      * json = { "resources" : [ { "name" : "A", "url" : "a1", "url2" : "a2"}, { "name" : "B", "url" : "b1", "url2" : "b2"}, ... } ] }
      * jsonOpt.asMap.asListOfKVStrings("resources","name","url") => Map("A" -> ("a1","a2"), "B" -> ("b1","b2"))
      */
    def asKVVStrings(listKey: String, nameKey: String, value1Key: String, value2Key: String): Map[String, (String,String)] =
        asList[Map[String, String]](listKey)
        .map(item => item match {
            case map: Map[String, Any] =>
                val name = map(nameKey).asInstanceOf[String]        // CBB: use match / case
                val value1 = map(value1Key).asInstanceOf[String]    // CBB: use match / case
                val value2 = map(value2Key).asInstanceOf[String]    // CBB: use match / case
                (name, (value1,value2))
            case _ => throw new IllegalStateException("invalid data (expected Map): " + outer)
        }).toMap
}