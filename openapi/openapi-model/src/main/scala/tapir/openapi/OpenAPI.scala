package tapir.openapi

import tapir.openapi.OpenAPI.{ReferenceOr, SecurityRequirement}

import scala.collection.immutable.ListMap

// todo tags, externaldocs
case class OpenAPI(openapi: String = "3.0.1",
                   info: Info,
                   servers: List[Server],
                   paths: ListMap[String, PathItem],
                   components: Option[Components],
                   security: List[SecurityRequirement]) {

  def addPathItem(path: String, pathItem: PathItem): OpenAPI = {
    val pathItem2 = paths.get(path) match {
      case None           => pathItem
      case Some(existing) => existing.mergeWith(pathItem)
    }

    copy(paths = paths + (path -> pathItem2))
  }

  def servers(s: List[Server]): OpenAPI = copy(servers = s)
}

object OpenAPI {
  type ReferenceOr[T] = Either[Reference, T]
  // using a Vector instead of a List, as empty Lists are always encoded as nulls
  // here, we need them encoded as an empty array
  type SecurityRequirement = ListMap[String, Vector[String]]
}

case class Info(
    title: String,
    version: String,
    description: Option[String] = None,
    termsOfService: Option[String] = None,
    contact: Option[Contact] = None,
    license: Option[License] = None
)

case class Contact(name: Option[String], email: Option[String], url: Option[String])
case class License(name: String, url: Option[String])

// todo: variables
case class Server(
    url: String,
    description: Option[String]
)

// todo: responses, parameters, examples, requestBodies, headers, links, callbacks
case class Components(
    schemas: ListMap[String, ReferenceOr[Schema]],
    securitySchemes: ListMap[String, ReferenceOr[SecurityScheme]]
)

// todo: $ref
case class PathItem(
    summary: Option[String],
    description: Option[String],
    get: Option[Operation],
    put: Option[Operation],
    post: Option[Operation],
    delete: Option[Operation],
    options: Option[Operation],
    head: Option[Operation],
    patch: Option[Operation],
    trace: Option[Operation],
    servers: List[Server],
    parameters: List[ReferenceOr[Parameter]]
) {
  def mergeWith(other: PathItem): PathItem = {
    PathItem(
      None,
      None,
      get = get.orElse(other.get),
      put = put.orElse(other.put),
      post = post.orElse(other.post),
      delete = delete.orElse(other.delete),
      options = options.orElse(other.options),
      head = head.orElse(other.head),
      patch = patch.orElse(other.patch),
      trace = trace.orElse(other.trace),
      servers = List.empty,
      parameters = List.empty
    )
  }
}

// todo: external docs, callbacks, security
case class Operation(
    tags: List[String],
    summary: Option[String],
    description: Option[String],
    operationId: String,
    parameters: List[ReferenceOr[Parameter]],
    requestBody: Option[ReferenceOr[RequestBody]],
    responses: ListMap[ResponsesKey, ReferenceOr[Response]],
    deprecated: Option[Boolean],
    security: List[SecurityRequirement],
    servers: List[Server]
)

case class Parameter(
    name: String,
    in: ParameterIn.ParameterIn,
    description: Option[String],
    required: Option[Boolean],
    deprecated: Option[Boolean],
    allowEmptyValue: Option[Boolean],
    style: Option[ParameterStyle.ParameterStyle],
    explode: Option[Boolean],
    allowReserved: Option[Boolean],
    schema: ReferenceOr[Schema],
    example: Option[ExampleValue],
    examples: ListMap[String, ReferenceOr[Example]],
    content: ListMap[String, MediaType]
)

object ParameterIn extends Enumeration {
  type ParameterIn = Value

  val Query = Value("query")
  val Header = Value("header")
  val Path = Value("path")
  val Cookie = Value("cookie")
}

object ParameterStyle extends Enumeration {
  type ParameterStyle = Value

  val Simple = Value("simple")
  val Form = Value("form")
  val Matrix = Value("matrix")
  val Label = Value("label")
  val SpaceDelimited = Value("spaceDelimited")
  val PipeDelimited = Value("pipeDelimited")
  val DeepObject = Value("deepObject")
}

case class RequestBody(description: Option[String], content: ListMap[String, MediaType], required: Option[Boolean])

case class MediaType(
    schema: Option[ReferenceOr[Schema]],
    example: Option[ExampleValue],
    examples: ListMap[String, ReferenceOr[Example]],
    encoding: ListMap[String, Encoding]
)

case class Encoding(
    contentType: Option[String],
    headers: ListMap[String, ReferenceOr[Header]],
    style: Option[ParameterStyle.ParameterStyle],
    explode: Option[Boolean],
    allowReserved: Option[Boolean]
)

sealed trait ResponsesKey
case object ResponsesDefaultKey extends ResponsesKey
case class ResponsesCodeKey(code: Int) extends ResponsesKey

// todo: links
case class Response(description: String, headers: ListMap[String, ReferenceOr[Header]], content: ListMap[String, MediaType])

case class Example(summary: Option[String], description: Option[String], value: Option[ExampleValue], externalValue: Option[String])

case class Header(description: Option[String],
                  required: Option[Boolean],
                  deprecated: Option[Boolean],
                  allowEmptyValue: Option[Boolean],
                  style: Option[ParameterStyle.ParameterStyle],
                  explode: Option[Boolean],
                  allowReserved: Option[Boolean],
                  schema: Option[ReferenceOr[Schema]],
                  example: Option[ExampleValue],
                  examples: ListMap[String, ReferenceOr[Example]],
                  content: ListMap[String, MediaType])

case class Reference($ref: String)

// todo: discriminator, xml, json-schema properties
case class Schema(title: Option[String],
                  required: List[String],
                  `type`: SchemaType.SchemaType,
                  items: Option[ReferenceOr[Schema]],
                  properties: ListMap[String, ReferenceOr[Schema]],
                  description: Option[String],
                  format: Option[SchemaFormat.SchemaFormat],
                  default: Option[ExampleValue],
                  nullable: Option[Boolean],
                  readOnly: Option[Boolean],
                  writeOnly: Option[Boolean],
                  example: Option[ExampleValue],
                  deprecated: Option[Boolean])

object Schema {
  def apply(`type`: SchemaType.SchemaType): Schema =
    Schema(None, List.empty, `type`, None, ListMap.empty, None, None, None, None, None, None, None, None)
}

object SchemaType extends Enumeration {
  type SchemaType = Value

  val Boolean = Value("boolean")
  val Object = Value("object")
  val Array = Value("array")
  val Number = Value("number")
  val String = Value("string")
  val Integer = Value("integer")
}

object SchemaFormat extends Enumeration {
  type SchemaFormat = Value

  val Int32 = Value("int32")
  val Int64 = Value("int64")
  val Float = Value("float")
  val Double = Value("double")
  val Byte = Value("byte")
  val Binary = Value("binary")
  val Date = Value("date")
  val DateTime = Value("date-time")
  val Password = Value("password")
}

case class ExampleValue(value: String)

case class SecurityScheme(`type`: String,
                          description: Option[String],
                          name: Option[String],
                          in: Option[String],
                          scheme: Option[String],
                          bearerFormat: Option[String],
                          flows: Option[OAuthFlows],
                          openIdConnectUrl: Option[String])

case class OAuthFlows(`implicit`: Option[OAuthFlow],
                      password: Option[OAuthFlow],
                      clientCredentials: Option[OAuthFlow],
                      authorizationCode: Option[OAuthFlow])

case class OAuthFlow(authorizationUrl: String, tokenUrl: String, refreshUrl: Option[String], scopes: ListMap[String, String])
