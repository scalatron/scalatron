package scalatron.scalatron.impl

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
      * @param jarFile a File object representing the plug-in (.jar) file
      * @param userName the user name, will be tried as a package name
      * @param gameSpecificPackagePath e.g. "scalatron.botwar.botPlugin"
      * @param factoryClassName e.g. "ControlFunctionFactory"
      */
    def loadBotControlFunctionFrom(
        jarFile: File,
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
        val fullyQualifiedControlFunctionFactoryClassNamesToTry = Iterable(
            gameSpecificPackagePath + "." + userName + "." + factoryClassName,
            userName + "." + factoryClassName,
            gameSpecificPackagePath + "." + factoryClassName,
            factoryClassName
        )
        val controlFunctionClassName = "ControlFunction"
        val fullyQualifiedControlFunctionClassNamesToTry = Iterable(
            gameSpecificPackagePath + "." + userName + "." + controlFunctionClassName,
            userName + "." + controlFunctionClassName,
            gameSpecificPackagePath + "." + controlFunctionClassName,
            controlFunctionClassName
        )

        // => look for: class ControlFunctionFactory { def create() : String => String }
        if(verbose) println("info: will attempt to load bot plug-in '%s'...".format(jarFile.getAbsolutePath))
        loadClassAndMethodFromJar(jarFile, fullyQualifiedControlFunctionFactoryClassNamesToTry, "create", None, verbose) match {
            case Right(throwable) =>
                // no "ControlFunctionFactory" class was present in the plug-in
                // Maybe this is a Java plug-in, or a Scala plug-in with a static control function?
                // => look for: class ControlFunction { def respond(input: String) : String }
                loadClassAndMethodFromJar(jarFile, fullyQualifiedControlFunctionClassNamesToTry, "respond", Some(classOf[String]), verbose) match {
                    case Right(throwable2) =>
                        // no "ControlFunction" class was present in the plug-in => nothing else to try
                        if(verbose) System.err.println("    error: failed to load a control function factory or control function from plug-in '%s': %s".format(jarFile.getAbsolutePath, throwable2.toString))
                        Right(throwable2)

                    case Left((extractedClass,methodOnExtractedClass)) =>
                        // a "ControlFunction" class was present in the plug-in => try to load it
                        if(verbose) println("    info: ControlFunction class and method located in bot plug-in '%s', will try to instantiate control function...".format(jarFile.getAbsolutePath))
                        try {
                            val classInstance = extractedClass.newInstance()
                            val controlFunction: String => String = (input: String) => methodOnExtractedClass.invoke(classInstance, input).asInstanceOf[String]
                            val controlFunctionFactory: () => (String => String) = () => controlFunction
                            if(verbose) println("    info: successfully extracted control function from bot plug-in '%s'...".format(jarFile.getAbsolutePath))
                            Left(controlFunctionFactory)
                        } catch {
                            case t: Throwable =>
                                // a "ControlFunctionFactory" class was present, but it failed to load; makes no sense to try the "ControlFunction" variant
                                if(verbose) System.err.println("    error: failed to extract control function factory from bot plug-in '%s': %s".format(jarFile.getAbsolutePath, t.toString))
                                Right(t)
                        }
                }

            case Left((extractedClass,methodOnExtractedClass)) =>
                // a "ControlFunctionFactory" class was present in the plug-in => try to load it
                if(verbose) println("    info: ControlFunctionFactory class and method located in bot plug-in '%s', will try to instantiate control function factory...".format(jarFile.getAbsolutePath))
                try {
                    val classInstance = extractedClass.newInstance()
                    val factoryFunction: () => (String => String) = () => methodOnExtractedClass.invoke(classInstance).asInstanceOf[(String => String)]
                    if(verbose) println("    info: successfully extracted control function factory from bot plug-in '%s'...".format(jarFile.getAbsolutePath))
                    Left(factoryFunction)
                } catch {
                    case t: Throwable =>
                        // a "ControlFunctionFactory" class was present, but it failed to load; makes no sense to try the "ControlFunction" variant
                        if(verbose) System.err.println("    error: failed to extract control function factory from bot plug-in '%s': %s".format(jarFile.getAbsolutePath, t.toString))
                        Right(t)
                }
        }
    }


    /** Creates a URLClassLoader for a given .jar file and attempts to load a class via the loader using a sequence
      * of class names in the sequence in which they appear in the given collection. All exceptions that are detected
      * are mapped to Right(Throwable). If a class and member function was successfully loaded, it is returned as
      * Left((extractedClass,methodOnExtractedClass)). The caller is responsible for instantiating the class and
      * invoking the method, as well as for then making the appropriate casts.
      * @param jarFile a File object representing the plug-in (.jar) file
      * @param fullyQualifiedClassNamesToTry a collection of class names (including package path) to try to load,
      *                                      e.g. Iterable("daniel.Factory", "scalatron.daniel.Factory")
      * @param methodName the name of the method to extract, e.g. "create"
      * @param methodParameterClass an optional parameter type if the method takes a parameter
      * @param verbose if true, prints what it is doing to the console
      * @return Left((extractedClass,methodOnExtractedClass)), or the last encountered exception as Right(throwable)
      */
    def loadClassAndMethodFromJar(
        jarFile: File,
        fullyQualifiedClassNamesToTry: Iterable[String],
        methodName: String,
        methodParameterClass: Option[java.lang.Class[_]],
        verbose: Boolean): Either[(Class[_],java.lang.reflect.Method), Throwable] =
    {
        try {
            val classLoader = new URLClassLoader(Array(jarFile.toURI.toURL), this.getClass.getClassLoader)

            val pluginFilePath = jarFile.getAbsolutePath

            // can't return from a .foreach to shortcircuit, throws scala.runtime.NonLocalReturnControl; so we iterate manually
            var lastError: Option[Throwable] = None
            val iterator = fullyQualifiedClassNamesToTry.iterator
            while(iterator.hasNext) {
                val fullyQualifiedClassName = iterator.next()
                try {
                    if(verbose) println("    info: will try to load class '%s' from plug-in '%s'...".format(fullyQualifiedClassName, pluginFilePath))
                    val extractedClass = Class.forName(fullyQualifiedClassName, true, classLoader)

                    if(verbose) println("    info: class '%s' loaded, will try to find method '%s'...".format(fullyQualifiedClassName, methodName))
                    val methodOnExtractedClass = methodParameterClass match {
                        case None => extractedClass.getMethod(methodName)
                        case Some(parameterClass) => extractedClass.getMethod(methodName, parameterClass)
                    }

                    if(verbose) println("    info: successfully located method '%s' on class '%s' in plug-in '%s'...".format(methodName, fullyQualifiedClassName, pluginFilePath))

                    return Left((extractedClass,methodOnExtractedClass))
                } catch {
                    case t: Throwable =>
                        lastError = Some(t)
                        if(verbose) println("    info: failed to load class '%s' from plug-in '%s'...: %s".format(fullyQualifiedClassName, pluginFilePath, t.toString))
                }
            }

            lastError match {
                case Some(t) => Right(t)
                case None => Right(new IllegalStateException("none of the factory class candidates found in '%s': %s".format(pluginFilePath, fullyQualifiedClassNamesToTry.mkString(", "))))
            }
        } catch {
            case t: Throwable => return Right(t)
        }
    }
}





