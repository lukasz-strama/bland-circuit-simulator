package pl.polsl.bland.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserDto(
        @JsonProperty("id") Long id,
        @JsonProperty("username") String username,
        @JsonProperty("email") String email) {
}
