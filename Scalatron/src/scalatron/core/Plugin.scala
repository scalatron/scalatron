package scalatron.core

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import java.net.URLClassLoader
import java.io.File


/** A plugin contains the name of the player that created it (derived from the plugin
  * directory name) as well the control function factory that was loaded from it.
  */
trait Plugin
{
    /** @return the name of the plug-in, which is also the name of the associated player, e.g. "Daniel". */
    def name: String

    /** @return the control function factory implemented by the plug-in. */
    def controlFunctionFactory: () => (String => String)

    override def toString = name
}


object Plugin
{
    /** This constant specifies the file name which all components of the Scalatron system expect to use for bot plug-ins. */
    val JarFilename = "ScalatronBot.jar"

    /** This constant specifies the file name to use when backing up an existing Scalatron plug-in. */
    val BackupJarFilename = "ScalatronBot.backup.jar"

    /** This constant specifies the (unqualified) class name expected for control function factories in plug-ins. */
    val ControlFunctionFactoryClassName = "ControlFunctionFactory"

    /** Information about where an external plug-in was loaded from, or where a plug-in
      * was attempted to be loaded from when a load failure occurred.
      */
    trait DiskInfo
    {
        def dirPath: String
        // e.g. "/Users/Scalatron/Scalatron/bots/Daniel" (no terminal slash)
        def filePath: String
        // e.g. "/Users/Scalatron/Scalatron/bots/Daniel/ScalatronBot.jar"
        def fileTime: Long
        // milliseconds since the epoch, as returned by File.lastModified()
        override def toString = filePath
    }

    /** Plugin.FromJarFile: a plugin implementation that provides a control function that was loaded from a
      * .jar file on disk.
      * @param dirPath e.g. "/Users/Scalatron/Scalatron/bots/Daniel" (no terminal slash)
      * @param filePath e.g. "/Users/Scalatron/Scalatron/bots/Daniel/ScalatronBot.jar"
      * @param fileTime milliseconds since the epoch, as returned by File.lastModified()
      * @param name e.g. "Daniel"
      * @param controlFunctionFactory the control function factory
      */
    case class FromJarFile(
        dirPath: String,
        filePath: String,
        fileTime: Long,
        name: String,
        controlFunctionFactory: () => (String => String))
        extends Plugin with DiskInfo
    {
        override def toString = filePath
    }


    /** Plugin.LoadFailure: Container used to report a plug-in load failure.
      * @param dirPath e.g. "/Users/Scalatron/Scalatron/bots/Daniel" (no terminal slash)
      * @param filePath e.g. "/Users/Scalatron/Scalatron/bots/Daniel/ScalatronBot.jar"
      * @param fileTime milliseconds since the epoch, as returned by File.lastModified()
      * @param exception the exception that caused/explains the load failure
      */
    case class LoadFailure(dirPath: String, filePath: String, fileTime: Long, exception: Throwable) extends DiskInfo
    {
        override def toString = exception + ": " + filePath
    }


