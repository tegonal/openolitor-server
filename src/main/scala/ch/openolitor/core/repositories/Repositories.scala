/*                                                                           *\
*    ____                   ____  ___ __                                      *
*   / __ \____  ___  ____  / __ \/ (_) /_____  _____                          *
*  / / / / __ \/ _ \/ __ \/ / / / / / __/ __ \/ ___/   OpenOlitor             *
* / /_/ / /_/ /  __/ / / / /_/ / / / /_/ /_/ / /       contributed by tegonal *
* \____/ .___/\___/_/ /_/\____/_/_/\__/\____/_/        http://openolitor.ch   *
*     /_/                                                                     *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by           *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package ch.openolitor.core.repositories

import ch.openolitor.core.models._

import java.util.UUID
import scalikejdbc._
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import ch.openolitor.core.EventStream
import scala.util._

case class ParameterBindMapping[A](cl: Class[A], binder: ParameterBinder[A])

trait BaseEntitySQLSyntaxSupport[E <: BaseEntity[_]] extends SQLSyntaxSupport[E] with LazyLogging {
  //override def columnNames 
  def apply(p: SyntaxProvider[E])(rs: WrappedResultSet): E = apply(p.resultName)(rs)

  def opt(e: SyntaxProvider[E])(rs: WrappedResultSet): Option[E] = try {
    rs.stringOpt(e.resultName.id).map(_ => apply(e)(rs))
  } catch {
    case e: IllegalArgumentException => None
  }

  def apply(rn: ResultName[E])(rs: WrappedResultSet): E

  /**
   * Declare parameter mappings for all parameters used on insert
   */
  def parameterMappings(entity: E): Seq[Any]

  /**
   * Declare update parameters for this entity used on update. Is by default an empty set
   */
  def updateParameters(entity: E): Seq[Tuple2[SQLSyntax, Any]] = Seq()
}

trait ParameterBinderMapping[A] {
  def bind(value: A): ParameterBinder[A]
}

trait SqlBinder[-T] extends (T => Any) {
}

trait DBMappings {
  import TypeBinder._

  def baseIdTypeBinder[T <: BaseId](implicit f: UUID => T): TypeBinder[T] = string.map(s => f(UUID.fromString(s)))
  def toStringSqlBinder[V] = new SqlBinder[V] { def apply(value: V): Any = value.toString }
  def seqSqlBinder[V](implicit binder: SqlBinder[V]) = new SqlBinder[Seq[V]] { def apply(values: Seq[V]): Any = values map (binder) mkString (",") }
  def setSqlBinder[V](implicit binder: SqlBinder[V]) = new SqlBinder[Set[V]] { def apply(values: Set[V]): Any = values map (binder) mkString (",") }
  def noConversionSqlBinder[V] = new SqlBinder[V] { def apply(value: V): Any = value }
  def optionSqlBinder[V](implicit binder: SqlBinder[V]) = new SqlBinder[Option[V]] { def apply(value: Option[V]): Any = value map (binder) }
  def baseIdSqlBinder[I <: BaseId] = new SqlBinder[I] { def apply(value: I): Any = value.id.toString }

  // Just for convenience so NoConversion does not escape the scope.
  private case object DefaultSqlConverter extends SqlBinder[Any] { def apply(value: Any): Any = value }
  private def defaultSqlConversion: SqlBinder[Any] = DefaultSqlConverter

  implicit val stringSqlBinder = noConversionSqlBinder[String]
  implicit val bigdecimalSqlBinder = noConversionSqlBinder[BigDecimal]
  implicit val booleanSqlBinder = noConversionSqlBinder[Boolean]
  implicit val intSqlBinder = noConversionSqlBinder[Int]
  implicit val floatSqlBinder = noConversionSqlBinder[Float]
  implicit val doubleSqlBinder = noConversionSqlBinder[Double]
  implicit val longSqlBinder = noConversionSqlBinder[Long]
  implicit val datetimeSqlBinder = noConversionSqlBinder[DateTime]
  implicit val optionStringSqlBinder = optionSqlBinder[String]
  implicit val optionDateTimeSqlBinder = optionSqlBinder[DateTime]
  implicit val optionIntSqlBinder = optionSqlBinder[Int]
  implicit val optionBigDecimalSqlBinder = optionSqlBinder[BigDecimal]

