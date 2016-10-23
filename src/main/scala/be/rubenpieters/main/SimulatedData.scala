package be.rubenpieters.main

import be.rubenpieters.model.github.{Comment, Issue, User, UserReference}

/**
  * Created by ruben on 23/10/2016.
  */
object SimulatedData {
  val issues = Map(
    ("typelevel", "cats") ->
      ((1 to 5).map(x => Issue(x, x, s"issue$x", UserReference(1))) ++
        (6 to 10).map(x => Issue(x, x, s"issue$x", UserReference(2)))
        ).toList
  )

  val comments = (1L to 10L).map { x =>
    ("typelevel", "cats", x) -> List(
      Comment(x, "body", UserReference(1))
    )
  }.toMap

  val users = Map(
    UserReference(1) -> User(1, "user1")
    ,UserReference(2) -> User(2, "user2")
  )
}
