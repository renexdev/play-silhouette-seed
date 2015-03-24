package models.daos.slick



import java.util.UUID

import models.User

import models.daos.slick.DBTableDefinitions._
import play.api.db.DB


import scala.slick.driver.PostgresDriver.simple._
import play.api.Play.current
import scala.concurrent.Future


object UserDAOSlickFinder extends UserDAOSlick {

  private val db = Database.forDataSource(DB.getDataSource())

//  /**
//   * Finds a user by its login info.
//   *
//   * @param loginInfo The login info of the user to find.
//   * @return The found user email or None if no user for the given login info could be found.
//   */
//  def find(loginInfo: LoginInfo):Future[Option[User]] = {
//    db.withSession { implicit session =>
//      Future.successful {
//        slickLoginInfos.filter(
//          x => x.providerID === loginInfo.providerID && x.providerKey === loginInfo.providerKey
//        ).firstOption match {
//          case Some(info) =>
//            slickUserLoginInfos.filter(_.loginInfoId === info.id).firstOption match {
//              case Some(userLoginInfo) =>
//                slickUsers.filter(_.id === userLoginInfo.userID).firstOption match {
//                  case Some(user) =>
//                    Some(User(UUID.fromString(user.userID), loginInfo, user.firstName, user.lastName, user.fullName, user.email, user.avatarURL))
//                  case None => None
//                }
//              case None => None
//            }
//          case None => None
//        }
//      }
//    }
//  }

  def findMyEmail(email: String) =
    db.withSession { implicit session =>
      Future.successful {
        slickLoginInfos.filter(_.providerKey === email).firstOption
      }
    }

  def updateMyPassword(email: String, password: String) = db.withSession {
    implicit request =>
      Future.successful {
        slickLoginInfos.filter(_.providerKey === email)
      }
  }


 // def findMyEmail(email: String) = email
}

object passwordInfoDAOSlickObject extends PasswordInfoDAOSlick
