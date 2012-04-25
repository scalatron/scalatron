/**This material is intended as a community resource and is licensed under the
 * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
 */

package scalatronCLI

import cmdline.CommandLineProcessor


/**A simple test application that exercises the Scalatron RESTful API over HTTP.
 *
 * To build this package, you need to download and include the Apache HTTP Components into your
 * project, see http://hc.apache.org/
 *
 */
object Main {
    def main(args: Array[String]) {
        CommandLineProcessor(args)
    }
}