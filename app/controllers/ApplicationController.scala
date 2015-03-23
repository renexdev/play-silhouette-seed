package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import forms._
import models.{TokenUser, User}
import play.api.data.Form
import play.api.data.Forms._
import utils.di.Mailer
import models.TokenUserService


import scala.concurrent.Future

/**
 * The basic application controller.
 *
 * @param env The Silhouette environment.
 */
class ApplicationController @Inject() (implicit val env: Environment[User, SessionAuthenticator])
  extends Silhouette[User, SessionAuthenticator] {

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.home(request.identity)))
  }

  /**
   * Handles the Sign In action.
   *
   * @return The result to display.
   */
  def signIn = UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) => Future.successful(Redirect(routes.ApplicationController.index()))
      case None => Future.successful(Ok(views.html.signIn(SignInForm.form)))
    }
  }

  /**
   * Handles the Sign Up action.
   *
   * @return The result to display.
   */
  def signUp = UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) => Future.successful(Redirect(routes.ApplicationController.index()))
      case None => Future.successful(Ok(views.html.signUp(SignUpForm.form)))
    }
  }

  /**
   * Handles the Sign Out action.
   *
   * @return The result to display.
   */
  def signOut = SecuredAction.async { implicit request =>
    val result = Future.successful(Redirect(routes.ApplicationController.index()))
    env.eventBus.publish(LogoutEvent(request.identity, request, request2lang))

    request.authenticator.discard(result)
  }
  // Forgotten Password
  val emailForm = Form(single("email" -> email))

  def forgotPassword = UserAwareAction.async { implicit request =>
    Future.successful( request.identity match {
      case Some(user) => Redirect(routes.ApplicationController.index())
      case None => Ok(views.html.forgotPassword(emailForm))
    })
  }

  def handleForgotPassword = UserAwareAction.async { implicit request =>
    emailForm.bindFromRequest.fold(
      hasErrors => Future.successful(BadRequest(views.html.forgotPassword(hasErrors))),
      email => {
        val token = TokenUser(email, isSignUp = false)
        lazy val tokenService = new TokenUserService
        tokenService.create(token)
        Mailer.forgotPassword(email, link = routes.ApplicationController.resetPassword(token.id).absoluteURL())
        Future.successful(Ok(views.html.forgotPasswordSent(email)))
      }
    )
  }

  def resetPassword(tokenId: String) = TODO

  def handleResetPassword(tokenId: String) = TODO
}
