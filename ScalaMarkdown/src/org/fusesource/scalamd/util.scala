/**
 * Copyright (C) 2009-2010 the original author or authors.
 */
package org.fusesource.scalamd

import java.lang.StringBuilder
import java.util.regex.{Pattern, Matcher}
import Markdown._

// # Character protector

/**
 * We use character protector mechanism to ensure that certain elements of markup,
 * such as inline HTML blocks, remain undamaged when processing.
 */
class Protector {
  protected var protectHash: Map[String, CharSequence] = Map()
  protected var unprotectHash: Map[CharSequence, String] = Map()

  /**
   * Generates a random hash key.
   */
  def randomKey = (0 to keySize).foldLeft("")((s, i) =>
    s + chars.charAt(rnd.nextInt(keySize)))

  /**
   * Adds the specified token to hash and returns the protection key.
   */
  def addToken(t: CharSequence): String = unprotectHash.get(t) match {
    case Some(key) => key
    case _ =>
      val key = randomKey
      protectHash += key -> t
      unprotectHash += t -> key
      key
  }

  /**
   * Attempts to retrieve an encoded sequence by specified `key`.
   */
  def decode(key: String): Option[CharSequence] = protectHash.get(key)

  /**
   * Hash keys that are currently in use.
   */
  def keys = protectHash.keys

  override def toString = protectHash.toString
}

// # Enhanced String Builder

/**
 * A simple wrapper over `StringBuilder` with utility methods.
 */
class StringEx(protected var text: StringBuilder) {

  def this(source: CharSequence) = this(new StringBuilder(source))

  /**
   * Creates a `Matcher` using specified `Pattern` and applies replacements literally
   * (without interpreting $1, $2, etc.) by calling specified `replacementFunction`
   * on each match.
   */
  def replaceAll(pattern: Pattern, replacementFunction: Matcher => CharSequence): this.type = {
    var lastIndex = 0;
    val m = pattern.matcher(text);
    val sb = new StringBuilder();
    while (m.find()) {
      sb.append(text.subSequence(lastIndex, m.start))
      sb.append(replacementFunction(m))
      lastIndex = m.end
    }
    sb.append(text.subSequence(lastIndex, text.length))
    text = sb;
    return this
  }

  /**
   * Replaces all occurences of specified `string` with specified `replacement`
   * without using regular expressions.
   */
  def replaceAll(string: String, replacement: CharSequence): this.type = {
    val result = new StringBuilder
    var startIdx = 0
    var oldIdx = 0
    oldIdx = text.indexOf(string, startIdx)
    while (oldIdx >= 0) {
      result.append(text.substring(startIdx, oldIdx))
      result.append(replacement)
      startIdx = oldIdx + string.length
      oldIdx = text.indexOf(string, startIdx)
    }
    result.append(text.substring(startIdx))
    text = result
    return this
  }

  def replaceAllFunc(pattern: Pattern, replacementFunction: Matcher => CharSequence, literally: Boolean = true): this.type =
    if (literally)
      replaceAll(pattern, replacementFunction)
    else {
      text = new StringBuilder(pattern.matcher(text).replaceAll(replacementFunction(null).toString))
      return this
    }

  def replaceAll(pattern: Pattern, replacement: CharSequence, literally: Boolean = true): this.type =
    if (literally) replaceAll(pattern, m => replacement)
    else {
      text = new StringBuilder(pattern.matcher(text).replaceAll(replacement.toString))
      return this
    }

  /**
   * Appends the specified character sequence.
   */
  def append(s: CharSequence): this.type = {
    text.append(s)
    return this
  }

  /**
   * Prepends the specified character sequence.
   */
  def prepend(s: CharSequence): this.type = {
    text = new StringBuilder(s).append(text)
    return this
  }

  /**
   * Removes at most 4 leading spaces at the beginning of every line.
   */
  def outdent(): this.type = replaceAll(rOutdent, "")

  /**
   * Provides the length of the underlying buffer.
   */
  def length = text.length

  /**
   * Extracts the sub-sequence from underlying buffer.
   */
  def subSequence(start: Int, end: Int) =
    text.subSequence(start, end)

  /**
   * Creates a `Matcher` from specified `pattern`.
   */
  def matcher(pattern: Pattern) = pattern.matcher(text)

  /**
   * Emits the content of underlying buffer.
   */
  override def toString = text.toString
}
