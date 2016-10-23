package be.rubenpieters.main

import be.rubenpieters.free.{IssueNumber, Owner, Repo}
import be.rubenpieters.model.github.{Comment, Issue, User, UserReference}
import be.rubenpieters.tagless.GithubApi.GithubApiResult
import be.rubenpieters.tagless.GithubApiAlg
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
