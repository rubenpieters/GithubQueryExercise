package be.rubenpieters.main

import cats.implicits._
import java.util.concurrent.Executors

import be.rubenpieters.model.github.UserReference
import be.rubenpieters.tagless.GithubApi
import cats.Monad

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Created by ruben on 23/10/2016.
  */
object TaglessMain {
  import SimulatedData._

  def main(args: Array[String]) = {
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))

    val future = GithubApi.allUsers("typelevel", "cats")(implicitly[Monad[Future]]).apply(new TestGithubFutureTaglessInterpreter(issues, comments, users))
    val futureResult = Await.result(future, 1.minute)
    println(futureResult)
  }
}
