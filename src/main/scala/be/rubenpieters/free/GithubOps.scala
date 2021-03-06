package be.rubenpieters.free

import be.rubenpieters.model.github.{Comment, Issue, User, UserReference}
import cats.data.{Coproduct, Xor, XorT}
import cats.free.{Free, FreeApplicative}
import cats.implicits._
import cats.{Applicative, Functor, Monad, RecursiveTailRecM, ~>}

/**
  * Created by ruben on 23/10/2016.
  */
class GithubOps {
  // MONAD

  type GithubOpsFree[A] = Free[GithubApiDsl, A]

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

  // APPLICATIVE

  type GithubOpsFreeApp[A] = FreeApplicative[GithubApiDsl, A]

  def listIssuesApp(owner: Owner, repo: Repo): GithubOpsFreeApp[GithubApiDslResult[List[Issue]]] =
    FreeApplicative.lift(ListIssues(owner, repo))

  def getCommentsApp(owner: Owner, repo: Repo, issueNr: IssueNumber): GithubOpsFreeApp[GithubApiDslResult[List[Comment]]] =
    FreeApplicative.lift(GetComments(owner, repo, issueNr))

  def getUserApp(userRef: UserReference): GithubOpsFreeApp[GithubApiDslResult[User]] =
    FreeApplicative.lift(GetUser(userRef))

  def getCommentsByListOfIssue(owner: Owner, repo: Repo, issueList: List[Issue])
  : GithubOpsFreeApp[GithubApiDslResult[List[(Issue, List[Comment])]]] = {
    issueList
      .traverse(issue => getCommentsApp(owner, repo, issue.number)
        .map(_.map((issue, _)))
      ).map(_.sequence)
  }

  def addUserInfo(result: GithubApiDslResult[List[(Issue, List[Comment])]])
  : GithubOpsFreeApp[GithubApiDslResult[List[(Issue, List[(Comment, User)])]]] = {
    val x = result.traverse { issueWithCommentsList =>
      val x = issueWithCommentsList.traverse { case (issue, commentList) =>
        (commentList.traverse(comment => getUserApp(comment.user)
          .map(_.map((comment, _)))).map(_.sequence): GithubOpsFreeApp[GithubApiDslResult[List[(Comment, User)]]])
          .map(_.map((issue, _)))
      }
      x.map(_.sequence): GithubOpsFreeApp[GithubApiDslResult[List[(Issue, List[(Comment, User)])]]]
    }
    x.map(_.flatten)
  }

  val logins: (GithubApiDsl ~> λ[α => Set[UserReference]]) = {
    new (GithubApiDsl ~> λ[α => Set[UserReference]]) {
      override def apply[A](fa: GithubApiDsl[A]): Set[UserReference] = fa match {
        case GetUser(userRef) => Set(userRef)
        case _ => Set.empty
      }
    }
  }

  def extractLogins(p: GithubOpsFreeApp[_]): Set[UserReference] =
    p.analyze(logins)

  def precompute[A, F[_]](
                           p: GithubOpsFreeApp[A]
                           , interp: GithubApiDsl ~> F
                         )(implicit applicative: Applicative[F])
  : F[Map[UserReference, User]] = {
    val distinctUserRefs = extractLogins(p).toList
    // So actually, there is an interesting problem here when using this precompute when modeling failures as well
    // We can just drop the failures, which means that we will still fetch duplicates for all the initially failed ones
    // But are there other possibilities?
    val fetched: F[List[GithubApiDslResult[User]]] = distinctUserRefs.traverse(getUserApp(_)).foldMap(interp)
    val filteredFetched: F[List[User]] = Functor[F].map(fetched)(list =>
      list.flatMap{ userOrErr =>
        userOrErr.fold(_ => None, Some(_))
      })
    Functor[F].map(filteredFetched)(distinctUserRefs.zip(_).toMap)
  }

  def optimizeNat[F[_]](
                         mapping: Map[UserReference,User]
                         , interp: GithubApiDsl ~> F
                       )(implicit applicative: Applicative[F])
  : GithubApiDsl ~> F = new (GithubApiDsl ~> F) {
    override def apply[A](fa: GithubApiDsl[A]): F[A] = fa match {
      case ffa @ GetUser(userReference) =>
        mapping.get(userReference) match {
          case Some(user) => Applicative[F].pure(Xor.Right(user))
          case None => interp(ffa)
        }
      case _ => interp(fa)
    }
  }

