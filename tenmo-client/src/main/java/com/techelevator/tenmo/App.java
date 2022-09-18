package com.techelevator.tenmo;

import com.techelevator.tenmo.model.*;
import com.techelevator.tenmo.services.AccountService;
import com.techelevator.tenmo.services.AuthenticationService;
import com.techelevator.tenmo.services.ConsoleService;
import com.techelevator.tenmo.services.TransferService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Objects;

public class App {

    private static final String API_BASE_URL = "http://localhost:8080/";

    private final ConsoleService consoleService = new ConsoleService();
    private final AuthenticationService authenticationService = new AuthenticationService(API_BASE_URL);

    private final AccountService accountService = new AccountService(API_BASE_URL);
    private final TransferService transferService = new TransferService(API_BASE_URL);
    private AuthenticatedUser currentUser;

    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    private void run() {
        consoleService.printGreeting();
        loginMenu();
        if (currentUser != null) {
            mainMenu();
        }
    }
    private void loginMenu() {
        int menuSelection = -1;
        while (menuSelection != 0 && currentUser == null) {
            consoleService.printLoginMenu();
            menuSelection = consoleService.promptForMenuSelection("Please choose an option: ");
            if (menuSelection == 1) {
                handleRegister();
            } else if (menuSelection == 2) {
                handleLogin();
            } else if (menuSelection != 0) {
                System.out.println("Invalid Selection");
                consoleService.pause();
            }
        }
    }

    private void handleRegister() {
        System.out.println("Please register a new user account");
        UserCredentials credentials = consoleService.promptForCredentials();
        if (authenticationService.register(credentials)) {
            System.out.println("Registration successful. You can now login.");
        } else {
            consoleService.printErrorMessage();
        }
    }

    private void handleLogin() {
        UserCredentials credentials = consoleService.promptForCredentials();
        currentUser = authenticationService.login(credentials);
        if (currentUser == null) {
            consoleService.printErrorMessage();
        }
    }

    private void mainMenu() {
        int menuSelection = -1;
        while (menuSelection != 0) {
            consoleService.printMainMenu();
            menuSelection = consoleService.promptForMenuSelection("Please choose an option: ");
            if (menuSelection == 1) {
                viewCurrentBalance();
            } else if (menuSelection == 2) {
                viewTransferHistory();
            } else if (menuSelection == 3) {
                viewPendingRequests();
            } else if (menuSelection == 4) {
                sendBucks();
            } else if (menuSelection == 5) {
                requestBucks();
            } else if (menuSelection == 0) {
                continue;
            } else {
                System.out.println("Invalid Selection");
            }
            consoleService.pause();
        }
    }

	private void viewCurrentBalance() {
        BigDecimal balance = accountService.getBalance(currentUser);
        String formattedBalance = NumberFormat.getCurrencyInstance().format(balance.doubleValue());
        System.out.println("Your current account balance is: "+ formattedBalance);
	}

	private void viewTransferHistory() {
        TransferDTO[] transferDTOS = transferService.getMyTransfers(currentUser);

        long transferId;
        while (true){
            consoleService.printTransferHistory(transferDTOS);
            transferId = consoleService.promptForLong("Please enter transfer ID to view details (0 to cancel): ");
            if (transferId>0L){
                TransferDTO transferDTO = transferService.getTransferById(currentUser, transferId);
                if (Objects.isNull(transferDTO)){
                    System.out.println("Invalid transfer ID\n");
                } else {
                    consoleService.printTransferDetails(transferDTO);
                    consoleService.pause();
                }
            } else if (transferId==0L) {
                return;
            } else {
                System.out.println("Invalid transfer ID\n");
            }
        }
	}

	private void viewPendingRequests() {
		TransferDTO[] transferDTOs=transferService.getPendingRequests(currentUser);
		long transferId;
        while(true){
            consoleService.printPendingRequests(transferDTOs);
            if(transferDTOs.length==0){
                break;
            }
            transferId=consoleService.promptForLong("Please enter transfer ID to approve/reject (0 to cancel): ");
            if(transferId>0L){
                TransferDTO transferDTO= transferService.getTransferById(currentUser, transferId);
                if(Objects.isNull(transferDTO)){
                    System.out.println("Invalid transfer ID\n");
                }
                else{
                    consoleService.printApproveOrRejectMenu();
                    int selection=consoleService.promptForMenuSelection("Please choose an option:");
                    switch(selection){
                        case 1:
                            transferService.approveOrRejectTransfer(transferId,currentUser,true);
                            System.out.println("Transfer approved.");
                            break;
                        case 2:
                            transferService.approveOrRejectTransfer(transferId,currentUser,false);
                            System.out.println("Transfer rejected.");
                            break;
                        case 0:
                            //Do nothing with transfer
                            break;
                    }
                    break;
                }
            }
            else{
                break;
            }
        }
	}

	private void sendBucks() {
        User[] otherUsers = accountService.getAllOtherUsers(currentUser);
        Account[] otherAccounts = accountService.getAllOtherAccounts(currentUser);
        consoleService.printUserSelectionMenu(otherUsers, currentUser.getUser());

        // prompt for menu choice of account/user
        int selection = consoleService.promptForInt("Enter ID of user you are sending to (0 to cancel):");
        if (selection == 0) {
            return;
        }
        User recipientUser = consoleService.findUserById(otherUsers,selection);
        if (recipientUser.getUsername() == null) {
            System.out.println("User was not found. Please try again.");
            sendBucks();
        }
        Account recipientAccount = consoleService.findAccountById(otherAccounts,selection);
        //Prompt for transfer amount
        BigDecimal transferAmount = consoleService.promptForBigDecimal("Enter amount: ");
        //Validate transfer input
        switch (transferService.validateTransferAmount(transferAmount, accountService.getBalance(currentUser))) {
            case -1:
                System.out.println("Insufficient funds for this transfer. Please try again.");
                sendBucks();
                break;
            case 0:
                System.out.println("That was not a valid dollar amount. Please try again.");
                sendBucks();
                break;
            case 1:
                //Send transfer to server
                transferService.sendBucks(  currentUser,
                                            accountService.getLoggedInAccount(currentUser),
                                            recipientAccount,
                                            transferAmount);
        }
    }

    private void requestBucks() {
        User[] otherUsers = accountService.getAllOtherUsers(currentUser);
        Account[] otherAccounts = accountService.getAllOtherAccounts(currentUser);
        consoleService.printUserSelectionMenu(otherUsers, currentUser.getUser());
        //prompt for menu choice of account/user
        int selection = consoleService.promptForInt("Enter ID of user you are requesting from (0 to cancel):");
        if (selection == 0) {
            return;
        }
        User senderUser= consoleService.findUserById(otherUsers,selection);
        if(senderUser.getUsername()==null){
            System.out.println("User was not found. Please try again.");
            requestBucks();
        }
        Account senderAccount= consoleService.findAccountById(otherAccounts,selection);
        //Prompt for request amount
        BigDecimal requestAmount= consoleService.promptForBigDecimal("Enter amount:");
        //Send request to server
        transferService.requestBucks(   currentUser,
                                        senderAccount,
                                        accountService.getLoggedInAccount(currentUser),
                                        requestAmount);
    }

}
