package models.daos.slick

import java.util.UUID

import models.User
import models.daos.UserDAO
import models.daos.slick.DBTableDefinitions._
import play.Logger
import play.api.db.DB
import com.mohiva.play.silhouette.api.LoginInfo
import scala.slick.driver.PostgresDriver.simple._
import play.api.Play.current


import scala.concurrent.Future

/**
 * Give access to the user object using Slick
 */
class UserDAOSlick extends UserDAO {

  private val db = Database.forDataSource(DB.getDataSource())

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo) = {
    db.withSession { implicit session =>
      Future.successful {
        slickLoginInfos.filter(
          x => x.providerID === loginInfo.providerID && x.providerKey === loginInfo.providerKey
        ).firstOption match {
          case Some(info) =>
            slickUserLoginInfos.filter(_.loginInfoId === info.id).firstOption match {
              case Some(userLoginInfo) =>
                slickUsers.filter(_.id === userLoginInfo.userID).firstOption match {
                  case Some(user) =>
                    Some(User(UUID.fromString(user.userID), loginInfo, user.firstName, user.lastName, user.fullName, user.email, user.avatarURL))
                  case None => None
                }
              case None => None
            }
          case None => None
        }
      }
    }
  }

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: UUID) = {
    db.withSession { implicit session =>
      Future.successful {
        slickUsers.filter(
          _.id === userID.toString
        ).firstOption match {
          case Some(user) =>
            slickUserLoginInfos.filter(_.userID === user.userID).firstOption match {
              case Some(info) =>
                slickLoginInfos.filter(_.id === info.loginInfoId).firstOption match {
                  case Some(loginInfo) =>
                    Some(User(UUID.fromString(user.userID), LoginInfo(loginInfo.providerID, loginInfo.providerKey), user.firstName, user.lastName, user.fullName, user.email, user.avatarURL))
                  case None => None
                }
              case None => None
            }
          case None => None
        }
      }
    }
  }

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User) = {
    db.withSession { implicit session =>
      Future.successful {
        val dbUser = DBUser(user.userID.toString, user.firstName, user.lastName, user.fullName, user.email, user.avatarURL)
        slickUsers.filter(_.id === dbUser.userID).firstOption match {
          case Some(userFound) => slickUsers.filter(_.id === dbUser.userID).update(dbUser)
          case None => slickUsers.insert(dbUser)
        }
        var dbLoginInfo = DBLoginInfo(None, user.loginInfo.providerID, user.loginInfo.providerKey)
        // Insert if it does not exist yet
        slickLoginInfos.filter(info => info.providerID === dbLoginInfo.providerID && info.providerKey === dbLoginInfo.providerKey).firstOption match {
          case None => slickLoginInfos.insert(dbLoginInfo)
          case Some(info) => Logger.debug("Nothing to insert since info already exists: " + info)
        }
        dbLoginInfo = slickLoginInfos.filter(info => info.providerID === dbLoginInfo.providerID && info.providerKey === dbLoginInfo.providerKey).first
        // Now make sure they are connected
        slickUserLoginInfos.filter(info => info.userID === dbUser.userID && info.loginInfoId === dbLoginInfo.id).firstOption match {
          case Some(info) =>
          // They are connected already, we could as well omit this case ;)
          case None =>
            slickUserLoginInfos += DBUserLoginInfo(dbUser.userID, dbLoginInfo.id.get)
        }
        user // We do not change the user => return it
      }
    }
  }
}

object UserDAOSlickFinder extends UserDAOSlick {

  private val db = Database.forDataSource(DB.getDataSource())

  def findEmailForPassReset(email: String) =
    db.withSession { implicit session =>
      Future.successful {
        slickLoginInfos.filter(_.providerKey === email).firstOption
      }
    }
}



