package com.techelevator.tenmo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//Custom exception to handle when an account is not found
@ResponseStatus(code= HttpStatus.BAD_REQUEST)
public class AccountNotFoundException extends Exception{

    public AccountNotFoundException(String message){
        super(message);
    };
}
