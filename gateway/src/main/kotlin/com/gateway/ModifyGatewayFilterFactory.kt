package com.gateway

import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction
import org.springframework.cloud.gateway.support.BodyInserterContext
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.MultipartHttpMessageReader
import org.springframework.http.codec.multipart.Part
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.DispatcherHandler
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI


@Component
class ModifyGatewayFilterFactory(
        private val dispatcherHandler: ObjectProvider<DispatcherHandler>,
) : AbstractGatewayFilterFactory<ModifyGatewayFilterFactory.Config>(Config::class.java) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    class Config {
    }

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val uri: URI = URI.create("http://localhost:8001/document")
            exchange.attributes[GATEWAY_REQUEST_URL_ATTR] = uri
            val newExchange = exchange.mutate().request(exchange.request.mutate().uri(uri).build()).build()
            val serverRequest = ServerRequest.create(newExchange, listOf(MultipartHttpMessageReader(DefaultPartHttpMessageReader())))
            val modifiedRequest = serverRequest.bodyToMono(object : ParameterizedTypeReference<MultiValueMap<String, Part>>() {}).map {
                val request = LinkedMultiValueMap<String, Part>()
                request["document"] = it["file"]?.first() as Part
                request
            }

            val bodyInserter = BodyInserters.fromPublisher(Mono.from(modifiedRequest), MultiValueMap::class.java)
            val headers = HttpHeaders()
            headers.putAll(newExchange.request.headers)
            headers.remove(HttpHeaders.CONTENT_LENGTH)

            val outputMessage = CachedBodyOutputMessage(newExchange, headers)

            bodyInserter.insert(outputMessage, BodyInserterContext())
                    .then(Mono.defer {
                        val decorator = decorate(newExchange, headers, outputMessage)
                        dispatcherHandler.ifAvailable!!.handle(newExchange.mutate().request(decorator).build())
                    })

            //.onErrorResume { throwable -> }
            //.onErrorResume((Function<Throwable, Mono<Void>>) throwable -> release(exchange,outputMessage, throwable)
        }
    }

    /*protected fun release(exchange: ServerWebExchange?, outputMessage: CachedBodyOutputMessage,
                          throwable: Throwable?): Mono<Void>? {
        return if (outputMessage.isCached()) {
            outputMessage.body.map { dataBuffer: DataBuffer? -> DataBufferUtils.release(dataBuffer) }.then(Mono.error(throwable!!))
        } else Mono.error(throwable!!)
    }*/

    fun decorate(exchange: ServerWebExchange, headers: HttpHeaders, outputMessage: CachedBodyOutputMessage): ServerHttpRequestDecorator {
        return object : ServerHttpRequestDecorator(exchange.request) {

            override fun getHeaders(): HttpHeaders {
                val contentLength = headers.contentLength
                val httpHeaders = HttpHeaders()
                httpHeaders.putAll(headers)
                if (contentLength > 0) {
                    httpHeaders.contentLength = contentLength
                } else {
                    // TODO: this causes a 'HTTP/1.1 411 Length Required' // on
                    // httpbin.org
                    httpHeaders[HttpHeaders.TRANSFER_ENCODING] = "chunked"
                }
                return httpHeaders
            }

            override fun getBody(): Flux<DataBuffer> {
                return outputMessage.body
            }
        }
    }
}

class ModifyRewriteFunction : RewriteFunction<MultiValueMap<String, Part>, MultiValueMap<String, Part>> {
    override fun apply(exchange: ServerWebExchange, value: MultiValueMap<String, Part>): Publisher<MultiValueMap<String, Part>> {
        return value.toMono()
    }

}

data class UploadRequest(
        val file: FilePart
)
