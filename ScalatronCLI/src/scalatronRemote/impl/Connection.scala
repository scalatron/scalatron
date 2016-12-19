/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatronRemote.impl

import org.apache.http.HttpStatus
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.{HttpDelete, HttpGet, HttpPost, HttpPut}
import org.apache.http.client.utils.{URIBuilder, URIUtils}
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.util.EntityUtils

import scala.util.parsing.json.JSON
import scalatronRemote.impl.Connection.{
  HttpFailureCodeException,
  JsonMimeType,
  synchronizedJsonParser
}

object Connection {
  val JsonMimeType = "application/json"
  val HttpCode_Created = "application/json"

  class ConnectionException(msg: String) extends RuntimeException(msg: String)

  class HttpFailureCodeException(val httpCode: Int,
                                 val reason: String,
                                 msg: String)
      extends RuntimeException(msg)

  /** Scala Parsers are not thread safe. See
    * http://scala-programming-language.1934581.n4.nabble.com/Combinator-Parsers-trait-is-not-thread-safe-td3079447.html
    */
  var lock: AnyRef = new Object()
  def synchronizedJsonParser(s: String): JSonOpt = {
    lock.synchronized { JSonOpt(JSON.parseFull(s)) }
  }

}

case class Connection(httpClient: HttpClient,
                      hostname: String,
                      port: Int,
                      verbose: Boolean = false) {

  /** Sends an HTTP GET to the server, retrieves a JSON string from the result and parses it.
    * Returns a wrapper object you can use to disassemble the JSON result. Throws on error.
    * @throws HttpFailureCodeException if an HTTP code other than SC_OK is received
    */
  def GET_json(resource: String): JSonOpt = {
    val uri = new URIBuilder()
      .setScheme("http")
      .setHost(hostname)
      .setPort(port)
      .setPath(resource)
      .setCustomQuery("")
      .build()

    val request = new HttpGet(uri)

    val response = httpClient.execute(request)
    val statusCode = response.getStatusLine.getStatusCode
    if (statusCode != HttpStatus.SC_OK) {
      throw new HttpFailureCodeException(
        statusCode,
        response.getStatusLine.getReasonPhrase,
        "GET on '" + uri.toString + "' failed; HTTP code: " + statusCode + ", reason: " + response.getStatusLine.getReasonPhrase)
    }

    val responseEntity = response.getEntity
    val mimeType = ContentType.getOrDefault(responseEntity).getMimeType
    val jsonMimeType = "application/json"
    if (mimeType != jsonMimeType) {
      throw new IllegalStateException(
        "mime type error; expected: " + jsonMimeType + ", received: " + mimeType)
    }

    val responseString = EntityUtils.toString(responseEntity)
    // if (verbose) println(responseString)

    synchronizedJsonParser(responseString)
  }

  /** Sends an HTTP POST to the server with the given JSON payload, retrieves a JSON string from
    * the result and parses it. Returns a wrapper object you can use to disassemble the JSON result.
    * Throws on error.
    */
  def POST_json_json(resource: String, jsonString: String): JSonOpt = {
    val uri = new URIBuilder()
      .setScheme("http")
      .setHost(hostname)
      .setPort(port)
      .setPath(resource)
      .setCustomQuery("")
      .build()

    val request = new HttpPost(uri)
    val requestEntity = new StringEntity(jsonString, JsonMimeType)
    request.setEntity(requestEntity)

    val response = httpClient.execute(request)
    val statusCode = response.getStatusLine.getStatusCode
    if (statusCode != HttpStatus.SC_CREATED && statusCode != HttpStatus.SC_OK) {
      throw new HttpFailureCodeException(
        statusCode,
        response.getStatusLine.getReasonPhrase,
        "POST on '" + uri.toString + "' failed with HTTP code " + statusCode + ", reason: " + response.getStatusLine.getReasonPhrase)
    }

    val entity = response.getEntity
    val mimeType = ContentType.getOrDefault(entity).getMimeType
    if (mimeType != JsonMimeType) {
      throw new IllegalStateException(
        "mime type error; expected: " + JsonMimeType + ", received: " + mimeType)
    }

    val responseString = EntityUtils.toString(entity)
    // if (verbose) println(responseString)

    synchronizedJsonParser(responseString)
  }

  /** Sends an HTTP POST to the server with the given JSON payload and retrieves a URL location
    * from the result. Throws on error.
    */
  def POST_json_url(resource: String, jsonString: String): String = {
    val uri = new URIBuilder()
      .setScheme("http")
      .setHost(hostname)
      .setPort(port)
      .setPath(resource)
      .setCustomQuery("")
      .build()

    val request = new HttpPost(uri)
    val requestEntity = new StringEntity(jsonString, JsonMimeType)
    request.setEntity(requestEntity)

    val response = httpClient.execute(request)
    val statusCode = response.getStatusLine.getStatusCode
    if (statusCode != HttpStatus.SC_CREATED && statusCode != HttpStatus.SC_OK) {
      throw new HttpFailureCodeException(
        statusCode,
        response.getStatusLine.getReasonPhrase,
        "POST on '" + uri.toString + "' failed with HTTP code " + statusCode + ", reason: " + response.getStatusLine.getReasonPhrase)
    }

    val entity = response.getEntity
    val mimeType = ContentType.getOrDefault(entity).getMimeType
    if (mimeType != JsonMimeType) {
      throw new IllegalStateException(
        "mime type error; expected: " + JsonMimeType + ", received: " + mimeType)
    }

    val responseString = EntityUtils.toString(entity)
    // if (verbose) println(responseString)

    responseString
  }

  /** Sends an HTTP PUT to the server with no input data but JSON output data. Throws on error.
    */
  def PUT_nothing_json(resource: String): JSonOpt = {
    val uri = new URIBuilder()
      .setScheme("http")
      .setHost(hostname)
      .setPort(port)
      .setPath(resource)
      .setCustomQuery("")
      .build()

    val request = new HttpPut(uri)

    val response = httpClient.execute(request)
    val statusCode = response.getStatusLine.getStatusCode
    if (statusCode != HttpStatus.SC_OK) {
      throw new HttpFailureCodeException(
        statusCode,
        response.getStatusLine.getReasonPhrase,
        "PUT on '" + uri.toString + "' failed with HTTP code " + statusCode + ", reason: " + response.getStatusLine.getReasonPhrase)
    }

    val entity = response.getEntity
    val mimeType = ContentType.getOrDefault(entity).getMimeType
    if (mimeType != JsonMimeType) {
      throw new IllegalStateException(
        "mime type error; expected: " + JsonMimeType + ", received: " + mimeType)
    }

    val responseString = EntityUtils.toString(entity)
    // if (verbose) println(responseString)

    synchronizedJsonParser(responseString)
  }

  /** Sends an HTTP PUT to the server with the given JSON payload and no result value. Throws on error.
    */
  def PUT_json_nothing(resource: String, jsonString: String): Unit = {
    val uri = new URIBuilder()
      .setScheme("http")
      .setHost(hostname)
      .setPort(port)
      .setPath(resource)
      .setCustomQuery("")
      .build()

    val request = new HttpPut(uri)
    val requestEntity = new StringEntity(jsonString, JsonMimeType)
    request.setEntity(requestEntity)

    val response = httpClient.execute(request)
    val statusCode = response.getStatusLine.getStatusCode
    if (statusCode != HttpStatus.SC_NO_CONTENT && statusCode != HttpStatus.SC_OK) {
      throw new HttpFailureCodeException(
        statusCode,
        response.getStatusLine.getReasonPhrase,
        "PUT on '" + uri.toString + "' failed with HTTP code " + statusCode + ", reason: " + response.getStatusLine.getReasonPhrase)
    }
  }

  /** Sends an HTTP PUT to the server with the given JSON payload and no result value. Throws on error.
    */
  def PUT_nothing_nothing(resource: String): Unit = {
    val uri = new URIBuilder()
      .setScheme("http")
      .setHost(hostname)
      .setPort(port)
      .setPath(resource)
      .setCustomQuery("")
      .build()

    val request = new HttpPut(uri)

    val response = httpClient.execute(request)
    val statusCode = response.getStatusLine.getStatusCode
    if (statusCode != HttpStatus.SC_NO_CONTENT) {
      throw new HttpFailureCodeException(
        statusCode,
        response.getStatusLine.getReasonPhrase,
        "PUT on '" + uri.toString + "' failed with HTTP code " + statusCode + ", reason: " + response.getStatusLine.getReasonPhrase)
    }
  }

  /** Sends an HTTP DELETE to the server. Throws on error.
    * */
  def DELETE(resource: String): Unit = {
    val uri = new URIBuilder()
      .setScheme("http")
      .setHost(hostname)
      .setPort(port)
      .setPath(resource)
      .setCustomQuery("")
      .build()

    val request = new HttpDelete(uri)

    val response = httpClient.execute(request)
    val statusCode = response.getStatusLine.getStatusCode
    if (statusCode != HttpStatus.SC_NO_CONTENT) {
      throw new HttpFailureCodeException(
        statusCode,
        response.getStatusLine.getReasonPhrase,
        "DELETE on '" + resource + "' failed with HTTP code " + statusCode + ", reason: " + response.getStatusLine.getReasonPhrase)
    }
  }

}
