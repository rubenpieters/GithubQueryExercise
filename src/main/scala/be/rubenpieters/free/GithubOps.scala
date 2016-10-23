package be.rubenpieters.free

import be.rubenpieters.model.github.{Issue, User, UserReference}
import cats.free.Free

/**
  * Created by ruben on 23/10/2016.
  */
class GithubOps {
  type GithubOpsFree[A] = Free[GithubApiDsl, A]

  def listIssues(owner: Owner, repo: Repo): GithubOpsFree[GithubApiDslResult[List[Issue]]] =
    Free.liftF(ListIssues(owner, repo))

  def getComments(owner: Owner, repo: Repo, issue: Issue): GithubOpsFree[GithubApiDslResult[List[Comment]]] =
    Free.liftF(GetComments(owner, repo, issue))

  def getUser(userRef: UserReference): GithubOpsFree[GithubApiDslResult[User]] =
    Free.liftF(GetUser(userRef))

}
