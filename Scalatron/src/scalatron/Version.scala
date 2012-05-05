/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron

object Version
{
    val VersionString = {
        // if we are not running from a .jar file (e.g. in debugger from .class files), the resource query may return 'null'
        val versionFromResource = Version.getClass.getPackage.getImplementationVersion
        if(versionFromResource == null) "unknown" else versionFromResource
    }
}