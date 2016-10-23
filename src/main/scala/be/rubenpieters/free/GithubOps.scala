package be.rubenpieters.free

import be.rubenpieters.model.github.{Comment, Issue, User, UserReference}
import cats.data.XorT
import cats.free.{Free, FreeApplicative}
import cats.implicits._

/**
  * Created by ruben on 23/10/2016.
  */
class GithubOps {
  type GithubOpsFree[A] = Free[GithubApiDsl, A]
  type GithubOpsFreeApp[A] = FreeApplicative[GithubApiDsl, A]

  def listIssues(owner: Owner, repo: Repo): GithubOpsFree[GithubApiDslResult[List[Issue]]] =
    Free.liftF(ListIssues(owner, repo))

  def getComments(owner: Owner, repo: Repo, issueNr: IssueNumber): GithubOpsFree[GithubApiDslResult[List[Comment]]] =
    Free.liftF(GetComments(owner, repo, issueNr))

  def getUser(userRef: UserReference): GithubOpsFree[GithubApiDslResult[User]] =
    Free.liftF(GetUser(userRef))

  def allUsers(owner: Owner, repo: Repo)
  : GithubOpsFree[GithubApiDslResult[List[(Issue, List[(Comment, User)])]]] = (for {
    issues <- XorT[GithubOpsFree, Throwable, List[Issue]](
      listIssues(owner, repo))

    issueComments <-
      issues.traverse(issue =>
        XorT[GithubOpsFree, Throwable, List[Comment]](getComments(owner, repo, issue.number))
          .map((issue, _))
      ): XorT[GithubOpsFree, Throwable, List[(Issue, List[Comment])]]

    users <-
      issueComments.traverse { case (issue, comments) =>
        comments.traverse(comment =>
          XorT[GithubOpsFree, Throwable, User](getUser(comment.user))
            .map((comment, _))
        ).map((issue, _))
      }: XorT[GithubOpsFree, Throwable, List[(Issue, List[(Comment, User)])]]
  } yield users
    ).value

  def listIssuesApp(owner: Owner, repo: Repo): GithubOpsFreeApp[GithubApiDslResult[List[Issue]]] =
    FreeApplicative.lift(ListIssues(owner, repo))

  def getCommentsApp(owner: Owner, repo: Repo, issueNr: IssueNumber): GithubOpsFreeApp[GithubApiDslResult[List[Comment]]] =
    FreeApplicative.lift(GetComments(owner, repo, issueNr))

  def getUserApp(userRef: UserReference): GithubOpsFreeApp[GithubApiDslResult[User]] =
    FreeApplicative.lift(GetUser(userRef))
}
