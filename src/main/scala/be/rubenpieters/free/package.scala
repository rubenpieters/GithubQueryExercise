package be.rubenpieters

import cats.data.Xor

/**
  * Created by ruben on 23/10/2016.
  */
package object free {
  type GithubApiDslResult[A] = Xor[Throwable, A]

  type Owner = String
  type Repo = String
  type Comment = String
  type UserLogin = String
}
