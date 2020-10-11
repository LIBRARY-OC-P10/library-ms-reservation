package org.mickael.librarymsreservation.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HandlerToken {

    public String formatToken (String token){
        return token.contains("Bearer ")?token:"Bearer " + token;
    }
}
