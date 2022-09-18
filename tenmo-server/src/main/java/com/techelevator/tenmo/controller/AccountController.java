package com.techelevator.tenmo.controller;

import com.techelevator.tenmo.dao.AccountDao;
import com.techelevator.tenmo.exception.AccountNotFoundException;
import com.techelevator.tenmo.model.Account;
import com.techelevator.util.BasicLogger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@PreAuthorize("isAuthenticated()")
public class AccountController {

    private AccountDao accountDao;

    public AccountController(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @GetMapping(value = "/myAccount")
    public Account getAccount(Principal principal) throws AccountNotFoundException {
        return accountDao.findAccountByUsername(principal.getName());
    }

    @GetMapping(value = "/account")
    public List<Account> getAllOtherAccounts(Principal principal){
        Account currentAccount= null;
        try {
            currentAccount = getAccount(principal);
        } catch (AccountNotFoundException e){
            BasicLogger.log(e.getMessage());
        }

        List<Account> accounts = accountDao.findAllOther();
        accounts.remove(currentAccount);
        return accounts;
    }
}