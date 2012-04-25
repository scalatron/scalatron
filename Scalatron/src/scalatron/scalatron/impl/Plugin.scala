package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import java.net.URLClassLoader
import java.io.File


/** A plugin contains the name of the player that created it (derived from the plugin
  * directory name) as well the control function factory that was loaded from it.
  */
trait Plugin {
    def name: String // "daniel"
    def controlFunctionFactory: () => ( String => String ) // control function factory
    override def toString = name
}


object Plugin {


    /** Information about where an external plug-in was loaded from, or where a plug-in
      * was attempted to be loaded from when a load failure occurred.
      */
    trait DiskInfo {
        def dirPath: String // e.g. "/Users/Scalatron/Scalatron/bots/Daniel" (no terminal slash)
        def filePath: String // e.g. "/Users/Scalatron/Scalatron/bots/Daniel/ScalatronBot.jar"
        def fileTime: Long // milliseconds since the epoch, as returned by File.lastModified()
        override def toString = filePath
    }


    /** Plugin.Internal:
      * The game server has the ability to activate internally implemented control functions
      * for debugging purposes, i.e. control functions that are not loaded from .jar file on
      * disk but instantiated from a collection of functions configured within the server itself.
      * Use e.g. to debug complex bots.
      */
    case class Internal(name: String, controlFunctionFactory: () => ( String => String )) extends Plugin


    /** Plugin.External:
      * Plugin based on an externally implemented control function, JAR was loaded from 'path'.
      * @param dirPath e.g. "/Users/Scalatron/Scalatron/bots/Daniel" (no terminal slash)
      * @param filePath e.g. "/Users/Scalatron/Scalatron/bots/Daniel/ScalatronBot.jar"
      * @param fileTime milliseconds since the epoch, as returned by File.lastModified()
      * @param name e.g. "Daniel"
      * @param controlFunctionFactory the control function factory
      */
    case class External(
        dirPath: String,
        filePath: String,
        fileTime: Long,
        name: String,
        controlFunctionFactory: () => ( String => String ))
        extends Plugin with DiskInfo {
        override def toString = filePath
    }


    /** Plugin.LoadFailure: Container used to report a plug-in load failure.
      * @param dirPath e.g. "/Users/Scalatron/Scalatron/bots/Daniel" (no terminal slash)
      * @param filePath e.g. "/Users/Scalatron/Scalatron/bots/Daniel/ScalatronBot.jar"
      * @param fileTime milliseconds since the epoch, as returned by File.lastModified()
      * @param exception the exception that caused/explains the load failure
      */
    case class LoadFailure(dirPath: String, filePath: String, fileTime: Long, exception: Throwable) extends DiskInfo {
        override def toString = exception + ": " + filePath
    }


    /** Returns either a control function factory (success) or an exception (failure).
      * @param pluginFile a File object representing the plug-in (.jar) file
      * @param factoryClassPackage e.g. "scalatron.botwar.botPlugin"
      * @param factoryClassName e.g. "ControlFunctionFactory"
      * */
    def loadFrom(
        pluginFile: File,
        factoryClassPackage: String,
        factoryClassName: String,
        verbose: Boolean): Either[() => ( String => String ), Throwable] =
    {
        try {
            val classLoader = new URLClassLoader(Array(pluginFile.toURI.toURL), this.getClass.getClassLoader)

            // try the fully qualified package + class name first
            val fullFactoryClassName = factoryClassPackage + "." + factoryClassName
            val factoryClass =
                try {
                    Class.forName(fullFactoryClassName, true, classLoader)
                } catch {
                    case t: Throwable =>
                        try {
                            if( verbose ) {
                                println("info: failed to load fully qualified factory from plug-in '" + pluginFile.getAbsolutePath + "':" + t)
                                println("info: trying unqualified factory class name... (" + factoryClassName + ")")
                            }
                            Class.forName(factoryClassName, true, classLoader)
                        } catch {
                            case t: Throwable =>
                                System.err.println("failed to load either qualified or unqualified factory from plug-in '" + pluginFile.getAbsolutePath + "':" + t)
                                throw t
                        }
                }

            val factoryMethod = factoryClass.getMethod("create")

            // wrap the Java factory method into a Scala factory function
            val factory = factoryClass.newInstance()
            val factoryFunction: () => ( String => String ) = () => factoryMethod.invoke(factory).asInstanceOf[( String => String )]

            Left(factoryFunction)
        } catch {
            case t: Throwable => Right(t)
        }
    }

}





