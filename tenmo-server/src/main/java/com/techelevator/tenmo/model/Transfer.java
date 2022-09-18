package com.techelevator.tenmo.model;

import javax.validation.constraints.*;
import java.math.BigDecimal;

public class Transfer {
    private Long transferId;
    @Min(value=1)
    @Max(value=2)
    private Long transferTypeId;
    @Min(value=1)
    @Max(value=3)
    private Long transferStatusId;
    @NotNull
    private Account senderAccount;
    @NotNull
    private Account recipientAccount;
    @DecimalMin(value="0.00",inclusive=false)
    //TODO Decide on sensible limit for integer portion of a transfer
    @Digits(integer=9001,fraction=2)
    private BigDecimal amount;

    public Account getSenderAccount(){
        return senderAccount;
    }

    public void setSenderAccount(Account senderAccount){
        this.senderAccount = senderAccount;
    }

    public Account getRecipientAccount(){
        return recipientAccount;
    }

    public void setRecipientAccount(Account recipientAccount){
        this.recipientAccount = recipientAccount;
    }

    public Long getTransferId(){
        return transferId;
    }

    public void setTransferId(Long transferId){
        this.transferId = transferId;
    }

    public Long getTransferTypeId(){
        return transferTypeId;
    }

    public void setTransferTypeId(Long transferTypeId){
        this.transferTypeId = transferTypeId;
    }

    public Long getTransferStatusId(){
        return transferStatusId;
    }

    public void setTransferStatusId(Long transferStatusId){
        this.transferStatusId = transferStatusId;
    }

    public BigDecimal getAmount(){
        return amount;
    }

    public void setAmount(BigDecimal amount){
        this.amount = amount;
    }
}
