package com.techelevator.tenmo.controller;

import com.techelevator.tenmo.dao.AccountDao;
import com.techelevator.tenmo.dao.TransferDao;
import com.techelevator.tenmo.dao.UserDao;
import com.techelevator.tenmo.exception.AccountNotFoundException;
import com.techelevator.tenmo.exception.InvalidTransferException;
import com.techelevator.tenmo.exception.TransferNotFoundException;
import com.techelevator.tenmo.exception.UnauthorizedException;
import com.techelevator.tenmo.model.Account;
import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.TransferDTO;
import com.techelevator.tenmo.model.User;
import com.techelevator.util.BasicLogger;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Objects;

@RestController
@PreAuthorize("isAuthenticated()")
public class TransferController {
    private TransferDao transferDao;
    private AccountDao accountDao;
    private UserDao userDao;

    public TransferController(TransferDao transferDao, AccountDao accountDao, UserDao userDao){
        this.transferDao=transferDao;
        this.accountDao=accountDao;
        this.userDao=userDao;
    }

    @PostMapping(path="/transfer/send")
    @ResponseStatus(HttpStatus.CREATED)
    public Transfer transferSend(@RequestBody @Valid Transfer transfer, Principal principal){
        if(validTransfer(transfer,principal)){
            return transferDao.createApprovedTransfer(transfer);
        }
        else{
            throw new InvalidTransferException("Sorry, that transfer is invalid.");
        }
    }

    @PostMapping(path="/transfer/request")
    @ResponseStatus(HttpStatus.CREATED)
    public Transfer transferRequest(@RequestBody @Valid Transfer transfer, Principal principal){
        if(validTransfer(transfer,principal)){
            return transferDao.createRequestedTransfer(transfer);
        }
        else{
            throw new InvalidTransferException("Sorry, that transfer request is invalid.");
        }
    }

    @PutMapping(path="myAccount/requests/pending")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveOrRejectTransfer(@RequestBody @Valid Transfer transfer, Principal principal){
        if((validApproval(transfer, principal))||(transfer.getTransferStatusId()==3)){
            transferDao.approveOrRejectPendingTransfer(transfer);
        }
        else if(!validApproval(transfer,principal)&&transfer.getTransferStatusId()==2){
            throw new InvalidTransferException("Sorry, you are unable to approve this transfer.");
        }
    }

    @GetMapping("/myAccount/transfers")
    public List<TransferDTO> getMyTransfers(Principal principal){
        User myUser = userDao.findByUsername(principal.getName());
        return transferDao.getTransfersByUser(myUser);
    }

    @GetMapping("myAccount/requests")
    public List<TransferDTO> getMyPendingRequests(Principal principal){
        User myUser=userDao.findByUsername(principal.getName());
        return transferDao.getRequestsByUser(myUser);
    }

    @GetMapping("/myAccount/transfers/{transferId}")
    public TransferDTO getTransferById(@PathVariable Long transferId,Principal principal) throws UnauthorizedException {
        TransferDTO transferDTO=transferDao.getTransferById(transferId);

        if (Objects.isNull(transferDTO)){
            throw new TransferNotFoundException("Transfer not found");
        }
        transferDTO.setUsernameFrom(userDao.findUsernameByAccountId(transferDTO.getSenderAccountId()));
        transferDTO.setUsernameTo(userDao.findUsernameByAccountId(transferDTO.getRecipientAccountId()));

        if(!transferDTO.getUsernameFrom().equals(principal.getName())&&!transferDTO.getUsernameTo().equals(principal.getName())) {
            throw new UnauthorizedException("You are not authorized to view transfers in which you are not involved.");
        }

        return transferDTO;
    }

    private boolean validApproval(Transfer transfer,Principal principal) {
        TransferDTO serverTransfer=transferDao.getTransferById(transfer.getTransferId());
        if (Objects.equals(transfer.getSenderAccount().getAccountId(), serverTransfer.getSenderAccountId()) &&
                Objects.equals(transfer.getRecipientAccount().getAccountId(), serverTransfer.getRecipientAccountId()) &&
                Objects.equals(transfer.getAmount(), serverTransfer.getAmount()) &&
                transfer.getTransferTypeId() == 1 &&
                transfer.getTransferStatusId() == 2 &&
                serverTransfer.getTransferStatusId() == 1) {
                return validTransfer(transfer, principal);
            }
        return false;
    }

    private boolean validTransfer(Transfer transfer, Principal principal){
        if(transfer.getTransferStatusId()==2) {
            return sufficientBalance(transfer) && validAccountsForTransfer(transfer, principal);
        }
        else if(transfer.getTransferStatusId()==1){
            return validAccountsForRequest(transfer,principal);
        }
        return false;
    }
    private boolean sufficientBalance(Transfer transfer) {
        Account senderAccount;
        try {
            senderAccount = accountDao.findAccountByAccountId(transfer.getSenderAccount().getAccountId());
        } catch (AccountNotFoundException e) {
            BasicLogger.log("findAccountByAccountId: "+e.getMessage());
            return false;
        }

        if (senderAccount == null) {
            BasicLogger.log("sufficientBalance() failure: senderAccount == null");
            return false;
        }

        return senderAccount.getBalance().compareTo(transfer.getAmount()) >= 0;
    }
    private boolean validAccountsForTransfer(Transfer transfer, Principal principal){
        Account senderAccount = null;
        Account recipientAccount = null;
        Account principalAccount = null;

        try {
            senderAccount = accountDao.findAccountByAccountId(transfer.getSenderAccount().getAccountId());
            recipientAccount = accountDao.findAccountByAccountId(transfer.getRecipientAccount().getAccountId());
            principalAccount = accountDao.findAccountByUsername(principal.getName());
        } catch (AccountNotFoundException e) {
            BasicLogger.log(e.getMessage());
        }

        if (senderAccount == null || recipientAccount == null){
            BasicLogger.log("senderAccount or recipientAccount are null");
            return false;
        }
        if (!senderAccount.equals(principalAccount)){
            BasicLogger.log("senderAccount != principalAccount");
            return false;
        }
        if (senderAccount.equals(recipientAccount)){
            BasicLogger.log("senderAccount == recipientAccount");
            return false;
        }

        return true;
    }
    private boolean validAccountsForRequest(Transfer transfer, Principal principal){
        Account requesterAccount = null;
        Account requesteeAccount = null;
        Account principalAccount = null;

        try{
            requesterAccount = accountDao.findAccountByAccountId(transfer.getRecipientAccount().getAccountId());
            requesteeAccount = accountDao.findAccountByAccountId(transfer.getSenderAccount().getAccountId());
            principalAccount = accountDao.findAccountByUsername(principal.getName());
        }catch(AccountNotFoundException e){
            BasicLogger.log(e.getMessage());
        }
        if(requesterAccount == null || requesteeAccount == null){
            BasicLogger.log("requesterAccount or requesteeAccount are null");
            return false;
        }
        if(!requesterAccount.equals(principalAccount)){
            BasicLogger.log("requesterAccount != principalAccount");
            return false;
        }
        if(requesteeAccount.equals(requesterAccount)){
            BasicLogger.log("requesteeAccount==requesterAccount");
            return false;
        }
        return true;
    }

}