  def interpretOpt[A, F[_]](p: GithubOpsFreeApp[A], interpret: GithubApiDsl ~> F)(implicit monad: Monad[F]): F[A] = {
    val mapping: F[Map[UserReference, User]] = precompute(p, interpret)

    mapping.flatMap { m =>
      val betterNat = optimizeNat(m, interpret)
      p.foldMap(betterNat)
    }
  }

  def interpretAppOpt[A, F[_]](interpret: GithubApiDsl ~> F)(implicit monad: Monad[F]): GithubOpsFreeApp ~> F =
    new (GithubOpsFreeApp ~> F) {
      override def apply[A](fa: GithubOpsFreeApp[A]): F[A] = {
        val mapping: F[Map[UserReference, User]] = precompute(fa, interpret)

        mapping.flatMap { m =>
          val betterNat = optimizeNat(m, interpret)
          fa.foldMap(betterNat)
        }
      }
    }

  // COMBINED

  type GithubCoproductFree[A] = Free[Coproduct[GithubApiDsl, GithubOpsFreeApp, ?], A]

  def listIssuesCop(owner: Owner, repo: Repo): GithubCoproductFree[GithubApiDslResult[List[Issue]]] =
    Free.liftF(Coproduct.left(ListIssues(owner, repo)))

  def getCommentsCop(owner: Owner, repo: Repo, issueNr: IssueNumber): GithubCoproductFree[GithubApiDslResult[List[Comment]]] =
    Free.liftF(Coproduct.left(GetComments(owner, repo, issueNr)))

  def getUserCop(userRef: UserReference): GithubCoproductFree[GithubApiDslResult[User]] =
    Free.liftF(Coproduct.left(GetUser(userRef)))

  def embed[A](p: GithubOpsFreeApp[A]): GithubCoproductFree[A] =
    Free.liftF[Coproduct[GithubApiDsl, GithubOpsFreeApp, ?], A](Coproduct.right(p))

  def embedXorT[A, B](xort: XorT[GithubOpsFreeApp, A, B]): XorT[GithubCoproductFree, A, B] =
    XorT[GithubCoproductFree, A, B](embed(xort.value))

  def mixedInterpreter[F[_]](freeInterp: GithubApiDsl ~> F, applInterp: GithubOpsFreeApp ~> F): Coproduct[GithubApiDsl, GithubOpsFreeApp, ?] ~> F =
    freeInterp.or[GithubOpsFreeApp](applInterp)

  def runCop[F[_], A](p: GithubCoproductFree[A], freeInterp: GithubApiDsl ~> F, applInterp: GithubOpsFreeApp ~> F)
                  (implicit monad: Monad[F], r: RecursiveTailRecM[F])
  : F[A] =
    p.foldMap(mixedInterpreter(freeInterp, applInterp))

//  def allUsersCop(owner: Owner, repo: Repo)
//  : GithubCoproductFree[GithubApiDslResult[List[(Issue, List[(Comment, User)])]]] = (for {
//    issues <- XorT[GithubCoproductFree, Throwable, List[Issue]](
//      listIssuesCop(owner, repo))
//
//    issueComments <- embedXorT {
//      issues.traverse(issue =>
//        XorT[GithubOpsFreeApp, Throwable, List[Comment]](getCommentsApp(owner, repo, issue.number))
//          .map((issue, _))
//      ): XorT[GithubOpsFreeApp, Throwable, List[(Issue, List[Comment])]]
//    }
//    /*embed {
//      issues.traverse(issue =>
//        XorT[GithubCoproductFree, Throwable, List[Comment]](getCommentsApp(owner, repo, issue.number))
//          .map((issue, _))
//      ): XorT[GithubCoproductFree, Throwable, List[(Issue, List[Comment])]]
//    }*/
//
//    users <- null
//    /*embed {
//      issueComments.traverse { case (issue, comments) =>
//        comments.traverse(comment =>
//          XorT[GithubCoproductFree, Throwable, User](getUserApp(comment.user))
//            .map((comment, _))
//        ).map((issue, _))
//      }: XorT[GithubCoproductFree, Throwable, List[(Issue, List[(Comment, User)])]]
//    }*/
//  } yield users
//    ).value
}

