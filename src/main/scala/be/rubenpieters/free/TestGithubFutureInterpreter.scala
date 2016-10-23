package be.rubenpieters.free

import be.rubenpieters.model.github.{Comment, Issue, User, UserReference}
import cats.data.Xor
import cats.~>
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by ruben on 23/10/2016.
  */
class TestGithubFutureInterpreter(
                                   issues: Map[(String, String), List[Issue]]
                                   , comments: Map[(String, String, IssueNumber), List[Comment]]
                                   , users: Map[UserReference, User]
                                 )(implicit ec: ExecutionContext)
  extends (GithubApiDsl ~> Future) {
  def simulateDelay() = Thread.sleep(500)

  override def apply[A](fa: GithubApiDsl[A]): Future[A] = fa match {
    case ListIssues(owner, repo) =>
      Future {
        LoggerFactory.getLogger(getClass).debug(s"get issues owner: $owner repo: $repo")
        simulateDelay()
        Xor.catchOnly[NoSuchElementException](issues((owner, repo)))
      }
    case GetComments(owner, repo, issueNr) =>
      Future {
        LoggerFactory.getLogger(getClass).debug(s"get comments owner: $owner repo: $repo issueNr: $issueNr")
        simulateDelay()
        Xor.catchOnly[NoSuchElementException](comments((owner, repo, issueNr)))
      }
    case GetUser(userRef: UserReference) =>
      Future {
        LoggerFactory.getLogger(getClass).debug(s"get user userRef: $userRef")
        simulateDelay()
        Xor.catchOnly[NoSuchElementException](users(userRef))
      }
  }
}
