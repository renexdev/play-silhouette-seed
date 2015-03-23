package models

import com.mohiva.play.silhouette.api.services.TokenService
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by lukasz on 3/23/2015.
 */
class TokenUserService extends TokenService[TokenUser] {
  def create (token: TokenUser): Future[Option[TokenUser]] = {
    TokenUser.save(token).map(Some(_))
  }
  def retrieve (id: String): Future[Option[TokenUser]] = {
    TokenUser.findById(id)
  }
  def consume (id: String): Unit = {
    TokenUser.delete(id)
  }
}
