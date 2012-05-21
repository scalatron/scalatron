package scalatron.botwar

object Version
{
    val VersionString = {
        // if we are not running from a .jar file (e.g. in debugger from .class files), the resource query may return 'null'
        val versionFromResource = Version.getClass.getPackage.getImplementationVersion
        if(versionFromResource == null) "unknown" else versionFromResource
    }
}