    /** Attempts to load a ControlFunctionFactory instance from a given plug-in file using a number of package
      * path candidates and returns either the resulting factory function (left, success) or the most recent
      * exception (right, failure).
      * @param pluginFile a File object representing the plug-in (.jar) file
      * @param userName the user name, will be tried as a package name
      * @param gameSpecificPackagePath e.g. "scalatron.botwar.botPlugin"
      * @param factoryClassName e.g. "ControlFunctionFactory"
      */
    def loadFrom(
        pluginFile: File,
        userName: String,
        gameSpecificPackagePath: String,
        factoryClassName: String,
        verbose: Boolean): Either[() => (String => String), Throwable] =
    {
        /** For regular tournament operation it would be OK to use any fixed package name on the factory class
          * (including no package statement at all). The compile service, however, recycles its compiler state
          * to accelerate compilation, which results in namespace collisions if multiple users use the same
          * fully qualified class names for their classes. So in order to make the compiler instance recycling
          * feasible, we need a bit of a hack: each user must (be able to) use a unique package name for her classes.
          * So we try that as the first option: a package name consisting of the user name (which during plug-in
          * loading for the tournament loop will be the name of the directory containing the plug-in .jar file).
          * Case is significant.
          * We try the following package names in the following order:
          * 1) {gamePackage}.{username}.{className} -- game- and user-specific package name, e.g. "scalatron.botwar.botPlugin.Frank.ControlFunctionFactory"
          * 2) {username}.{className}               -- user-specific package name, e.g. "Frank.ControlFunctionFactory"
          * 3) {gamePackage}.{className}            -- game-specific package name, e.g. "scalatron.botwar.botPlugin.ControlFunctionFactory"
          * 4) {className}                          -- no package name, e.g. "ControlFunctionFactory"
          */
        val classNamesWithPackagePathsToTry = Iterable(
            gameSpecificPackagePath + "." + userName + "." + factoryClassName,
            userName + "." + factoryClassName,
            gameSpecificPackagePath + "." + factoryClassName,
            factoryClassName
        )

        loadFactoryClassFromJar(
            pluginFile,
            classNamesWithPackagePathsToTry,
            verbose
        )
    }


    /** Creates a URLClassLoader for a given file and attempts to load a class via the loader using a sequence of
      * class names, one after the other. All exceptions that are detected are mapped to Right(Throwable).
      * @param jarFile a File object representing the plug-in (.jar) file
      * @param classNamesWithPackagePathsToTry a collection of class names (including package path) to try to load
      * @param verbose if true, prints what it is doing to the console
      * @return the class that was loaded, or the last encountered exception
      */
    private def loadFactoryClassFromJar(
        jarFile: File,
        classNamesWithPackagePathsToTry: Iterable[String],
        verbose: Boolean): Either[() => (String => String), Throwable] =
    {
        /** TODO: think about sandboxing plug-ins to prevent them from accessing sensitive stuff. See
          * http://stackoverflow.com/questions/3947558/java-security-sandboxing-plugins-loaded-via-urlclassloader
          */
        val classLoader =
            try {
                new URLClassLoader(Array(jarFile.toURI.toURL), this.getClass.getClassLoader)
            } catch {
                case t: Throwable => return Right(t)
            }

        val pluginFilePath = jarFile.getAbsolutePath
        val methodName = "create"

        // can't return from a .foreach to shortcircuit, throws scala.runtime.NonLocalReturnControl; so we iterate manually
        var lastError: Option[Throwable] = None
        val iterator = classNamesWithPackagePathsToTry.iterator
        while(iterator.hasNext) {
            val classNamesWithPackagePath = iterator.next()
            try {
                if(verbose) println("info: will try to load class '%s' from plug-in '%s'...".format(classNamesWithPackagePath, pluginFilePath))
                val factoryClass = Class.forName(classNamesWithPackagePath, true, classLoader)

                if(verbose) println("info: class '%s' loaded, will try to find method '%s'...".format(classNamesWithPackagePath, methodName))
                val factoryMethod = factoryClass.getMethod(methodName)

                if(verbose) println("info: method '%s' found, will try to instantiate factory...".format(methodName))
                val factory = factoryClass.newInstance()
                val factoryFunction: () => (String => String) = () => factoryMethod.invoke(factory).asInstanceOf[(String => String)]

                if(verbose) println("info: successfully loaded class '%s' from plug-in '%s'...".format(classNamesWithPackagePath, pluginFilePath))
                return Left(factoryFunction)
            } catch {
                case t: Throwable =>
                    lastError = Some(t)
                    if(verbose) println("info: failed to load class '%s' from plug-in '%s'...: %s".format(classNamesWithPackagePath, pluginFilePath, t.toString))
            }
        }

        lastError match {
            case Some(t) => Right(t)
            case None => Right(new IllegalStateException("none of the factory class candidates found in '%s': %s".format(pluginFilePath, classNamesWithPackagePathsToTry.mkString(", "))))
        }
    }

}





