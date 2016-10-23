package be.rubenpieters.free

/**
  * Created by ruben on 23/10/2016.
  */
sealed trait GithubApiDsl[A]

case class ListIssues(owner: Owner, repo: Repo)
  extends GithubApiDsl[List[Issue]]

case class GetComments(owner: Owner, repo: Repo, issue: Issue)
  extends GithubApiDsl[List[Comment]]

case class GetUser(login: UserLogin)
  extends GithubApiDsl[User]