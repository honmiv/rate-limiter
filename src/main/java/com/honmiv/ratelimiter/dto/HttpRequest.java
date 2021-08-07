package com.honmiv.ratelimiter.dto;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HttpRequest {
    String rqUid;
    String value;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
