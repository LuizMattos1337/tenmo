package com.techelevator.tenmo.services;

import com.techelevator.tenmo.model.Account;
import com.techelevator.tenmo.model.AuthenticatedUser;
import com.techelevator.tenmo.model.User;
import com.techelevator.util.BasicLogger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

public class AccountService {
    private final String BASE_URL;
    private final RestTemplate restTemplate = new RestTemplate();

    public AccountService(String url) {
        BASE_URL = url;
    }

    public BigDecimal getBalance (AuthenticatedUser user){
        Account account = null;
        try {
            ResponseEntity<Account> response = restTemplate.exchange(BASE_URL+"/myAccount", HttpMethod.GET, makeAuthEntity(user.getToken()), Account.class);
            account = response.getBody();
            assert account != null;
            return account.getBalance();
        } catch (AssertionError e){
            BasicLogger.log(e.getMessage());
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public Long getLoggedInAccountId(AuthenticatedUser user){
        Account account = null;
        try{
            ResponseEntity<Account> response = restTemplate.exchange(BASE_URL+"/myAccount",HttpMethod.GET, makeAuthEntity(user.getToken()), Account.class);
            account= response.getBody();
            assert account!=null;
            return account.getAccountId();
        } catch (AssertionError e){
            BasicLogger.log(e.getMessage());
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public Account getLoggedInAccount(AuthenticatedUser user) {
        Account account = null;
        try{
            ResponseEntity<Account> response = restTemplate.exchange(BASE_URL+"/myAccount",HttpMethod.GET, makeAuthEntity(user.getToken()), Account.class);
            account= response.getBody();
            assert account!=null;
            return account;
        } catch (AssertionError e){
            BasicLogger.log(e.getMessage());
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public Account[] getAllOtherAccounts(AuthenticatedUser user){
        Account[] accounts = null;
        try {
            ResponseEntity<Account[]> response = restTemplate.exchange(BASE_URL+"/account", HttpMethod.GET, makeAuthEntity(user.getToken()), Account[].class);
            accounts = response.getBody();
        } catch (RestClientResponseException | ResourceAccessException e){
            BasicLogger.log(e.getMessage());
        }
        return accounts;
    }

    public User[] getAllOtherUsers(AuthenticatedUser user){
        User[] users = null;
        try {
            ResponseEntity<User[]> response = restTemplate.exchange(BASE_URL+"/user", HttpMethod.GET, makeAuthEntity(user.getToken()), User[].class);
            users = response.getBody();
        } catch (RestClientResponseException | ResourceAccessException e){
            BasicLogger.log(e.getMessage());
        }
        return users;
    }

    private HttpEntity<Void> makeAuthEntity(String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        return new HttpEntity<>(headers);
    }
}