  def parameters[A](params: Tuple1[A])(
    implicit binder0: SqlBinder[A]) = {
    Tuple1(parameter(params._1)).productIterator.toSeq
  }

  def parameters[A, B](params: Tuple2[A, B])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B]) = {
    Tuple2(parameter(params._1),
      parameter(params._2)).productIterator.toSeq
  }

  def parameters[A, B, C](params: Tuple3[A, B, C])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C]) = {
    Tuple3(parameter(params._1),
      parameter(params._2),
      parameter(params._3)).productIterator.toSeq
  }

  def parameters[A, B, C, D](params: Tuple4[A, B, C, D])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D]) = {
    Tuple4(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E](params: Tuple5[A, B, C, D, E])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E]) = {
    Tuple5(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F](params: Tuple6[A, B, C, D, E, F])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F]) = {
    Tuple6(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G](params: Tuple7[A, B, C, D, E, F, G])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G]) = {
    Tuple7(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H](params: Tuple8[A, B, C, D, E, F, G, H])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H]) = {
    Tuple8(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H, I](params: Tuple9[A, B, C, D, E, F, G, H, I])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H],
    binder8: SqlBinder[I]) = {
    Tuple9(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H, I, J](params: Tuple10[A, B, C, D, E, F, G, H, I, J])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H],
    binder8: SqlBinder[I],
    binder9: SqlBinder[J]) = {
    Tuple10(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9),
      parameter(params._10)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K](params: Tuple11[A, B, C, D, E, F, G, H, I, J, K])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H],
    binder8: SqlBinder[I],
    binder9: SqlBinder[J],
    binder10: SqlBinder[K]) = {
    Tuple11(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9),
      parameter(params._10),
      parameter(params._11)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L](params: Tuple12[A, B, C, D, E, F, G, H, I, J, K, L])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H],
    binder8: SqlBinder[I],
    binder9: SqlBinder[J],
    binder10: SqlBinder[K],
    binder11: SqlBinder[L]) = {
    Tuple12(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9),
      parameter(params._10),
      parameter(params._11),
      parameter(params._12)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M](params: Tuple13[A, B, C, D, E, F, G, H, I, J, K, L, M])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H],
    binder8: SqlBinder[I],
    binder9: SqlBinder[J],
    binder10: SqlBinder[K],
    binder11: SqlBinder[L],
    binder12: SqlBinder[M]) = {
    Tuple13(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9),
      parameter(params._10),
      parameter(params._11),
      parameter(params._12),
      parameter(params._13)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N](params: Tuple14[A, B, C, D, E, F, G, H, I, J, K, L, M, N])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H],
    binder8: SqlBinder[I],
    binder9: SqlBinder[J],
    binder10: SqlBinder[K],
    binder11: SqlBinder[L],
    binder12: SqlBinder[M],
    binder13: SqlBinder[N]) = {
    Tuple14(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9),
      parameter(params._10),
      parameter(params._11),
      parameter(params._12),
      parameter(params._13),
      parameter(params._14)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O](params: Tuple15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H],
    binder8: SqlBinder[I],
    binder9: SqlBinder[J],
    binder10: SqlBinder[K],
    binder11: SqlBinder[L],
    binder12: SqlBinder[M],
    binder13: SqlBinder[N],
    binder14: SqlBinder[O]) = {
    Tuple15(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9),
      parameter(params._10),
      parameter(params._11),
      parameter(params._12),
      parameter(params._13),
      parameter(params._14),
      parameter(params._15)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P](params: Tuple16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H],
    binder8: SqlBinder[I],
    binder9: SqlBinder[J],
    binder10: SqlBinder[K],
    binder11: SqlBinder[L],
    binder12: SqlBinder[M],
    binder13: SqlBinder[N],
    binder14: SqlBinder[O],
    binder15: SqlBinder[P]) = {
    Tuple16(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9),
      parameter(params._10),
      parameter(params._11),
      parameter(params._12),
      parameter(params._13),
      parameter(params._14),
      parameter(params._15),
      parameter(params._16)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q](params: Tuple17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H],
    binder8: SqlBinder[I],
    binder9: SqlBinder[J],
    binder10: SqlBinder[K],
    binder11: SqlBinder[L],
    binder12: SqlBinder[M],
    binder13: SqlBinder[N],
    binder14: SqlBinder[O],
    binder15: SqlBinder[P],
    binder16: SqlBinder[Q]) = {
    Tuple17(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9),
      parameter(params._10),
      parameter(params._11),
      parameter(params._12),
      parameter(params._13),
      parameter(params._14),
      parameter(params._15),
      parameter(params._16),
      parameter(params._17)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R](params: Tuple18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H],
    binder8: SqlBinder[I],
    binder9: SqlBinder[J],
    binder10: SqlBinder[K],
    binder11: SqlBinder[L],
    binder12: SqlBinder[M],
    binder13: SqlBinder[N],
    binder14: SqlBinder[O],
    binder15: SqlBinder[P],
    binder16: SqlBinder[Q],
    binder17: SqlBinder[R]) = {
    Tuple18(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9),
      parameter(params._10),
      parameter(params._11),
      parameter(params._12),
      parameter(params._13),
      parameter(params._14),
      parameter(params._15),
      parameter(params._16),
      parameter(params._17),
      parameter(params._18)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S](params: Tuple19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H],
    binder8: SqlBinder[I],
    binder9: SqlBinder[J],
    binder10: SqlBinder[K],
    binder11: SqlBinder[L],
    binder12: SqlBinder[M],
    binder13: SqlBinder[N],
    binder14: SqlBinder[O],
    binder15: SqlBinder[P],
    binder16: SqlBinder[Q],
    binder17: SqlBinder[R],
    binder18: SqlBinder[S]) = {
    Tuple19(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9),
      parameter(params._10),
      parameter(params._11),
      parameter(params._12),
      parameter(params._13),
      parameter(params._14),
      parameter(params._15),
      parameter(params._16),
      parameter(params._17),
      parameter(params._18),
      parameter(params._19)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T](params: Tuple20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H],
    binder8: SqlBinder[I],
    binder9: SqlBinder[J],
    binder10: SqlBinder[K],
    binder11: SqlBinder[L],
    binder12: SqlBinder[M],
    binder13: SqlBinder[N],
    binder14: SqlBinder[O],
    binder15: SqlBinder[P],
    binder16: SqlBinder[Q],
    binder17: SqlBinder[R],
    binder18: SqlBinder[S],
    binder19: SqlBinder[T]) = {
    Tuple20(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9),
      parameter(params._10),
      parameter(params._11),
      parameter(params._12),
      parameter(params._13),
      parameter(params._14),
      parameter(params._15),
      parameter(params._16),
      parameter(params._17),
      parameter(params._18),
      parameter(params._19),
      parameter(params._20)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U](params: Tuple21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H],
    binder8: SqlBinder[I],
    binder9: SqlBinder[J],
    binder10: SqlBinder[K],
    binder11: SqlBinder[L],
    binder12: SqlBinder[M],
    binder13: SqlBinder[N],
    binder14: SqlBinder[O],
    binder15: SqlBinder[P],
    binder16: SqlBinder[Q],
    binder17: SqlBinder[R],
    binder18: SqlBinder[S],
    binder19: SqlBinder[T],
    binder20: SqlBinder[U]) = {
    Tuple21(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9),
      parameter(params._10),
      parameter(params._11),
      parameter(params._12),
      parameter(params._13),
      parameter(params._14),
      parameter(params._15),
      parameter(params._16),
      parameter(params._17),
      parameter(params._18),
      parameter(params._19),
      parameter(params._20),
      parameter(params._21)).productIterator.toSeq
  }

  def parameters[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V](params: Tuple22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V])(
    implicit binder0: SqlBinder[A],
    binder1: SqlBinder[B],
    binder2: SqlBinder[C],
    binder3: SqlBinder[D],
    binder4: SqlBinder[E],
    binder5: SqlBinder[F],
    binder6: SqlBinder[G],
    binder7: SqlBinder[H],
    binder8: SqlBinder[I],
    binder9: SqlBinder[J],
    binder10: SqlBinder[K],
    binder11: SqlBinder[L],
    binder12: SqlBinder[M],
    binder13: SqlBinder[N],
    binder14: SqlBinder[O],
    binder15: SqlBinder[P],
    binder16: SqlBinder[Q],
    binder17: SqlBinder[R],
    binder18: SqlBinder[S],
    binder19: SqlBinder[T],
    binder20: SqlBinder[U],
    binder21: SqlBinder[V]) = {
    Tuple22(parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9),
      parameter(params._10),
      parameter(params._11),
      parameter(params._12),
      parameter(params._13),
      parameter(params._14),
      parameter(params._15),
      parameter(params._16),
      parameter(params._17),
      parameter(params._18),
      parameter(params._19),
      parameter(params._20),
      parameter(params._21),
      parameter(params._22)).productIterator.toSeq
  }

  def parameter[V](value: V)(implicit binder: SqlBinder[V] = defaultSqlConversion): Any = binder.apply(value)
}

