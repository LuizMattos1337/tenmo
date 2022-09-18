package com.techelevator.tenmo.services;

import com.techelevator.tenmo.model.*;
import com.techelevator.util.BasicLogger;
import org.springframework.http.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class TransferService {
    private final String BASE_URL;
    private final RestTemplate restTemplate=new RestTemplate();
    private final AccountService accountService;


    enum TransferType{
        TRANSFER,
        REQUEST;
    }
    public TransferService(String url){
        BASE_URL=url;
        accountService = new AccountService(BASE_URL);
    }
    public boolean sendBucks(AuthenticatedUser user, Account senderAccount, Account recipientAccount, BigDecimal amount){
        Transfer transfer = buildTransfer(senderAccount,recipientAccount,amount,TransferType.TRANSFER);
        HttpEntity<Transfer> entity= makeTransferEntity(transfer,user.getToken());
        boolean success = false;
        try{
            restTemplate.exchange(BASE_URL+"transfer/send",HttpMethod.POST,entity,Void.class);
            success=true;
        }catch (RestClientResponseException | ResourceAccessException e) {
            BasicLogger.log(e.getMessage());
        }
        return success;
    }

    public boolean requestBucks(AuthenticatedUser user, Account senderAccount, Account recipientAccount, BigDecimal amount){
        Transfer transfer=buildTransfer(senderAccount,recipientAccount,amount,TransferType.REQUEST);
        HttpEntity<Transfer> entity = makeTransferEntity(transfer,user.getToken());
        boolean success = false;
        try{
            restTemplate.exchange(BASE_URL+"transfer/request",HttpMethod.POST,entity,Void.class);
            success=true;
        }catch(RestClientResponseException | ResourceAccessException e){
            BasicLogger.log(e.getMessage());
        }
        return success;
    }

    public boolean approveOrRejectTransfer(long transferId,AuthenticatedUser currentUser,boolean approved) {
        TransferDTO[] pendingRequests=getPendingRequests(currentUser);
        TransferDTO transferDTOToApprove=new TransferDTO();
        for (TransferDTO transferDTO:pendingRequests) {
            if(transferDTO.getTransferId()==transferId){
                transferDTOToApprove=transferDTO;
                break;
            }
        }
        User[] otherUsers= accountService.getAllOtherUsers(currentUser);
        User recipientUser= new User();
        for(User user: otherUsers){
            if(user.getUsername().equals(transferDTOToApprove.getUsernameTo())){
                recipientUser=user;
                break;
            }
        }
        Account[] otherAccounts= accountService.getAllOtherAccounts(currentUser);
        Account recipientAccount=new Account();
        for(Account account:otherAccounts){
            if(Objects.equals(account.getUserId(), recipientUser.getUserId())){
                recipientAccount=account;
            }
        }
        Transfer transfer=buildTransfer(transferId, accountService.getLoggedInAccount(currentUser),recipientAccount,transferDTOToApprove.getAmount(),approved);
        HttpEntity<Transfer> entity= makeTransferEntity(transfer,currentUser.getToken());
        boolean success=false;
        try{
            restTemplate.exchange(BASE_URL+"myAccount/requests/pending",HttpMethod.PUT,entity,Void.class);
            success=true;
        }catch (RestClientResponseException | ResourceAccessException e) {
            BasicLogger.log(e.getMessage());
        }
        return success;
    }

    public int validateTransferAmount(BigDecimal transferAmount, BigDecimal accountBalance){
        if (transferAmount.compareTo(BigDecimal.valueOf(0))<=0){
            return 0;
        };
        if (transferAmount.compareTo(accountBalance)>=0){
            return -1;
        }
        return 1;
    }

    public TransferDTO[] getMyTransfers(AuthenticatedUser user){
        TransferDTO[] transferDTOS = null;
        try {
            ResponseEntity<TransferDTO[]> response =
                    restTemplate.exchange(
                            BASE_URL+"/myAccount/transfers",
                            HttpMethod.GET, makeAuthEntity(user.getToken()),
                            TransferDTO[].class);
            transferDTOS = response.getBody();
        } catch (RestClientResponseException | ResourceAccessException e){
            BasicLogger.log(e.getMessage());
        }
        return transferDTOS;
    }

    public TransferDTO[] getPendingRequests(AuthenticatedUser user) {
        TransferDTO[] transferDTOs=null;
        try{
            ResponseEntity<TransferDTO[]>response =
                    restTemplate.exchange(
                            BASE_URL+"myAccount/requests",
                            HttpMethod.GET, makeAuthEntity(user.getToken()),
                            TransferDTO[].class);
            transferDTOs=response.getBody();
        } catch (RestClientResponseException | ResourceAccessException e){
            BasicLogger.log(e.getMessage());
        }
        return transferDTOs;
    }

    public TransferDTO getTransferById(AuthenticatedUser user, Long transferId){
        TransferDTO transferDTO=null;
        try {
            ResponseEntity<TransferDTO> response =
                    restTemplate.exchange(
                            BASE_URL+"myAccount/transfers/"+transferId,
                            HttpMethod.GET,makeAuthEntity(user.getToken()),
                            TransferDTO.class);
            transferDTO=response.getBody();
        } catch (RestClientResponseException | ResourceAccessException e){
            BasicLogger.log(e.getMessage());
        }

        return transferDTO;
    }

    private HttpEntity<Void> makeAuthEntity(String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        return new HttpEntity<>(headers);
    }

    private HttpEntity<Transfer> makeTransferEntity(Transfer transfer, String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(authToken);
        return new HttpEntity<>(transfer,headers);
    }

    private Transfer buildTransfer(Account senderAccount, Account recipientAccount, BigDecimal amount, TransferType transferType){
        Transfer transfer = new Transfer();
        switch(transferType){
            case TRANSFER:
                transfer.setTransferTypeId(2L);
                transfer.setTransferStatusId(2L);
                break;
            case REQUEST:
                transfer.setTransferTypeId(1L);
                transfer.setTransferStatusId(1L);
                break;
        }
        transfer.setSenderAccount(senderAccount);
        transfer.setRecipientAccount(recipientAccount);
        transfer.setAmount(amount);
        return transfer;
    }

    private Transfer buildTransfer(Long transferId, Account senderAccount, Account recipientAccount, BigDecimal amount, boolean approved){
        Transfer transfer = new Transfer();
        transfer.setTransferTypeId(1L);
        transfer.setTransferId(transferId);
        if(approved) {
            transfer.setTransferStatusId(2L);
        }
        else{
            transfer.setTransferStatusId(3L);
        }
        transfer.setSenderAccount(senderAccount);
        transfer.setRecipientAccount(recipientAccount);
        transfer.setAmount(amount);
        return transfer;
    }
}
