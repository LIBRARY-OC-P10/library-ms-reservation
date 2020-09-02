package org.mickael.librarymsreservation.exception;

import feign.Response;
import feign.codec.ErrorDecoder;

public class CustomErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String invoquer, Response response) {
        if (response.status() == 400){
            return new BadRequestException(response.body().toString());
        } else if (response.status() == 404){
            return new NotFoundException(response.body().toString());
        }
        return defaultErrorDecoder.decode(invoquer, response);
    }
}
