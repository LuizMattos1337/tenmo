package com.techelevator.tenmo.dao;


import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.TransferDTO;
import com.techelevator.tenmo.model.User;
import com.techelevator.util.BasicLogger;
import org.intellij.lang.annotations.Language;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class JdbcTransferDao implements TransferDao{

    private JdbcTemplate jdbcTemplate;

    public JdbcTransferDao(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate=jdbcTemplate;
    }

    @Override
    public Transfer createApprovedTransfer(Transfer transfer) {
        @Language("SQL")
        String sql =
                "BEGIN TRANSACTION;" +
                "UPDATE account SET balance = balance - ? WHERE account_id=?;" +
                "UPDATE account SET balance = balance + ? WHERE account_id=?;" +
                "INSERT INTO transfer(transfer_type_id, transfer_status_id, account_from, account_to, amount) VALUES (?,?,?,?,?) RETURNING transfer_id;" +
                "COMMIT;";
        Transfer newTransfer=null;
        try{
            newTransfer=jdbcTemplate.queryForObject(sql,
                                                    Transfer.class,
                                                    transfer.getAmount(),
                                                    transfer.getSenderAccount().getAccountId(),
                                                    transfer.getAmount(),
                                                    transfer.getRecipientAccount().getAccountId(),
                                                    transfer.getTransferTypeId(),
                                                    transfer.getTransferStatusId(),
                                                    transfer.getSenderAccount().getAccountId(),
                                                    transfer.getRecipientAccount().getAccountId(),
                                                    transfer.getAmount().doubleValue());
        }catch(DataAccessException e){
            BasicLogger.log(e.getMessage());
        }
        return newTransfer;
    }

    @Override
    public Transfer createRequestedTransfer(Transfer transfer){
        @Language("SQL")
        String sql=
                "INSERT INTO transfer(transfer_type_id, transfer_status_id, account_from, account_to, amount) VALUES (?,?,?,?,?) RETURNING transfer_id;";
        Transfer newTransfer=null;
        try{
            newTransfer=jdbcTemplate.queryForObject(sql,
                                                    Transfer.class,
                                                    transfer.getTransferTypeId(),
                                                    transfer.getTransferStatusId(),
                                                    transfer.getSenderAccount().getAccountId(),
                                                    transfer.getRecipientAccount().getAccountId(),
                                                    transfer.getAmount().doubleValue());
        }catch(DataAccessException e){
            BasicLogger.log(e.getMessage());
        }
        return newTransfer;
    }

    @Override
    public int approveOrRejectPendingTransfer(Transfer transfer){
        @Language("SQL")
        String sql="";
        int updateStatus=-1;
        try {
            if (transfer.getTransferStatusId() == 2) {
                sql =
                    "BEGIN TRANSACTION;" +
                    "UPDATE account SET balance = balance - ? WHERE account_id=?;" +
                    "UPDATE account SET balance = balance + ? WHERE account_id=?;" +
                    "UPDATE transfer SET transfer_status_id=2 WHERE transfer_id=?;" +
                    "COMMIT;";
                updateStatus=jdbcTemplate.update(sql,
                                    transfer.getAmount(), transfer.getSenderAccount().getAccountId(),
                                    transfer.getAmount(), transfer.getRecipientAccount().getAccountId(),
                                    transfer.getTransferId());
            } else if (transfer.getTransferStatusId() == 3) {
                sql =
                    "UPDATE transfer SET transfer_status_id=3 WHERE transfer_id=?;";
                updateStatus=jdbcTemplate.update(sql,transfer.getTransferId());
            }
        } catch (DataAccessException e){
            throw new RuntimeException(e);
        }
        return updateStatus;
    }

    @Override
    public List<TransferDTO> getTransfersByUser(User user) {
        List<TransferDTO> transferDTOs=new ArrayList<>();
        @Language("SQL")
        String sql="SELECT transfer.transfer_id,tenmo_user.username,transfer.amount,transfer.transfer_status_id " +
                "FROM transfer " +
                "JOIN account ON transfer.account_to = account.account_id " +
                "JOIN tenmo_user ON account.user_id = tenmo_user.user_id " +
                "WHERE transfer.account_from=(SELECT account_id FROM account JOIN tenmo_user ON account.user_id=tenmo_user.user_id WHERE tenmo_user.user_id=?)";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, user.getUserId());
        while(results.next()){
            transferDTOs.add(mapRowToTransferDTOSent(results,user));
        }
        sql="SELECT transfer.transfer_id,tenmo_user.username,transfer.amount,transfer.transfer_status_id " +
                "FROM transfer " +
                "JOIN account ON transfer.account_from = account.account_id " +
                "JOIN tenmo_user ON account.user_id = tenmo_user.user_id " +
                "WHERE transfer.account_to=(SELECT account_id FROM account JOIN tenmo_user ON account.user_id=tenmo_user.user_id WHERE tenmo_user.user_id=?)";
        results = jdbcTemplate.queryForRowSet(sql, user.getUserId());
        while(results.next()){
            transferDTOs.add(mapRowToTransferDTOReceived(results,user));
        }
        Collections.sort(transferDTOs);
        return transferDTOs;
    }

    @Override
    public List<TransferDTO> getRequestsByUser(User user){
        List<TransferDTO> transferDTOs=new ArrayList<>();
        @Language("SQL")
        String sql= "SELECT transfer.transfer_id,tenmo_user.username,transfer.amount,transfer.transfer_status_id,transfer_type_id " +
                    "FROM transfer " +
                    "JOIN account ON transfer.account_to = account.account_id " +
                    "JOIN tenmo_user ON account.user_id = tenmo_user.user_id " +
                    "WHERE transfer.account_from=(SELECT account_id FROM account JOIN tenmo_user ON account.user_id=tenmo_user.user_id WHERE tenmo_user.user_id=?)" +
                    "AND transfer_status_id='1'";
        SqlRowSet results=jdbcTemplate.queryForRowSet(sql,user.getUserId());
        while(results.next()){
            transferDTOs.add(mapRowToTransferDTORequested(results,user));
        }
        Collections.sort(transferDTOs);
        return transferDTOs;
    }

    @Override
    public TransferDTO getTransferById(Long transferId){
        @Language("SQL")
        String sql="SELECT t.transfer_id, t.transfer_type_id, t.transfer_status_id, t.amount, t.account_from, t.account_to " +
                "FROM transfer t " +
                "WHERE t.transfer_id = ?;";
        SqlRowSet result=jdbcTemplate.queryForRowSet(sql,transferId);
        if(result.next()) {
            return mapRowToIncompleteTransferDTO(result);
        }
        return null;
    }

    private TransferDTO mapRowToTransferDTOSent(SqlRowSet rs,User user) {
        TransferDTO transferDTO=new TransferDTO();
        transferDTO.setTransferId(rs.getLong("transfer_id"));
        transferDTO.setUsernameFrom(user.getUsername());
        transferDTO.setUsernameTo(rs.getString("username"));
        transferDTO.setAmount(rs.getBigDecimal("amount"));
        transferDTO.setToPrincipal(true);
        return transferDTO;
    }

    private TransferDTO mapRowToTransferDTOReceived(SqlRowSet rs, User user) {
        TransferDTO transferDTO=new TransferDTO();
        transferDTO.setTransferId(rs.getLong("transfer_id"));
        transferDTO.setUsernameFrom(rs.getString("username"));
        transferDTO.setUsernameTo(user.getUsername());
        transferDTO.setAmount(rs.getBigDecimal("amount"));
        return transferDTO;
    }

    private TransferDTO mapRowToTransferDTORequested(SqlRowSet rs, User user) {
        TransferDTO transferDTO=new TransferDTO();
        transferDTO.setTransferId(rs.getLong("transfer_id"));
        transferDTO.setTransferStatusId(rs.getLong("transfer_status_id"));
        transferDTO.setTransferTypeId(rs.getLong("transfer_type_id"));
        transferDTO.setUsernameFrom(user.getUsername());
        transferDTO.setUsernameTo(rs.getString("username"));
        transferDTO.setAmount(rs.getBigDecimal("amount"));
        return transferDTO;
    }

    private TransferDTO mapRowToIncompleteTransferDTO(SqlRowSet rs){
        TransferDTO transferDTO=new TransferDTO();
        transferDTO.setTransferId(rs.getLong("transfer_id"));
        transferDTO.setTransferTypeId(rs.getLong("transfer_type_id"));
        transferDTO.setTransferStatusId(rs.getLong("transfer_status_id"));
        transferDTO.setAmount(rs.getBigDecimal("amount"));
        transferDTO.setSenderAccountId(rs.getLong("account_from"));
        transferDTO.setRecipientAccountId(rs.getLong("account_to"));
        return transferDTO;
    }
}
