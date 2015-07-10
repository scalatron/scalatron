package scalatron.scalatron.impl

import java.util.concurrent.{ThreadPoolExecutor, ThreadFactory, LinkedBlockingQueue, TimeUnit}
import java.security.Permission
import java.io.FilePermission
import java.lang.reflect.ReflectPermission

import scala.concurrent.ExecutionContext


object ExecutionContextForUntrustedCode
{
    /** Creates an ExecutionContext instance based on a thread pool whose threads are optionally sandboxed via a
      * custom security manager. This execution context should be used for all invocations of untrusted code, such
      * as that of bot plug-ins' control functions.
      *
      * Notes:
      * (a) using a collection of paths (e.g. readablePathPrefixes: Iterable[String]) does not work because this will
      *     result in a java.lang.ClassCircularityError when we check
      *     val tryingToAccessPluginDirectory = readablePathPrefixes.find(path.startsWith).isDefined.
      * (b) sandboxed threads are identified by their ID, which is maintained in a concurrent skip list set.
      * (c) the following invocation points need to be secured:
      *     (1) the processing loop during simulation - done.
      *     (2) sandbox processing via REST API - TODO.
      *     (3) execution of static code during class loading - TODO.
      *     (4) execution of control function factory code at start of round - TODO.
      * @param applicationJarDirectoryPath a path prefix from which plug-ins may read
      * @param applicationOutDirectoryPath a path prefix from which plug-ins may read
      * @param pluginDirectoryPath a path prefix from which plug-ins may read
      * @param secureMode if true, the custom security manager is activated and plug-ins are sandboxed
      * @param verbose if true, verbose logging is enabled
      */
    def create(
        applicationJarDirectoryPath: String,
        applicationOutDirectoryPath: String,
        pluginDirectoryPath: String,
        secureMode: Boolean,
        verbose: Boolean) =
    {
        val availableProcessors = Runtime.getRuntime.availableProcessors
        val threadCount = new java.util.concurrent.atomic.AtomicLong(0L)

        // set maintains list of all currently running untrusted thread IDs (so we can identify them in the SecurityManager)
        val threadSet = new java.util.concurrent.ConcurrentSkipListSet[Long]

        class PluginThread(target: Runnable) extends Thread(target) {
            override def run() {
                threadSet.add(getId)
                // System.err.println("Launching untrusted thread: " + getName)
                super.run()
                // System.err.println("untrusted thread ending: " + getName)
                threadSet.remove(getId)
            }
        }

        val defaultThreadPool = new java.util.concurrent.ThreadPoolExecutor(
            availableProcessors,
            Int.MaxValue,
            100L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue[Runnable],
            new ThreadFactory {
                def newThread(runnable: Runnable) = {
                    val t = new PluginThread(runnable)
                    t.setName("UntrustedThread-" + threadCount.incrementAndGet)
                    t.setDaemon(true)
                    t
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy
        )

        val executionContextForUntrustedCode = ExecutionContext.fromExecutorService(defaultThreadPool)


        case class PluginSecurityManager(
            applicationJarDirectoryPath: String,
            applicationOutDirectoryPath: String,
            pluginDirectoryPath: String) extends SecurityManager {
            override def checkPermission(perm: Permission) { check(perm) }
            override def checkPermission(perm: Permission, context: Any) { check(perm) }

            private def check(permission: Permission) {
                val currentThread = Thread.currentThread()

                val isPluginThread = threadSet.contains(currentThread.getId)
                if(isPluginThread) {
                    permission match {
                        case filePermission: FilePermission =>
                            permission.getActions match {
                                case "read" =>
                                    // request for read-only access -- is the request inside permitted source directories?
                                    val path = permission.getName
                                    if( path.startsWith(applicationJarDirectoryPath) ||
                                        path.startsWith(applicationOutDirectoryPath) ||
                                        path.startsWith(pluginDirectoryPath) ) {
                                        return // granted
                                    }

                                case _ => // denied
                            }

                        case runtimePermission: RuntimePermission =>
                            permission.getName match {
                                case "createClassLoader" =>
                                    // TODO: this is triggered by the control function factory invocation - without it, plug-ins fail to load :-(
                                    // System.err.println("warning: granting permission to untrusted code: '%s'".format(permission))
                                    return // granted

                                case "accessClassInPackage.sun.reflect" =>
                                    // TODO: this is triggered by the control function factory invocation - without it, plug-ins fail to load :-(
                                    // System.err.println("warning: granting permission to untrusted code: '%s'".format(permission))
                                    return // granted

                                case "accessDeclaredMembers" =>
                                  // this is triggered by Enumeration$$populateNameMap
                                  return // granted

                                case _ => // denied
                            }

                        case reflectPermission: ReflectPermission =>
                            permission.getName match {
                                case "suppressAccessChecks" =>
                                    // TODO: this is triggered by the control function factory invocation - without it, plug-ins fail to load :-(
                                    // System.err.println("warning: granting permission to untrusted code: '%s'".format(permission))
                                    return // granted

                                case _ => // denied
                            }

                        case _ => // denied
                    }

                    System.err.println("warning: untrusted code was denied permission: '%s'".format(permission))
                    throw new SecurityException("Permission denied: " + permission)
                }
            }
        }


        if(secureMode) {
            val pluginSecurityManager =
                new PluginSecurityManager(
                    "/Users/dev/Scalatron/Scalatron/bin/Scalatron.jar",
                    "/Users/dev/Scalatron/Scalatron/out/",
                    "/Users/dev/ScalatronPrivate/_testing/bots/"
                )
            System.setSecurityManager(pluginSecurityManager)
            // val priorSecurityManager = System.getSecurityManager
            // System.setSecurityManager(priorSecurityManager)
        }


        executionContextForUntrustedCode
    }

}