object BaseRepository extends LazyLogging {
}

trait BaseWriteRepository extends DBMappings with LazyLogging with EventStream {

  type Validator[E] = E => Boolean
  val TrueValidator: Validator[Any] = x => true

  def getById[E <: BaseEntity[I], I <: BaseId](syntax: BaseEntitySQLSyntaxSupport[E], id: I)(implicit session: DBSession,
    binder: SqlBinder[I]): Option[E] = {
    val alias = syntax.syntax("x")
    val idx = alias.id
    withSQL {
      select
        .from(syntax as alias)
        .where.eq(alias.id, parameter(id))
    }.map(syntax.apply(alias)).single.apply()
  }

  def insertEntity[E <: BaseEntity[I], I <: BaseId](entity: E)(implicit session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: SqlBinder[I],
    user: UserId) = {
    val params = syntaxSupport.parameterMappings(entity)
    logger.debug(s"create entity with values:$entity")
    getById(syntaxSupport, entity.id) match {
      case Some(x) =>
        logger.debug(s"Ignore insert event, entity already exists:${entity.id}")
      case None => 
        withSQL(insertInto(syntaxSupport).values(params: _*)).update.apply()
        
        //publish event to stream
        publish(EntityCreated(user, entity))
    }
  }
  def updateEntity[E <: BaseEntity[I], I <: BaseId](entity: E)(implicit session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: SqlBinder[I],
    user: UserId) = {
    getById(syntaxSupport, entity.id).map { orig =>
      val alias = syntaxSupport.syntax("x")
      val id = alias.id
      val updateParams = syntaxSupport.updateParameters(entity)
      withSQL(update(syntaxSupport as alias).set(updateParams: _*).where.eq(id, parameter(entity.id))).update.apply()

      //publish event to stream
      publish(EntityModified(user, entity, orig))
    }
  }

  def deleteEntity[E <: BaseEntity[I], I <: BaseId](id: I, validator: Validator[E])(implicit session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: SqlBinder[I],
    user: UserId): Option[E] = {
    deleteEntity[E, I](id, Some(validator))
  }

  def deleteEntity[E <: BaseEntity[I], I <: BaseId](id: I, validator: Option[Validator[E]] = None)(implicit session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: SqlBinder[I],
    user: UserId): Option[E] = {
    logger.debug(s"delete from ${syntaxSupport.tableName}: $id")
    getById(syntaxSupport, id).map { entity =>
      val validation = validator.getOrElse(TrueValidator)
      validation(entity) match {
        case true =>
          withSQL(deleteFrom(syntaxSupport).where.eq(syntaxSupport.column.id, parameter(id))).update.apply()

          //publish event to stream
          publish(EntityDeleted(user, entity))
          Some(entity)
        case false =>
          logger.debug(s"Couldn't delete from ${syntaxSupport.tableName}: $id, validation didn't succeed")
          None
      }
    }.getOrElse(None)
  }
}
