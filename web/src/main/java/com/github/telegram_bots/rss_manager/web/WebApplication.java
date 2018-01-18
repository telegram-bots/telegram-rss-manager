package com.github.telegram_bots.rss_manager.web;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.nio.file.Files.exists;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.commons.lang3.StringUtils.split;

@Slf4j
public class WebApplication {
    private HttpServer<?, ?> createServer(final int port, final Path storagePath) {
        return HttpServer.newServer(port)
                .enableWireLogging("web", LogLevel.DEBUG)
                .addChannelHandlerLast("logger", ChannelLoggingHandler::new)
                .start((req, resp) -> {
                    try {
                        val params = getParams(req);
                        if (params.length != 3 || !Objects.equals(params[0], "feed") || !isNumeric(params[1])) {
                            return resp.setStatus(BAD_REQUEST).sendHeaders();
                        }

                        val file = storagePath.resolve(Long.parseLong(params[1]) + "_" + params[2] + ".xml");
                        if (!exists(file)) {
                            return resp.setStatus(NOT_FOUND).sendHeaders();
                        }

                        return resp.setStatus(OK)
                                .setHeader("content-type", "application/rss+xml; charset=utf-8")
                                .writeStringAndFlushOnEach(streamFile(file));
                    } catch (Exception e) {
                        return resp.setStatus(INTERNAL_SERVER_ERROR).sendHeaders();
                    }
                });
    }

    private String[] getParams(HttpServerRequest<?> request) {
        return Stream.of(split(request.getUri(), "/"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toArray(String[]::new);
    }

    private Observable<String> streamFile(final Path file) {
        return Observable.using(
                () -> {
                    try {
                        return Files.newBufferedReader(file);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                reader -> Observable.from(() -> reader.lines().iterator()),
                reader -> {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private static class ChannelLoggingHandler extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof HttpRequest) {
                log.info("{}", msg);
            }
            super.channelRead(ctx, msg);
        }
    }

    public static void main(String[] args) {
        final HttpServer<?, ?> server = new WebApplication().createServer(8691, Paths.get("/data"));
        server.awaitShutdown();
    }
}