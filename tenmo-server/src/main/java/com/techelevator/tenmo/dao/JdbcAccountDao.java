package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.exception.AccountNotFoundException;
import com.techelevator.tenmo.model.Account;
import com.techelevator.tenmo.model.User;
import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcAccountDao implements AccountDao{

    private JdbcTemplate jdbcTemplate;

    public JdbcAccountDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Account> findAllOther() {
        List<Account> accounts=new ArrayList<>();
        @Language("SQL")
        String sql= "SELECT account_id, user_id, balance FROM account;";
        SqlRowSet results=jdbcTemplate.queryForRowSet(sql);
        while(results.next()){
            Account account=mapRowToAccount(results);
            account.setBalance(BigDecimal.ZERO);
            accounts.add(account);
        }
        return accounts;
    }

    @Override
    public Account findAccountByUserId(User user) throws AccountNotFoundException {
        @Language("SQL")
        String sql="SELECT account_id, user_id, balance FROM account WHERE user_id = ?;";
        SqlRowSet rowSet= jdbcTemplate.queryForRowSet(sql,user.getUserId());
        if(rowSet.next()){
            return mapRowToAccount(rowSet);
        }
        throw new AccountNotFoundException("Account for user " + user.getUsername() + " was not found.");
    }

    @Override
    public Account findAccountByUsername(String username) throws AccountNotFoundException {
        @Language("SQL")
        String sql="SELECT a.account_id, a.user_id, a.balance FROM tenmo_user tu JOIN account a ON a.user_id = tu.user_id WHERE tu.username = ?;";
        SqlRowSet rowSet= jdbcTemplate.queryForRowSet(sql, username);
        if(rowSet.next()){
            return mapRowToAccount(rowSet);
        }
        throw new AccountNotFoundException("Account for user " + username + " was not found.");

    }

    @Override
    public Account findAccountByAccountId(Long accountId) throws AccountNotFoundException {
        @Language("SQL")
        String sql="SELECT account_id, user_id, balance FROM account WHERE account_id=?";
        SqlRowSet rowSet=jdbcTemplate.queryForRowSet(sql,accountId);
        if(rowSet.next()){
            return mapRowToAccount(rowSet);
        }
        throw new AccountNotFoundException("Account for account id " + accountId +" was not found.");
    }

    private Account mapRowToAccount(SqlRowSet rs) {
        Account account = new Account();
        account.setAccountId(rs.getLong("account_id"));
        account.setUserId(rs.getLong("user_id"));
        account.setBalance(rs.getBigDecimal("balance"));
        return account;
    }
}
