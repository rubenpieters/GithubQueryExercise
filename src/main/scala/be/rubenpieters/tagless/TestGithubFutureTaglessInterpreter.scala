package be.rubenpieters.tagless

import be.rubenpieters.free.{IssueNumber, Owner, Repo}
import be.rubenpieters.model.github.{Comment, Issue, User, UserReference}
import be.rubenpieters.tagless.GithubApi.GithubApiResult
import cats.Monad
import cats.data.Xor
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by ruben on 23/10/2016.
  */
class TestGithubFutureTaglessInterpreter(
                                          issues: Map[(String, String), List[Issue]]
                                          , comments: Map[(String, String, IssueNumber), List[Comment]]
                                          , users: Map[UserReference, User]
                                        )(implicit ec: ExecutionContext)
  extends GithubApiAlg[Future] {
  def simulateDelay() = Thread.sleep(500)

  override def listIssues(owner: Owner, repo: Repo): Future[GithubApiResult[List[Issue]]] = Future {
    LoggerFactory.getLogger(getClass).debug(s"get issues owner: $owner repo: $repo")
    simulateDelay()
    Xor.catchOnly[NoSuchElementException](issues((owner, repo)))
  }


  override def getComments(owner: Owner, repo: Repo, issueNr: IssueNumber): Future[GithubApiResult[List[Comment]]] = Future {
    LoggerFactory.getLogger(getClass).debug(s"get comments owner: $owner repo: $repo issueNr: $issueNr")
    simulateDelay()
    Xor.catchOnly[NoSuchElementException](comments((owner, repo, issueNr)))
  }

  override def getUser(userRef: UserReference): Future[GithubApiResult[User]] = Future {
    LoggerFactory.getLogger(getClass).debug(s"get user userRef: $userRef")
    simulateDelay()
    Xor.catchOnly[NoSuchElementException](users(userRef))
  }
}

object Optimizer {
  def cachedUsers[F[_] : Monad](alg: GithubApiAlg[F], mapping: Map[UserReference, User]) = new GithubApiAlg[F] {
    val userCache: Map[UserReference, User] = mapping

    override def listIssues(owner: Owner, repo: Repo): F[GithubApiResult[List[Issue]]] = alg.listIssues(owner, repo)

    override def getUser(userRef: UserReference): F[GithubApiResult[User]] =
        userCache.get(userRef) match {
          case Some(user) => Monad[F].pure(Xor.Right(user))
          case None => alg.getUser(userRef)
        }

    override def getComments(owner: Owner, repo: Repo, issueNr: IssueNumber): F[GithubApiResult[List[Comment]]] = alg.getComments(owner, repo, issueNr)
  }
}