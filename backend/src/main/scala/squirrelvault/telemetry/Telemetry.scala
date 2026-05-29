package squirrelvault.telemetry

import io.opentelemetry.api.common.{AttributeKey, Attributes}
import io.opentelemetry.api.{OpenTelemetry as JOpenTelemetry}
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.BatchSpanProcessor
import zio.*
import zio.telemetry.opentelemetry.OpenTelemetry
import zio.telemetry.opentelemetry.tracing.Tracing

object Telemetry:

  private val otlpEndpoint = sys.env.getOrElse("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4318")
  private val serviceName = sys.env.getOrElse("OTEL_SERVICE_NAME", "squirrelvault")

  private val buildSdk: Task[JOpenTelemetry] = ZIO.attempt {
    val resource = Resource.create(
      Attributes.of(AttributeKey.stringKey("service.name"), serviceName)
    )
    val exporter = OtlpHttpSpanExporter
      .builder()
      .setEndpoint(s"$otlpEndpoint/v1/traces")
      .build()
    val tracerProvider = SdkTracerProvider
      .builder()
      .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
      .setResource(resource)
      .build()
    OpenTelemetrySdk
      .builder()
      .setTracerProvider(tracerProvider)
      .build(): JOpenTelemetry
  }

  private val ctxLayer = OpenTelemetry.contextZIO

  val layer: TaskLayer[Tracing] =
    (OpenTelemetry.custom(buildSdk) ++ ctxLayer) >>> OpenTelemetry.tracing(serviceName)
