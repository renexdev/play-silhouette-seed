package controllers

import java.util.UUID
import javax.inject.Inject


import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.services.AuthInfoService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.impl.services.DelegableAuthInfoService
import com.mohiva.play.silhouette.impl.util.BCryptPasswordHasher
import com.mohiva.play.silhouette.api.exceptions.AuthenticatorException
import forms._
import models.daos.slick.{UserDAOSlickFinder, UserDAOSlick}
import models.services.UserService
import models.{TokenUser, User}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.mvc.RequestHeader
import utils.di.Mailer
import models.TokenUserService
import play.mvc._
import models.daos.slick._
import scala.concurrent.ExecutionContext.Implicits.global



import scala.concurrent.Future

/**
 * The basic application controller.
 *
 * @param env The Silhouette environment.
 */
class ApplicationController @Inject() (implicit val env: Environment[User, SessionAuthenticator],
                                       val userService: UserService,
                                       val authInfoService: AuthInfoService,
                                       val passwordHasher: PasswordHasher)
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

  val passwordForm = Form(tuple(
    "password1" -> nonEmptyText(minLength = 6),
    "password2" -> nonEmptyText
  ) verifying(Messages("Passwords not equal"), passwords => passwords._1 == passwords._2))

  def resetPassword(tokenId: String) = UserAwareAction.async { implicit request =>
    lazy val tokenService = new TokenUserService
    tokenService.retrieve(tokenId).flatMap {
      case Some(token) if (!token.isSignUp && !token.isExpired) => {
        Future.successful(Ok(views.html.resetPassword(tokenId,passwordForm)))
      }
      case Some(token) => {
        tokenService.consume(tokenId)
        notFoundDefault
      }
      case None => notFoundDefault
    }
  }

//  def handleResetPassword(tokenId: String) = TODO

  def handleResetPassword(tokenId: String) = UserAwareAction.async { implicit request =>
    passwordForm.bindFromRequest.fold(
      hasErrors => Future.successful(BadRequest(views.html.resetPassword(tokenId, hasErrors))),
      passwords => {
        lazy val tokenService = new TokenUserService
        tokenService.retrieve(tokenId).flatMap {
          case Some(token) if (!token.isSignUp && !token.isExpired) => {
            UserDAOSlickFinder.findMyEmail(token.email).flatMap {
              case Some(loginInfo) => {

                val loginInfo = LoginInfo(CredentialsProvider.ID, token.email)
                val authInfo = passwordHasher.hash(passwords._1)
                authInfoService.save(loginInfo, authInfo)

                // Handling works, but the password doesnt change
                env.authenticatorService.create(loginInfo).flatMap { authenticator =>
//                  env.eventBus.publish(LoginEvent(loginInfo, request, request2lang))
                  tokenService.consume(tokenId)
                  env.authenticatorService.init(authenticator).flatMap(v => env.authenticatorService.embed(v, Future.successful(Ok(views.html.resetedPassword(loginInfo)))))
                }
              }
              case None => Future.failed(new AuthenticatorException("Could not find the user"))
            }
          }
          case Some(token) => {
            tokenService.consume(tokenId)
            notFoundDefault
          }
          case None => notFoundDefault
        }
      }
    )
  }



  def notFoundDefault(implicit request: RequestHeader) = Future.successful(NotFound(views.html.onHandlerNotFound(request)))
}


