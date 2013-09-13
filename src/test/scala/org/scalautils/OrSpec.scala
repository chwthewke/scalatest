/*
 * Copyright 2001-2013 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalautils

import java.text._
import org.scalatest._
import scala.util.Try
import scala.util.Success
import scala.util.Failure

class OrSpec extends UnitSpec with Validations with TypeCheckedTripleEquals {
  "An Or" can "be either Good or Bad" in {
    Good(7) shouldBe 'good
    Bad("oops") shouldBe 'bad

    Good(7) shouldBe an [Or[_, _]]
    Good(7) shouldBe an [Good[_, _]]

    Bad("oops") shouldBe an [Or[_, _]]
    Bad("oops") shouldBe an [Bad[_, _]]
  }
  it can "have its non-inferred type widened by an apply call with a type param" in {
    /*
      scala> Good[Int].orBad("hi")
      res0: org.scalautils.Bad[Int,String] = Bad(hi)

      scala> Good(3).orBad[String]
      res1: org.scalautils.Good[Int,String] = Good(3)

      scala> Good(3).orBad[ErrorMessage]
      res2: org.scalautils.Good[Int,org.scalautils.ErrorMessage] = Good(3)

      scala> Good(3).orBad("oops")
      <console>:11: error: type mismatch;
       found   : String("oops")
       required: <:<[Nothing,?]
                    Good(3).orBad("oops")
                                  ^

      scala> Good[Int].orBad[String]
      <console>:11: error: missing arguments for method orBad in class GoodieGoodieGumdrop;
      follow this method with `_' if you want to treat it as a partially applied function
                    Good[Int].orBad[String]
                                   ^
    */
    // If the expected type is known, then you can just say Good or Bad:
    Good(3) shouldBe Good(3)
    Bad("oops") shouldBe Bad("oops")

    // But if the expected type is not known, the inferred type of the other side will be Nothing:
    // Good(3) will be a Good[Int, Nothing]
    // Bad("oops") will be a Bad[Nothing, String]

    // If you want to specify a more specific type than Nothing, you can use this syntax:
    Good(3).orBad[String] shouldBe Good(3)
    Good[Int].orBad("oops") shouldBe Bad("oops")

    // You could also do it this way:
    Good[Int, String](3) shouldBe Good(3)
    Bad[Int, String]("oops") shouldBe Bad("oops")

    // But that requires that you also give a type that would be inferred from the value. This
    // would only be necessary if you wanted a more general type than that which
    // would otherwise be inferred from the given value, such as:
    Good[AnyVal, String](3) shouldBe Good(3)
    Bad[Int, AnyRef]("oops") shouldBe Bad("oops")

    // In that case, though, I recommend a type ascription, because I think it is easier to read:
    (Good(3): AnyVal Or String) shouldBe Good(3)
    (Bad("oops"): Int Or AnyRef) shouldBe Bad("oops")
  }
  it can "be used in infix notation" in {
    def div(a: Int, b: Int): Int Or ArithmeticException = {
      try Good(a / b)
      catch { case ae: ArithmeticException => Bad(ae) }
    }
    div(1, 1) shouldEqual Good(1)
    div(6, 2) shouldEqual Good(3)
    div(6, 2) shouldEqual Good(3)
    div(1, 0) should be a 'bad
    val ae = div(1, 0) match {
      case Bad(ae) => ae
      case result => fail("didn't get an Bad" + result)
    }
    ae should have message "/ by zero"
  }
  it can "be used with map" in {
    Good(8) map (_ + 1) should equal (Good(9))
    Good[Int].orBad("eight") map (_ + 1) should equal (Bad("eight"))
  }
  it can "be used with foreach" in {
    var vCount = 0
    var eCount = 0
    Good(8) foreach { vCount += _ }
    vCount should equal (8)
    Good[Int].orBad("eight") foreach { eCount += _ }
    eCount should equal (0)
  }
  it can "be used with flatMap" in {
    Good(8).orBad[String] flatMap ((x: Int) => Good(x + 1)) should equal (Good(9))
    Good[Int].orBad("eight") flatMap ((x: Int) => Good(x + 1)) should equal (Bad("eight"))
  }
  it can "be used with filter" in {
    Good(12).filter(_ > 10) should be (Some(Good(12)))
    Good(7).filter(_ > 10) should be (None)
    Good[Int].orBad(12).filter(_ > 10) should be (None)
  }
  it can "be used with exists" in {
    Good(12).exists(_ == 12) shouldBe true
    Good(12).exists(_ == 13) shouldBe false
    Good[Int].orBad(12).exists(_ == 12) shouldBe false
  }
  it can "be used with forall" in {
    Good(12).forall(_ > 10) shouldBe true
    Good(7).forall(_ > 10) shouldBe false
    Good[Int].orBad(12).forall(_ > 10) shouldBe true
    Good[Int].orBad(7).forall(_ > 10) shouldBe true
  }
  it can "be used with getOrElse" in {
    Good(12).getOrElse(17) shouldBe 12
    Good[Int].orBad(12).getOrElse(17) shouldBe 17
  }
  it can "be used with toOption" in {
    Good(12).toOption shouldBe Some(12)
    Good[Int].orBad(12).toOption shouldBe None
  }
  it can "be used with toSeq" in {
    Good(12).toSeq shouldEqual Seq(12)
    Good[Int].orBad(12).toSeq shouldEqual Seq.empty
  }
