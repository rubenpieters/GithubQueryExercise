package be.rubenpieters.free

import be.rubenpieters.model.github.{Comment, Issue, User, UserReference}

/**
  * Created by ruben on 23/10/2016.
  */
sealed trait GithubApiDsl[A]

case class ListIssues(owner: Owner, repo: Repo)
  extends GithubApiDsl[GithubApiDslResult[List[Issue]]]

case class GetComments(owner: Owner, repo: Repo, issue: IssueNumber)
  extends GithubApiDsl[GithubApiDslResult[List[Comment]]]

case class GetUser(userRef: UserReference)
  extends GithubApiDsl[GithubApiDslResult[User]]