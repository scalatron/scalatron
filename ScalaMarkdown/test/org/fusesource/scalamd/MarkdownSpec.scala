package org.fusesource.scalamd

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{Matchers, PropSpec}

import scala.io.Source

class MarkdownSpec extends PropSpec with Matchers with TableDrivenPropertyChecks {

  val examples = Table(
    "template",
    "Images",
    "TOC",
    "Amps and angle encoding",
    "Auto links",
    "Backslash escapes",
    "Blockquotes with code blocks",
    "Hard-wrapped paragraphs with list-like lines",
    "Horizontal rules",
    "Inline HTML (Advanced)",
    "Inline HTML (Simple)",
    "Inline HTML comments",
    "Links, inline style",
    "Links, reference style",
    "Literal quotes in titles",
    "Nested blockquotes",
    "Ordered and unordered lists",
    "Strong and em together",
    "Tabs",
    "Tidyness",
    "SmartyPants",
    "Markdown inside inline HTML",
    "Spans inside headers",
    "Macros"
  )

  property("MarkdownProcessor should process files correctly") {
    forAll(examples) { name =>
      val inputText = Source.fromURI(getClass.getResource(s"/$name.text").toURI).mkString
      val expectedHtml = Source.fromURI(getClass.getResource(s"/$name.html").toURI).mkString.trim
      Markdown(inputText).trim shouldBe expectedHtml
    }
  }
}
