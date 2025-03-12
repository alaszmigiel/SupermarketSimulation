package com.example.supermarketsimulation;

import javafx.scene.paint.Color;
import java.util.concurrent.ThreadLocalRandom;

public class Cashier implements Runnable {
    private final int cashierId;
    private final int registerId;
    private final CashRegistersMonitor monitor;
    private final SupermarketController controller;
    private final int minUseTime;
    private final int maxUseTime;
    private long totalServiceTime;
    private boolean breakRequested; // Flaga prośby o przerwę
    private boolean changeRequested; // Flaga prośby o zmianę
    private boolean afterBreak; // Flaga czy kasjer jest juz po przerwie
    private int simulationTime;

    public Cashier(int cashierId, int registerId, CashRegistersMonitor monitor, int minUseTime, int maxUseTime, SupermarketController controller) {
        this.cashierId = cashierId;
        this.registerId = registerId;
        this.monitor = monitor;
        this.minUseTime = minUseTime;
        this.maxUseTime = maxUseTime;
        this.controller = controller;
        this.totalServiceTime = 0;
        this.breakRequested = false;
        this.changeRequested = false;
        this.afterBreak = false;
    }

    @Override
    public void run() {
        try {
            simulationTime = controller.getSimulationTimeMillis();
            while (!Thread.currentThread().isInterrupted()) {
                handleOperations();
                Integer clientId = monitor.getClientFromQueue(registerId);
                if (clientId == null) {
                    handleOperations();
                    if (!monitor.hasMoreClients() && monitor.isQueueEmpty(registerId)) {
                        endCashierWork();
                        break;
                    }
                } else {
                    serveClient(clientId);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Operacje kasjera (przerwa, zmiana)
    private void handleOperations() throws InterruptedException {
        askForOperation();
        if ((changeRequested || breakRequested) && monitor.isOperationAllowed(registerId) && monitor.hasMoreClients()) {
            monitor.closeRegister(registerId);
            Integer clientId;
            while ((clientId = monitor.getClientFromQueue(registerId)) != null) {
                serveClient(clientId);
                if (!monitor.hasMoreClients()) {
                    controller.updateCashRegisterColor(registerId, Color.LIGHTGRAY);
                }
            }
            if (monitor.hasMoreClients()) {
                if (breakRequested && !afterBreak) {
                    takeBreak();
                } else if (changeRequested) {
                    changeCashier();
                }
            }
        } else {
            if (!monitor.hasMoreClients()) {
                controller.updateCashRegisterColor(registerId, Color.LIGHTGRAY);
            }
        }
    }

    // Obsługa klienta
    private void serveClient(Integer clientId) throws InterruptedException {
        Thread.sleep(simulationTime);
        monitor.startServing(clientId, registerId);
        long serviceTime = ThreadLocalRandom.current().nextInt(minUseTime, maxUseTime + 1);
        Thread.sleep(serviceTime);
        totalServiceTime += serviceTime;
        monitor.finishServing(registerId, clientId);
    }

    // Przerwa
    private void takeBreak() throws InterruptedException {
        monitor.goOnBreak(registerId);
        Thread.sleep(simulationTime);
        System.out.printf("Kasjer %d przy kasie %d zakończył przerwę i wraca do pracy.\n", cashierId, registerId);
        monitor.openRegister(registerId);
        afterBreak = true;
        totalServiceTime = 0;
    }

    // Zmiana
    private void changeCashier() throws InterruptedException {
        monitor.changeCashier(registerId);
        Thread.sleep(1000);
        controller.replaceCashier(registerId);
    }

    // Prośba o przerwę/zmianę
    private void askForOperation() {
        if (totalServiceTime >= 6000 && !breakRequested && monitor.hasMoreClients()) {
            breakRequested = true;
            monitor.requestBreak(registerId);
        } else if (totalServiceTime >= 6000 && !changeRequested && monitor.hasMoreClients() && afterBreak) {
            changeRequested = true;
            monitor.requestChange(registerId);
        }
    }

    // Koniec pracy
    private void endCashierWork() throws InterruptedException {
        Thread.sleep(1000);
        System.out.printf("Kasjer %d przy kasie %d zatrzymuje się, ponieważ wszyscy klienci zostali obsłużeni.\n", cashierId, registerId);
        controller.updateCashRegisterColor(registerId, Color.DIMGRAY);
    }

    public int getRegisterId() {
        return registerId;
    }

    public int getCashierId() {
        return cashierId;
    }
}
