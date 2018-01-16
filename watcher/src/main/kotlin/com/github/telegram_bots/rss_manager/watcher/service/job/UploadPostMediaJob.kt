package com.github.telegram_bots.rss_manager.watcher.service.job

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils.emptyMap
import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.api.utils.MediaInput
import com.github.badoualy.telegram.api.utils.getAbsMediaInput
import com.github.badoualy.telegram.tl.api.TLMessage
import com.github.badoualy.telegram.tl.api.upload.TLFile
import com.github.telegram_bots.rss_manager.watcher.domain.FileURL
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.Single.defer
import io.reactivex.functions.Function
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.APPEND
import java.util.*

class UploadPostMediaJob(private val client: TelegramClient, private val cloud: Cloudinary)
    : Function<TLMessage, Single<Optional<String>>> {
    companion object {
        private const val LARGE_SIZE = 20_000_000
        private const val CHUNK_SIZE = 5_000_000
    }

    override fun apply(message: TLMessage): Single<Optional<FileURL>> = defer {
        val media = message.media?.getAbsMediaInput()

        if (media == null) Single.just(Optional.empty())
        else {
            val tmpFile = Files.createTempFile("rss", null)
            Maybe.just(media)
                    .filter { it.mimeType == "image/jpeg" } // Just images for now
                    .flatMapObservable { downloadFile(media) }
                    .reduce(tmpFile, { file, data -> Files.write(file, data.bytes.data, APPEND) })
                    .filter { Files.size(it) > 0 }
                    .flatMap(::uploadFile)
                    .map { Optional.of(it) }
                    .toSingle(Optional.empty())
                    .doAfterTerminate { Files.delete(tmpFile) }
        }
    }

    private fun downloadFile(media: MediaInput, tries: Int = 5): Observable<TLFile> =
            if (tries == 0) Observable.empty()
            else Observable.defer { client.downloadFile(media.inputFileLocation, media.size) }
                    .retry(5)
                    .switchIfEmpty(downloadFile(media, tries - 1))

    private fun uploadFile(file: Path): Maybe<FileURL> = Maybe
            .fromCallable {
                if (Files.size(file) >= LARGE_SIZE)
                    cloud.uploader().uploadLarge(
                            Files.newInputStream(file),
                            mapOf("chunk_size" to CHUNK_SIZE)
                    )
                else cloud.uploader().upload(Files.readAllBytes(file), emptyMap())
            }
            .retry(5)
            .map { it as? Map<*, *> }
            .flatMap { (it["secure_url"] as? String)?.let { Maybe.just(it) } ?: Maybe.empty() }
            .onErrorResumeNext(Maybe.empty())
}