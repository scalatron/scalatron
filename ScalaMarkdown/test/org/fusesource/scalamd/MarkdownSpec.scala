package org.fusesource.scalamd

import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class MarkdownSpec extends FlatSpec with Matchers {

  val beFine = Matcher((name: String) => {
    val inputText = Source.fromURI(getClass.getResource(s"/$name.text").toURI).mkString
    val html = Source.fromURI(getClass.getResource(s"/$name.html").toURI).mkString.trim
    val text = Markdown(inputText).trim
    lazy val failure = s"html was not equal to markdown - expected: '$html'\n actual: '$text'\n"
    MatchResult(text == html, failure, failure)
  })

  "MarkdownProcessor" should "process Images" in {
    "Images" should beFine
  }
  it should "process TOC" in {
    "TOC" should beFine
  }
  it should "process Amps and angle encoding" in {
    "Amps and angle encoding" should beFine
  }
  it should "process Auto links" in {
    "Auto links" should beFine
  }
  it should "process Backslash escapes" in {
    "Backslash escapes" should beFine
  }
  it should "process Blockquotes with code blocks" in {
    "Blockquotes with code blocks" should beFine
  }
  it should "process Hard-wrapped paragraphs with list-like lines" in {
    "Hard-wrapped paragraphs with list-like lines" should beFine
  }
  it should "process Horizontal rules" in {
    "Horizontal rules" should beFine
  }
  it should "process Inline HTML (Advanced)" in {
    "Inline HTML (Advanced)" should beFine
  }
  it should "process Inline HTML (Simple)" in {
    "Inline HTML (Simple)" should beFine
  }
  it should "process Inline HTML comments" in {
    "Inline HTML comments" should beFine
  }
  it should "process Links, inline style" in {
    "Links, inline style" should beFine
  }
  it should "process Links, reference style" in {
    "Links, reference style" should beFine
  }
  it should "process Literal quotes in titles" in {
    "Literal quotes in titles" should beFine
  }
  it should "process Nested blockquotes" in {
    "Nested blockquotes" should beFine
  }
  it should "process Ordered and unordered lists" in {
    "Ordered and unordered lists" should beFine
  }
  it should "process Strong and em together" in {
    "Strong and em together" should beFine
  }
  it should "process Tabs" in {
    "Tabs" should beFine
  }
  it should "process Tidyness" in {
    "Tidyness" should beFine
  }
  it should "process SmartyPants" in {
    "SmartyPants" should beFine
  }
  it should "process Markdown inside inline HTML" in {
    "Markdown inside inline HTML" should beFine
  }
  it should "process Spans inside headers" in {
    "Spans inside headers" should beFine
  }
  it should "process Macros" in {
    "Macros" should beFine
  }
}