// toArray, toBuffer, toIndexedSeq, toIterable, toIterator, toList, 
// toSeq, toStream, toTraversable, toVector
  it can "be used with toEither" in {
    Good(12).toEither shouldBe Right(12)
    Bad(12).toEither shouldBe Left(12)
  }
  it can "be used with accumulating" in {
    Good(12).orBad[Int].accumulating shouldBe Good(12).orBad[Every[Int]]
    Good[Int].orBad(12).accumulating shouldBe Good[Int].orBad(One(12))
  }
  it can "be used with toTry, if the error type is a subtype of Throwable" in {
    Good(12).orBad[Throwable].toTry shouldBe Success(12)
    Good(12).orBad[RuntimeException].toTry shouldBe Success(12)
    val ex = new RuntimeException("oops")
    Good[Int].orBad(ex).toTry shouldBe Failure(ex)
    Good[Int].orBad(ex).toTry shouldBe Failure(ex)
    // Does not compile: Good[Int, Int](12).toTry shouldBe Success(12)
  }
  it can "be used with swap" in {
    Good(12).orBad[String].swap should === (Good[String].orBad(12))
    Good[Int].orBad("hi").swap should === (Good("hi").orBad[Int])
  }
  it can "be used with zip" in {
    Good(12).orBad[Every[ErrorMessage]] zip Good("hi").orBad[Every[ErrorMessage]] should === (Good((12, "hi")).orBad[Every[ErrorMessage]])
    Good[Int].orBad(One("so")) zip Good[String].orBad(One("ho")) should === (Bad(Many("so", "ho")))
    (Good(12): Int Or Every[ErrorMessage]) zip Bad[String, Every[ErrorMessage]](One("ho")) should === (Bad(One("ho")))
    Bad[Int, Every[ErrorMessage]](One("so")) zip Good[String, Every[ErrorMessage]]("hi") should === (Bad(One("so")))

    Good[Int, One[ErrorMessage]](12) zip Good[String, Every[ErrorMessage]]("hi") should === (Good[(Int, String), Every[ErrorMessage]]((12, "hi")))
    Bad[Int, One[ErrorMessage]](One("so")) zip Bad[String, Every[ErrorMessage]](One("ho")) should === (Bad(Many("so", "ho")))
    Good[Int, One[ErrorMessage]](12) zip Bad[String, Every[ErrorMessage]](One("ho")) should === (Bad(One("ho")))
    Bad[Int, One[ErrorMessage]](One("so")) zip Good[String, Every[ErrorMessage]]("hi") should === (Bad(One("so")))

    Good[Int, Every[ErrorMessage]](12) zip Good[String, One[ErrorMessage]]("hi") should === (Good[(Int, String), Every[ErrorMessage]]((12, "hi")))
    Bad[Int, Every[ErrorMessage]](One("so")) zip Bad[String, One[ErrorMessage]](One("ho")) should === (Bad(Many("so", "ho")))
    Good[Int, Every[ErrorMessage]](12) zip Bad[String, One[ErrorMessage]](One("ho")) should === (Bad(One("ho")))
    Bad[Int, Every[ErrorMessage]](One("so")) zip Good[String, One[ErrorMessage]]("hi") should === (Bad(One("so")))

    // Works when right hand side ERR type is a supertype of left hand side ERR type, because that's what Every's ++ does.
    Good[Int].orBad(One("oops")) zip Good[Int].orBad(One(-1: Any)) shouldBe Bad(Many("oops", -1))
    // This one doesn't work on purpose, though could zip it hte other way:
    // Good[Int].orBad(One("oops": Any)) zip Good[Int].orBad(One(-1)) shouldBe Bad(Many("oops", -1))
    Good[Int].orBad(One(-1)) zip Good[Int].orBad(One("oops": Any)) shouldBe Bad(Many(-1, "oops"))
  }
  it can "be used with transform" in {
    Good[Int, Every[ErrorMessage]](2) transform Good[Int => String, Every[ErrorMessage]]("hi" * _) should === (Good[String, Every[ErrorMessage]]("hihi"))
    Bad[Int, Every[ErrorMessage]](One("so")) transform Bad[Int => String, Every[ErrorMessage]](One("ho")) should === (Bad(Many("so", "ho")))
    Good[Int, Every[ErrorMessage]](2) zip Bad[Int => String, Every[ErrorMessage]](One("ho")) should === (Bad(One("ho")))
    Bad[Int, Every[ErrorMessage]](One("so")) transform Good[Int => String, Every[ErrorMessage]]("hi" * _) should === (Bad(One("so")))

    Good[Int, One[ErrorMessage]](2) transform Good[Int => String, Every[ErrorMessage]]("hi" * _) should === (Good[String, Every[ErrorMessage]]("hihi"))
    Bad[Int, One[ErrorMessage]](One("so")) transform Bad[Int => String, Every[ErrorMessage]](One("ho")) should === (Bad(Many("so", "ho")))
    Good[Int, One[ErrorMessage]](2) transform Bad[Int => String, Every[ErrorMessage]](One("ho")) should === (Bad(One("ho")))
    Bad[Int, One[ErrorMessage]](One("so")) transform Good[Int => String, Every[ErrorMessage]]("hi" * _) should === (Bad(One("so")))

    Good[Int, Every[ErrorMessage]](2) transform Good[Int => String, One[ErrorMessage]]("hi" * _) should === (Good[String, Every[ErrorMessage]]("hihi"))
    Bad[Int, Every[ErrorMessage]](One("so")) transform Bad[Int => String, One[ErrorMessage]](One("ho")) should === (Bad(Many("so", "ho")))
    Good[Int, Every[ErrorMessage]](2) transform Bad[Int => String, One[ErrorMessage]](One("ho")) should === (Bad(One("ho")))
    Bad[Int, Every[ErrorMessage]](One("so")) transform Good[Int => String, One[ErrorMessage]]("hi" * _) should === (Bad(One("so")))
  }
  it can "be used with validate" in {
    Good[Int, Every[ErrorMessage]](12).validate(
      (i: Int) => if (i > 0) None else Some(i + " was not greater than 0"),
      (i: Int) => if (i < 100) None else Some(i + " was not less than 100"),
      (i: Int) => if (i % 2 == 0) None else Some(i + " was not even")
    ) shouldBe Good(12)
    Good[Int, Every[ErrorMessage]](12).validate(
      (i: Int) => if (i > 0) None else Some(i + " was not greater than 0"),
      (i: Int) => if (i < 3) None else Some(i + " was not less than 3"),
      (i: Int) => if (i % 2 == 0) None else Some(i + " was not even")
    ) shouldBe Bad(One("12 was not less than 3"))
    Good[Int, Every[ErrorMessage]](12).validate(
      (i: Int) => if (i > 0) None else Some(i + " was not greater than 0"),
      (i: Int) => if (i < 3) None else Some(i + " was not less than 3"),
      (i: Int) => if (i % 2 == 1) None else Some(i + " was not odd")
    ) shouldBe Bad(Many("12 was not less than 3", "12 was not odd"))
    Good[Int, Every[ErrorMessage]](12).validate(
      (i: Int) => if (i > 99) None else Some(i + " was not greater than 99"),
      (i: Int) => if (i < 3) None else Some(i + " was not less than 3"),
      (i: Int) => if (i % 2 == 1) None else Some(i + " was not odd")
    ) shouldBe Bad(Many("12 was not greater than 99", "12 was not less than 3", "12 was not odd"))
    Bad[Int, Every[ErrorMessage]](One("original error")).validate(
      (i: Int) => if (i > 0) None else Some(i + " was not greater than 0"),
      (i: Int) => if (i < 3) None else Some(i + " was not less than 3"),
      (i: Int) => if (i % 2 == 0) None else Some(i + " was not even")
    ) shouldBe Bad(One("original error"))
    Bad[Int, Every[ErrorMessage]](Many("original error 1", "original error 2")).validate(
      (i: Int) => if (i > 0) None else Some(i + " was not greater than 0"),
      (i: Int) => if (i < 3) None else Some(i + " was not less than 3"),
      (i: Int) => if (i % 2 == 0) None else Some(i + " was not even")
    ) shouldBe Bad(Many("original error 1", "original error 2"))
  }
  it can "be created with the attempt helper method" in {
    attempt { 2 / 1 } should === (Good(2))
    val divByZero = attempt { 1 / 0 }
    divByZero should be a 'bad
    divByZero match {
      case Bad(ex) =>
        ex shouldBe an [ArithmeticException]
        ex.getMessage shouldBe "/ by zero"
      case _ => fail()
    }
    divByZero should be a 'bad
    intercept[NotImplementedError] {
      attempt { ??? }
    }
  }
  it can "be created from a Try via the from factory method" in {
    Or.from(Success(12)) shouldBe Good(12)
    val ex = new Exception("oops")
    Or.from(Failure(ex)) shouldBe Bad(ex)
  }
  it can "be created with the fromEither factory method" in {
    Or.from(Right(12)) shouldBe Good(12)
    val ex = new Exception("oops")
    Or.from(Left(ex)) shouldBe Bad(ex)
    Or.from(Left("oops")) shouldBe Bad("oops")
  }

  it can "be validated with collection.validatedBy" in {

    def isOdd(i: Int): Int Or One[ErrorMessage] =
      if (i % 2 == 1) Good(i) else Bad(One(s"$i was not odd"))

    // List
    List.empty[Int].validatedBy(isOdd) shouldBe Good(List.empty[Int])

    List(3).validatedBy(isOdd) shouldBe Good(List(3))
    List(4).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))

    List(3, 5).validatedBy(isOdd) shouldBe Good(List(3, 5))
    List(4, 6).validatedBy(isOdd) shouldBe Bad(Every("4 was not odd", "6 was not odd"))
    List(3, 4).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))
    List(4, 3).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))

    List(3, 5, 7).validatedBy(isOdd) shouldBe Good(List(3, 5, 7))

    // Vector
    Vector.empty[Int].validatedBy(isOdd) shouldBe Good(Vector.empty[Int])

    Vector(3).validatedBy(isOdd) shouldBe Good(Vector(3))
    Vector(4).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))

    Vector(3, 5).validatedBy(isOdd) shouldBe Good(Vector(3, 5))
    Vector(4, 6).validatedBy(isOdd) shouldBe Bad(Every("4 was not odd", "6 was not odd"))
    Vector(3, 4).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))
    Vector(4, 3).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))

    Vector(3, 5, 7).validatedBy(isOdd) shouldBe Good(Vector(3, 5, 7))

    // Iterator
    List.empty[Int].iterator.validatedBy(isOdd).map(_.toStream) shouldBe Good(List.empty[Int].iterator).map(_.toStream)

    List(3).iterator.validatedBy(isOdd).map(_.toStream) shouldBe Good(List(3).iterator).map(_.toStream)
    List(4).iterator.validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))

    List(3, 5).iterator.validatedBy(isOdd).map(_.toStream) shouldBe Good(List(3, 5).iterator).map(_.toStream)
    List(4, 6).iterator.validatedBy(isOdd) shouldBe Bad(Every("4 was not odd", "6 was not odd"))
    List(3, 4).iterator.validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))
    List(4, 3).iterator.validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))

    List(3, 5, 7).iterator.validatedBy(isOdd).map(_.toStream) shouldBe Good(List(3, 5, 7).iterator).map(_.toStream)

    // Set
    Set.empty[Int].validatedBy(isOdd) shouldBe Good(Set.empty[Int])

    Set(3).validatedBy(isOdd) shouldBe Good(Set(3))
    Set(4).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))

    Set(3, 5).validatedBy(isOdd) shouldBe Good(Set(3, 5))
    Set(4, 6).validatedBy(isOdd) shouldBe Bad(Every("4 was not odd", "6 was not odd"))
    Set(3, 4).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))
    Set(4, 3).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))

    Set(3, 5, 7).validatedBy(isOdd) shouldBe Good(Set(3, 5, 7))

    Set.empty[Int].validatedBy(isOdd) shouldBe Good(Set.empty[Int])

    // Every
    One(3).validatedBy(isOdd) shouldBe Good(One(3))
    Every(3).validatedBy(isOdd) shouldBe Good(One(3))
    One(4).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))
    Every(4).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))

    Many(3, 5).validatedBy(isOdd) shouldBe Good(Many(3, 5))
    Every(3, 5).validatedBy(isOdd) shouldBe Good(Many(3, 5))
    Many(4, 6).validatedBy(isOdd) shouldBe Bad(Every("4 was not odd", "6 was not odd"))
    Every(4, 6).validatedBy(isOdd) shouldBe Bad(Every("4 was not odd", "6 was not odd"))
    Many(3, 4).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))
    Every(3, 4).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))
    Many(4, 3).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))
    Every(4, 3).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))

    Many(3, 5, 7).validatedBy(isOdd) shouldBe Good(Every(3, 5, 7))
    Every(3, 5, 7).validatedBy(isOdd) shouldBe Good(Many(3, 5, 7))

    // Option
    Some(3).validatedBy(isOdd) shouldBe Good(Some(3))
    (None: Option[Int]).validatedBy(isOdd) shouldBe Good(None)
    Some(4).validatedBy(isOdd) shouldBe Bad(One("4 was not odd"))
  }

  it can "be validated with collection.validatedBy when the map goes to a different type" in {
    def parseAge(input: String): Int Or One[ErrorMessage] = {
      try {
        val age = input.trim.toInt
        if (age >= 0) Good(age) else Bad(One(s""""${age}" is not a valid age"""))
      }
      catch {
        case _: NumberFormatException => Bad(One(s""""${input}" is not a valid integer"""))
      }
    }

    Some("29").validatedBy(parseAge) shouldBe Good(Some(29))
    Some("-30").validatedBy(parseAge) shouldBe Bad(One("\"-30\" is not a valid age"))

    Every("29", "30", "31").validatedBy(parseAge) shouldBe Good(Many(29, 30, 31))
    Every("29", "-30", "31").validatedBy(parseAge) shouldBe Bad(One("\"-30\" is not a valid age"))
    Every("29", "-30", "-31").validatedBy(parseAge) shouldBe Bad(Many("\"-30\" is not a valid age", "\"-31\" is not a valid age"))

    List("29", "30", "31").validatedBy(parseAge) shouldBe Good(List(29, 30, 31))
    List("29", "-30", "31").validatedBy(parseAge) shouldBe Bad(One("\"-30\" is not a valid age"))
    List("29", "-30", "-31").validatedBy(parseAge) shouldBe Bad(Many("\"-30\" is not a valid age", "\"-31\" is not a valid age"))
  }

  it can "be combined with collection.combined" in {

    // List
    // Is this the right answer? Has to be, because couldn't come up with an error anyway.
    List.empty[Int Or Every[String]].combined shouldBe Good(List.empty[Int])

    //  def combine[G, ELE, EVERY[b] <: Every[b], SEQ[s]](xs: SEQ[G Or EVERY[ELE]])(implicit seq: Sequenceable[SEQ]): SEQ[G] Or Every[ELE] =
    // G = Int, ELE = Nothing, SEQ = List
    List(Good(3)).combined shouldBe Good(List(3))
    List(Bad(One("oops"))).combined shouldBe Bad(One("oops"))

    List(Good(3), Good(4)).combined shouldBe Good(List(3, 4))
    List(Bad(One("darn")), Bad(One("oops"))).combined shouldBe Bad(Every("darn", "oops"))
    List(Good(3), Bad(One("oops"))).combined shouldBe Bad(One("oops"))
    List(Bad(One("oops")), Good(3)).combined shouldBe Bad(One("oops"))

    List(Good(3), Good(4), Good(5)).combined shouldBe Good(List(3, 4, 5))

    // Vector
    Vector.empty[Int Or Every[String]].combined shouldBe Good(Vector.empty[Int])

    Vector(Good(3)).combined shouldBe Good(Vector(3))
    Vector(Bad(One("oops"))).combined shouldBe Bad(One("oops"))

    Vector(Good(3), Good(4)).combined shouldBe Good(Vector(3, 4))
    Vector(Bad(One("darn")), Bad(One("oops"))).combined shouldBe Bad(Every("darn", "oops"))
    Vector(Good(3), Bad(One("oops"))).combined shouldBe Bad(One("oops"))
    Vector(Bad(One("oops")), Good(3)).combined shouldBe Bad(One("oops"))

    Vector(Good(3), Good(4), Good(5)).combined shouldBe Good(Vector(3, 4, 5))

    // Do the same thing with Iterator
    (List.empty[Int Or Every[String]].iterator).combined.map(_.toStream) shouldEqual (Good(List.empty[Int].iterator).map(_.toStream))

    List(Good(3)).iterator.combined.map(_.toStream) shouldEqual (Good(List(3).iterator).map(_.toStream))
    List(Bad(One("oops"))).iterator.combined shouldEqual (Bad(One("oops")))

    List(Good(3), Good(4)).iterator.combined.map(_.toStream) shouldEqual (Good(List(3, 4).iterator).map(_.toStream))
    List(Bad(One("darn")), Bad(One("oops"))).iterator.combined shouldEqual (Bad(Every("darn", "oops")))
    List(Good(3), Bad(One("oops"))).iterator.combined shouldEqual (Bad(One("oops")))
    List(Bad(One("oops")), Good(3)).iterator.combined shouldEqual (Bad(One("oops")))

    List(Good(3), Good(4), Good(5)).iterator.combined.map(_.toStream) shouldEqual (Good(List(3, 4, 5).iterator).map(_.toStream))

    // Set
    Set.empty[Int Or Every[String]].combined shouldBe Good(Set.empty[Int])
    Set(Good[Int, Every[String]](3), Bad[Int, Every[String]](Every("oops"))).asInstanceOf[Set[Int Or Every[String]]].combined shouldBe Bad(One("oops"))
    Set(Good[Int, Every[String]](3), Bad[Int, Every[String]](Every("oops"))).combined shouldBe Bad(One("oops"))

    Set(Good(3)).combined shouldBe Good(Set(3))
    Set(Bad(One("oops"))).combined shouldBe Bad(One("oops"))

    Set(Good(3), Good(4)).combined shouldBe Good(Set(3, 4))
    Set(Bad(One("darn")), Bad(One("oops"))).combined shouldBe Bad(Every("darn", "oops"))
    Set(Good(3), Bad(One("oops"))).combined shouldBe Bad(One("oops"))
    Set(Bad(One("oops")), Good(3)).combined shouldBe Bad(One("oops"))

    Set(Good(3), Good(4), Good(5)).combined shouldBe Good(Set(3, 4, 5))

    // Every
    Every(Good(3).orBad[Every[String]], Good[Int].orBad(Every("oops"))).combined shouldBe Bad(One("oops"))

    Every(Good(3)).combined shouldBe Good(Every(3))
    One(Good(3)).combined shouldBe Good(Every(3))
    Every(Bad(One("oops"))).combined shouldBe Bad(One("oops"))
    One(Bad(One("oops"))).combined shouldBe Bad(One("oops"))

    Every(Good(3), Good(4)).combined shouldBe Good(Every(3, 4))
    Many(Good(3), Good(4)).combined shouldBe Good(Every(3, 4))
    Every(Bad(One("darn")), Bad(One("oops"))).combined shouldBe Bad(Every("darn", "oops"))
    Many(Bad(One("darn")), Bad(One("oops"))).combined shouldBe Bad(Every("darn", "oops"))
    Every(Good(3), Bad(One("oops"))).combined shouldBe Bad(One("oops"))
    Every(Bad(One("oops")), Good(3)).combined shouldBe Bad(One("oops"))

    Every(Good(3), Good(4), Good(5)).combined shouldBe Good(Every(3, 4, 5))

    // Option
    Some(Good(3)).combined shouldBe Good(Some(3))
    (None: Option[Int Or Every[ErrorMessage]]).combined shouldBe Good(None)
    Some(Bad(One("oops"))).combined shouldBe Bad(One("oops"))
    Some(Bad(Many("oops", "idoops"))).combined shouldBe Bad(Many("oops", "idoops"))
  }
}


