package io.seeitlive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Wire shape of an error response: {@code {statusCode,error,message,code?,field?}}. Unknown fields ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
record ErrorBody(String message, String code, String field) {
}
