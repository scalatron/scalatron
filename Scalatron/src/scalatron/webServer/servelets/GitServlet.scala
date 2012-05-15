package scalatron.webServer.servelets

import scala.collection.JavaConverters._

import java.util.{Collection => JCollection}
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import javax.xml.bind.DatatypeConverter

import org.eclipse.jgit.api.{ResetCommand, Git}
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.{ReceiveCommand, PostReceiveHook, ReceivePack}
import org.eclipse.jgit.transport.resolver.{RepositoryResolver, ReceivePackFactory}

import scalatron.scalatron.api.Scalatron.User
import scalatron.scalatron.impl.GitServer

case class GitServlet(context: WebContext, gitServer: GitServer) extends org.eclipse.jgit.http.server.GitServlet {

    import context.scalatron

    override def init(config: ServletConfig) {
        setReceivePackFactory(new Receive());
        setRepositoryResolver(new UserRepositoryResolver());
        super.init(config);
    }

    override def service(req: HttpServletRequest, rsp: HttpServletResponse) {
        if (!getUser(req).isDefined) {
            rsp.setHeader("WWW-Authenticate", "Basic realm=\"Scalatron git server\"")
            rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "")
        }
        else {
            super.service(req, rsp)
        }
    }

    def getUser(req: HttpServletRequest) = {
        Option(req.getHeader("authorization")) flatMap {
            authorization =>
                val decoded = new String(DatatypeConverter.parseBase64Binary(authorization.split(" ")(1))).split(":", 2)
                for (user <- scalatron.user(decoded(0)); if user.getPasswordOpt.exists(_ == decoded(1))) yield user
        }
    }

    class UserRepositoryResolver extends RepositoryResolver[HttpServletRequest] {

        override def open(req: HttpServletRequest, name: String) = {
            getUser(req).flatMap(gitServer.get(_)).getOrElse(throw new RepositoryNotFoundException(name))
        }
    }

    class Receive extends ReceivePackFactory[HttpServletRequest] {

        def create(req: HttpServletRequest, repo: Repository) = {
            val rp = new ReceivePack(repo)
            rp.setPostReceiveHook(new ReceiveCommits(getUser(req).get))
            rp
        }

        class ReceiveCommits(user: User) extends PostReceiveHook {

            def onPostReceive(rp: ReceivePack, commands: JCollection[ReceiveCommand]) {
                resetHead(rp)
                val ok = commands.asScala.forall(_.getResult == ReceiveCommand.Result.OK)
                if (ok && build(rp)) {
                    List("", "Your Scalatron bot has been built successfully", "").map(rp.sendMessage)
                }
            }

            // TODO It would be nice to do this in preReceive to stop the push, but I'm not sure how to get the files
            def build(rp: ReceivePack) = {
                val buildResult = user.buildSources()
                def getMessages = buildResult.messages.map(message => "%s:%s %s" format(message.sourceFile, message.lineAndColumn, message.multiLineMessage))
                if (!buildResult.successful) {
                    (List("") ++ getMessages ++ List("")).map(rp.sendMessage)
                }
                buildResult.successful
            }

            def resetHead(rp: ReceivePack) = new Git(rp.getRepository).reset.setMode(ResetCommand.ResetType.HARD).setRef("HEAD").call
        }

    }

}
