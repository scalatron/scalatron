/**
 * Copyright (C) 2009-2010 the original author or authors.
 */
package org.fusesource.scalamd.test

import org.specs2._
import org.fusesource.scalamd.Markdown
import org.apache.commons.lang3.StringUtils
import org.apache.commons.io.IOUtils
import org.specs2.matcher.{Expectable, MatchResult, MatchSuccess, Matcher}

object MarkdownSpec extends mutable.Specification {

  val beFine = new Matcher[String] {
    override def apply[S <: String](name: Expectable[S]): MatchResult[S] = {
      val textFile = this.getClass.getResourceAsStream("/" + name.value + ".text")
      val htmlFile = this.getClass.getResourceAsStream("/" + name.value + ".html")
      val text = normalize(Markdown(IOUtils.toString(textFile, "ISO-8859-1")))
      val expectedHtml = normalize(IOUtils.toString(htmlFile, "ISO-8859-1"))
      val diffIndex = StringUtils.indexOfDifference(text, expectedHtml)
      val diff = StringUtils.difference(text, expectedHtml)
      val success = MatchSuccess(
        "\"" + name.value + "\" is fine",
        "\"" + name.value + "\" fails at " + diffIndex + ": " + StringUtils.abbreviate(diff, 32),
        name
      )
      if (diffIndex == -1) {
        success
      } else {
        success.negate
      }
    }
  }

  def normalize(s: String) : String = s.trim.replace(sys.props("line.separator"),"\n")

  "MarkdownProcessor" should {
    "process Images" in {
      "Images" must beFine
    }
    "process TOC" in {
      "TOC" must beFine
    }
    "process Amps and angle encoding" in {
      "Amps and angle encoding" must beFine
    }
    "process Auto links" in {
      "Auto links" must beFine
    }
    "process Backslash escapes" in {
      "Backslash escapes" must beFine
    }
    "process Blockquotes with code blocks" in {
      "Blockquotes with code blocks" must beFine
    }
    "process Hard-wrapped paragraphs with list-like lines" in {
      "Hard-wrapped paragraphs with list-like lines" must beFine
    }
    "process Horizontal rules" in {
      "Horizontal rules" must beFine
    }
    "process Inline HTML (Advanced)" in {
      "Inline HTML (Advanced)" must beFine
    }
    "process Inline HTML (Simple)" in {
      "Inline HTML (Simple)" must beFine
    }
    "process Inline HTML comments" in {
      "Inline HTML comments" must beFine
    }
    "process Links, inline style" in {
      "Links, inline style" must beFine
    }
    "process Links, reference style" in {
      "Links, reference style" must beFine
    }
    "process Literal quotes in titles" in {
      "Literal quotes in titles" must beFine
    }
    "process Nested blockquotes" in {
      "Nested blockquotes" must beFine
    }
    "process Ordered and unordered lists" in {
      "Ordered and unordered lists" must beFine
    }
    "process Strong and em together" in {
      "Strong and em together" must beFine
    }
    "process Tabs" in {
      "Tabs" must beFine
    }
    "process Tidyness" in {
      "Tidyness" must beFine
    }
    "process SmartyPants" in {
      "SmartyPants" must beFine
    }
    "process Markdown inside inline HTML" in {
      "Markdown inside inline HTML" must beFine
    }
    "process Spans inside headers" in {
      "Spans inside headers" must beFine
    }
    "process Macros" in {
      "Macros" must beFine
    }
  }
}
