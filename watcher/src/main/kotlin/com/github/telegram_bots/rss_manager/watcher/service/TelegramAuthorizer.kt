package com.github.telegram_bots.rss_manager.watcher.service

import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.mtproto.exception.RpcErrors.AUTH_KEY_UNREGISTERED
import com.github.badoualy.telegram.mtproto.exception.getError
import com.github.badoualy.telegram.tl.api.auth.TLAuthorization
import com.github.badoualy.telegram.tl.exception.RpcErrorException
import com.github.telegram_bots.rss_manager.watcher.config.properties.TelegramProperties
import io.reactivex.Completable
import io.reactivex.Single
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.*
import javax.annotation.PostConstruct

@Service
class TelegramAuthorizer(private val props: TelegramProperties, private val client: TelegramClient) {
    companion object : KLogging()

    @PostConstruct
    fun init() = authorize()

    fun authorize() {
        isAuthorized()
                .flatMapCompletable { authorized ->
                    if (authorized) Completable.complete()
                    else {
                        sendAuthCode()
                                .map { it to getTypedAuthCode() }
                                .flatMap(::signIn)
                                .map(::getAuthStatus)
                                .doOnSuccess(logger::info)
                                .toCompletable()
                    }
                }
                .doOnError(::onError)
                .blockingAwait()
    }

    private fun isAuthorized() = client.accountGetAuthorizations()
            .map { true }
            .onErrorResumeNext { e ->
                if (e is RpcErrorException && e.getError() == AUTH_KEY_UNREGISTERED) {
                    Single.just(false)
                } else if (e is NoSuchElementException) { // WARNING. Bug in library
                    Single.just(true)
                } else {
                    Single.error(e)
                }
            }

    private fun signIn(sentAndTypedCodes: Pair<String, String>) = client
            .authSignIn(props.phoneNumber, sentAndTypedCodes.first, sentAndTypedCodes.second)
            .onErrorResumeNext { e ->
                if (e is RpcErrorException && e.getError().type == "SESSION_PASSWORD_NEEDED")
                    client.authCheckPassword(getTypedTwoStepAuthCode())
                else Single.error(e)
            }

    private fun sendAuthCode() = client.authSendCode(false, props.phoneNumber, true)
            .map { it.phoneCodeHash }

    private fun getTypedAuthCode() = logger.info { "Enter authentication code: " }
            .run { Scanner(System.`in`).nextLine() }

    private fun getTypedTwoStepAuthCode() = logger.info { "Enter two-step auth password: " }
            .run { Scanner(System.`in`).nextLine() }

    private fun getAuthStatus(auth: TLAuthorization) = auth.user.asUser()
            ?.run { "You are now signed in as $firstName $lastName @$username" }

    private fun onError(e: Throwable): Unit = throw AuthorisationFailedException(e)

    class AuthorisationFailedException(e: Throwable) : RuntimeException(
            "Failed to authorize: ${e.message}", null, false, false
    )
}
