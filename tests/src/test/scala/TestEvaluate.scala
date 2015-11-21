package scala.meta

import org.scalatest.FunSuite

import tql._
import interpreter._
import internal.interpreter._
import internal.interpreter.Interpreter._

import internal.representations._

/**
 * Created by rutz on 06/10/15.
 */

class TestEvaluate extends FunSuite {
  val scalaLibrary = sys.props("sbt.paths.scalalibrary.classes")
  // val classpath = sys.props("sbt.paths.scrutinee.classes")
  // val sourcepath = sys.props("sbt.paths.scrutinee.sources")

  implicit val c: Context = Context(Artifact(scalaLibrary))

  test("literal") {
     assert(eval(q"""{ def x = 2; x }""") match {
        case Literal(2) => true
        case _ => false
     })
  }

  test("simple main with args") {
    val stat: Stat = """
            |object Test {
            |  def main(args: Array[String]): Unit = {
            |    val x = args.length
            |    args update (0, "Hello")
            |    val y = x * x
            |    println(y)
            |    println(args.length)
            |    println(args.apply(0) + x)
            |  }
            |}
        """.stripMargin.parse[Stat]

    val env = Env("args" -> Array[String]("test", "if", "it", "works", "for", "6"))
    assert(evalMain(stat, env) match {
        case Literal(()) => true
        case _ => false
    })
  }

  test("defining functions") {
    eval("""
        |{
        |   def loop(x: Int): Int = {
        |       def helper(x: Int) = true
        |       if (helper(x)) 0
        |       else loop(x - 1)
        |   }
        |   loop(42)
        |}
        """.stripMargin.parse[Term])
  }

  test("compiled method calls") {
    evalMain("""
        |object Test {
        |   def main(args: Array[String]): Unit = {
        |       val x = 2
        |       println(x.toString)
        |       println(x.toDouble)
        |       println(x + x)
        |   }
        |}
        """.stripMargin.parse[Stat])
  }
/*
  test("patterns in declarations") {
    eval("""
        |object Test {
        |   def main(args: Array[String]): Unit = {
        |       val (x, y) = (2, 3)
        |       println(x.toString)
        |       println(y.toDouble)
        |       println(x + y)
        |   }
        |}
        """.stripMargin.parse[Stat])
  }

  test("more complex patterns in declarations") {
    eval("""
        |object Test {
        |   def main(args: Array[String]): Unit = {
        |       val ((List(x), y), z) = ((List(2), 3), 4)
        |       println(x.toString)
        |       println(y.toDouble)
        |       println(x + y + z)
        |   }
        |}
        """.stripMargin.parse[Stat])
  }
  */

  def evalMain(stat0: Stat, env: EnvImpl = Env())(implicit ctx: Context): Value = {
    val stat = ctx.typecheck(stat0).asInstanceOf[Stat]
    val internalEnv = Environment(env, stat)(ctx)
    // println(internalEnv)

    stat match {
      // Stat can be either a block (multiple statements like class/object def and imports etc...)
      // Or it is an object containing the main function

      case q"object $name extends $template" =>
        val template"{ ..$_} with ..$_ { $_ => ..$stats1 }" = template

        val completeEnv: Environment = stats1.foldLeft(internalEnv) {
          case (tEnv, q"..$mods def main(${argsName: Term.Name}: Array[String]): Unit = ${expr: Term}") =>
            val args: Literal = tEnv.get.getOrElse(Local(argsName), Literal(Array[String]())).asInstanceOf[Literal]
            tEnv + (MainFun, Main(Literal(args), expr)) + (Local(argsName), args)
          case (tEnv, q"..$mods def $name[..$tparams](..$params): $tpeopt = $expr") => 
            tEnv + (Local(name), Function(name, params, expr))
        }

        val Main(arguments, term) = completeEnv(MainFun)
        evaluate(term, completeEnv)(ctx)
        Literal(())

      case t: Term => evaluate(ctx.typecheck(t).asInstanceOf[Term].desugar(ctx), internalEnv)(ctx)._1
    }
  }
}
