package hoecoga.core

import play.api.libs.functional.syntax._
import play.api.libs.json._

trait JsonUtil {
  def format[A, B](ap: String, apply: A => B, unapply: B => Option[A])(implicit fa: Format[A]): Format[B] =
    (JsPath \ ap).format[A].inmap(apply, Function.unlift(unapply))

  def format[A, B, C](ap: String, bp: String, apply: (A, B) => C, unapply: C => Option[(A, B)])(implicit fa: Format[A], fb: Format[B]): Format[C] = {
    ((JsPath \ ap).format[A] and (JsPath \ bp).format[B])(apply, Function.unlift(unapply))
  }

  def format[A, B, C, D](ap: String, bp: String, cp: String, apply: (A, B, C) => D, unapply: D => Option[(A, B, C)])(implicit fa: Format[A], fb: Format[B], fc: Format[C]): Format[D] = {
    ((JsPath \ ap).format[A] and (JsPath \ bp).format[B] and (JsPath \ cp).format[C])(apply, Function.unlift(unapply))
  }

  def format[A, B, C, D, E](ap: String, bp: String, cp: String, dp: String, apply: (A, B, C, D) => E, unapply: E => Option[(A, B, C, D)])(implicit fa: Format[A], fb: Format[B], fc: Format[C], fd: Format[D]): Format[E] = {
    ((JsPath \ ap).format[A] and (JsPath \ bp).format[B] and (JsPath \ cp).format[C] and (JsPath \ dp).format[D])(apply, Function.unlift(unapply))
  }

  def format[A, B, C, D, E, F](ap: String, bp: String, cp: String, dp: String, ep: String, apply: (A, B, C, D, E) => F, unapply: F => Option[(A, B, C, D, E)])(implicit fa: Format[A], fb: Format[B], fc: Format[C], fd: Format[D], fe: Format[E]): Format[F] = {
    ((JsPath \ ap).format[A] and (JsPath \ bp).format[B] and (JsPath \ cp).format[C] and (JsPath \ dp).format[D] and (JsPath \ ep).format[E])(apply, Function.unlift(unapply))
  }

  def format[A, B, C, D, E, F, G](ap: String, bp: String, cp: String, dp: String, ep: String, fp: String, apply: (A, B, C, D, E, F) => G, unapply: G => Option[(A, B, C, D, E, F)])(implicit fa: Format[A], fb: Format[B], fc: Format[C], fd: Format[D], fe: Format[E], ff: Format[F]): Format[G] = {
    ((JsPath \ ap).format[A] and (JsPath \ bp).format[B] and (JsPath \ cp).format[C] and (JsPath \ dp).format[D] and (JsPath \ ep).format[E] and (JsPath \ fp).format[F])(apply, Function.unlift(unapply))
  }

  def format[A, B, C, D, E, F, G, H](ap: String, bp: String, cp: String, dp: String, ep: String, fp: String, gp: String, apply: (A, B, C, D, E, F, G) => H, unapply: H => Option[(A, B, C, D, E, F, G)])(implicit fa: Format[A], fb: Format[B], fc: Format[C], fd: Format[D], fe: Format[E], ff: Format[F], fg: Format[G]): Format[H] = {
    ((JsPath \ ap).format[A] and (JsPath \ bp).format[B] and (JsPath \ cp).format[C] and (JsPath \ dp).format[D] and (JsPath \ ep).format[E] and (JsPath \ fp).format[F] and (JsPath \ gp).format[G])(apply, Function.unlift(unapply))
  }

  def format[A, B, C, D, E, F, G, H, I](ap: String, bp: String, cp: String, dp: String, ep: String, fp: String, gp: String, hp: String, apply: (A, B, C, D, E, F, G, H) => I, unapply: I => Option[(A, B, C, D, E, F, G, H)])(implicit fa: Format[A], fb: Format[B], fc: Format[C], fd: Format[D], fe: Format[E], ff: Format[F], fg: Format[G], fh: Format[H]): Format[I] = {
    ((JsPath \ ap).format[A] and (JsPath \ bp).format[B] and (JsPath \ cp).format[C] and (JsPath \ dp).format[D] and (JsPath \ ep).format[E] and (JsPath \ fp).format[F] and (JsPath \ gp).format[G] and (JsPath \ hp).format[H])(apply, Function.unlift(unapply))
  }

  def format[A, B, C, D, E, F, G, H, I, J](ap: String, bp: String, cp: String, dp: String, ep: String, fp: String, gp: String, hp: String, ip: String, apply: (A, B, C, D, E, F, G, H, I) => J, unapply: J => Option[(A, B, C, D, E, F, G, H, I)])(implicit fa: Format[A], fb: Format[B], fc: Format[C], fd: Format[D], fe: Format[E], ff: Format[F], fg: Format[G], fh: Format[H], fi: Format[I]): Format[J] = {
    ((JsPath \ ap).format[A] and (JsPath \ bp).format[B] and (JsPath \ cp).format[C] and (JsPath \ dp).format[D] and (JsPath \ ep).format[E] and (JsPath \ fp).format[F] and (JsPath \ gp).format[G] and (JsPath \ hp).format[H] and (JsPath \ ip).format[I])(apply, Function.unlift(unapply))
  }

  def format[A, B, C, D, E, F, G, H, I, J, K](ap: String, bp: String, cp: String, dp: String, ep: String, fp: String, gp: String, hp: String, ip: String, jp: String, apply: (A, B, C, D, E, F, G, H, I, J) => K, unapply: K => Option[(A, B, C, D, E, F, G, H, I, J)])(implicit fa: Format[A], fb: Format[B], fc: Format[C], fd: Format[D], fe: Format[E], ff: Format[F], fg: Format[G], fh: Format[H], fi: Format[I], fj: Format[J]): Format[K] = {
    ((JsPath \ ap).format[A] and (JsPath \ bp).format[B] and (JsPath \ cp).format[C] and (JsPath \ dp).format[D] and (JsPath \ ep).format[E] and (JsPath \ fp).format[F] and (JsPath \ gp).format[G] and (JsPath \ hp).format[H] and (JsPath \ ip).format[I] and (JsPath \ jp).format[J])(apply, Function.unlift(unapply))
  }

}

object JsonUtil extends JsonUtil
