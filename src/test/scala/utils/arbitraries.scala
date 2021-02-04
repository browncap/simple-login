package com.login.utils

import com.login.{LoginUser, Password, Username}
import org.scalacheck.{Arbitrary, Gen}

object arbitraries {
  implicit val loginUserArb: Arbitrary[LoginUser] = Arbitrary {
    for {
      u <- Gen.alphaNumStr.map(Username(_))
      p <- Gen.alphaNumStr.map(Password(_))
    } yield LoginUser(u, p)
  }
}
