package utils.di


import models._
import play.api.libs.mailer.{MailerPlugin, Email}
import play.api.mvc.{Controller, Action}
import play.api.Play.current


object Mailer extends Controller {

  def forgotPassword(email: String, link: String) =  {
    val emailInside = Email(
    "Simple mail",
    "MISER FROM <from@email.com>",
    Seq("Miss TO <to@email.com>"),
    bodyText = Option(views.html.forgotPasswordTxt(email, link).toString),
    bodyHtml = Option(views.html.forgotPasswordMessageHTML(email,link).toString)
    )
    val id = MailerPlugin.send(emailInside)
    Ok(s"Email $id sent!")
  }

}
