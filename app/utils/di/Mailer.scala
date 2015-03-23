package utils.di


import models._
import play.api.libs.mailer.{MailerPlugin, Email}
import play.api.mvc.{Controller, Action}
import play.api.Play.current


object Mailer extends Controller {
//
//  def forgotPassword(email: String, link: String) =  {
//    println("1")
//
//    val emailInside = Email(
//    subject = "Simple mail",
//    from = "development@development.com",
//    to = Seq(email),
//    bodyText = Option(views.html.forgotPasswordTxt(email, link).toString),
//    bodyHtml = Option(views.html.forgotPasswordMessageHTML(email,link).toString)
//    )
//    val id = MailerPlugin.send(emailInside)
//    println(emailInside + "2")
//    Ok(s"Email $id sent!")
//  }

  def forgotPassword(email: String, link: String) = {
    println(email)
    println(link)
  }
}
