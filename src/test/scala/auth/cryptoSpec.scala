package com.login.auth

import cats.effect.IO
import com.login.Password
import io.chrisdavenport.fuuid.FUUID
import org.scalatest._

class CryptoSpec extends FlatSpec with Matchers {
  val crypto = BCrypto.build[IO]

  "Crypto" should "successfully hash and verify a password" in {
    val password = Password(FUUID.randomFUUID[IO].unsafeRunSync().show)

    val hashPw = crypto.getHashedPassword(password).unsafeRunSync()
    val check = crypto.checkHashedPassword(password, hashPw).unsafeRunSync()

    check should be(true)
  }

}
