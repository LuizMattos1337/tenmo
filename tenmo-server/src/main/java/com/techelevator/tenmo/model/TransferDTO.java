package com.techelevator.tenmo.model;

import javax.validation.constraints.*;
import java.math.BigDecimal;

public class TransferDTO implements Comparable<TransferDTO>{
    private Long transferId;
    @NotEmpty
    private String usernameFrom;
    @NotEmpty
    private String usernameTo;
    @Positive
    private Long senderAccountId;
    @Positive
    private Long recipientAccountId;
    @Min(value=1)
    @Max(value=2)
    private Long transferTypeId;
    @Min(value=1)
    @Max(value=3)
    private Long transferStatusId;
    @DecimalMin(value="0.0",inclusive = false)
    //TODO Decide on sensible limit for integer portion of a transfer
    @Digits(integer=9001,fraction=2)
    private BigDecimal amount;
    private boolean toPrincipal;

    public boolean isToPrincipal(){
        return toPrincipal;
    }

    public void setToPrincipal(boolean toPrincipal){
        this.toPrincipal = toPrincipal;
    }

    public Long getSenderAccountId(){
        return senderAccountId;
    }

    public void setSenderAccountId(Long senderAccountId){
        this.senderAccountId = senderAccountId;
    }

    public Long getRecipientAccountId(){
        return recipientAccountId;
    }

    public void setRecipientAccountId(Long recipientAccountId){
        this.recipientAccountId = recipientAccountId;
    }

    public Long getTransferId(){
        return transferId;
    }

    public void setTransferId(Long transferId){
        this.transferId = transferId;
    }

    public String getUsernameFrom(){
        return usernameFrom;
    }

    public void setUsernameFrom(String usernameFrom){
        this.usernameFrom = usernameFrom;
    }

    public String getUsernameTo(){
        return usernameTo;
    }

    public void setUsernameTo(String usernameTo){
        this.usernameTo = usernameTo;
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

    @Override
    public int compareTo(TransferDTO otherTransferDTO){
        if(this.getTransferId()<otherTransferDTO.getTransferId()){
            return -1;
        }
        else if(this.getTransferId()> otherTransferDTO.getTransferId()){
            return 1;
        }
        return 0;
    }
}
