package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages


object ResetPasswordForms {

  val emailForm = Form(single("email" -> email))

  val passwordForm = Form(tuple(
    "password1" -> nonEmptyText(minLength = 6),
    "password2" -> nonEmptyText
  ) verifying(Messages("Passwords not equal"), passwords => passwords._1 == passwords._2))

}
