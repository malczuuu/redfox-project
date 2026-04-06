package io.github.malczuuu.redfox.app.security

import io.github.problem4j.core.Problem
import io.github.problem4j.core.ProblemContext
import io.github.problem4j.spring.webmvc.AdviceWebMvcInspector
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest

@Component
class DiagnosticInspector(private val meterRegistry: MeterRegistry) : AdviceWebMvcInspector {

  companion object {
    private val log = LoggerFactory.getLogger(DiagnosticInspector::class.java)

    private const val REST_ERRORS_METRIC = "redfox.app.rest.errors"
  }

  override fun inspect(
      context: ProblemContext,
      problem: Problem,
      ex: Exception,
      headers: HttpHeaders,
      status: HttpStatusCode,
      request: WebRequest,
  ) {
    val info = RequestInfo.from(request, problem, ex)

    val tags =
        listOf(
            Tag.of("error", info.error),
            Tag.of("status", status.value().toString()),
            Tag.of("path", info.path),
            Tag.of("method", info.method),
        )

    meterRegistry.counter(REST_ERRORS_METRIC, tags).increment()

    var builder = log.atError()
    tags.forEach { tag -> builder = builder.addKeyValue(tag.key, tag.value) }

    if (log.isDebugEnabled) {
      builder = builder.setCause(ex)
    }

    builder.log("Handled exception in HTTP controller")
  }

  private data class RequestInfo(val path: String, val method: String, val error: String) {
    companion object {
      private const val UNKNOWN = "unknown"

      fun from(request: WebRequest, problem: Problem, ex: Exception): RequestInfo {
        val type =
            if (problem.isTypeNonBlank) {
              problem.type.toString()
            } else {
              ex.javaClass.simpleName
            }

        return if (request is ServletWebRequest) {
          RequestInfo(request.request.requestURI, request.request.method, type)
        } else {
          RequestInfo(UNKNOWN, UNKNOWN, type)
        }
      }
    }
  }
}
