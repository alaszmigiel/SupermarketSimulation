package com.example.supermarketsimulation;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class CashRegistersMonitor {
    private final SupermarketController controller;
    private final List<Queue<Integer>> queues; // Lista kolejek dla każdej kasy
    private final List<ReentrantLock> locks; // Zamki dla każdej kolejki
    private final List<Boolean> openStatus; // Status otwarcia każdej kasy
    private final Queue<Integer> operationQueue = new LinkedList<>(); // Kolejka operacji
    private final ReentrantLock globalLock = new ReentrantLock(); // Globalny zamek
    private boolean isOperationActive = false; // Flaga wskazująca czy operacja jest aktywna
    private int totalClients; // Całkowita liczba klientów do obsłużenia

    public CashRegistersMonitor(int numRegisters, int numClients, SupermarketController controller) {
        queues = new ArrayList<>(numRegisters);
        locks = new ArrayList<>(numRegisters);
        openStatus = new ArrayList<>(numRegisters);
        for (int i = 0; i < numRegisters; i++) {
            queues.add(new LinkedList<>());
            ReentrantLock lock = new ReentrantLock();
            locks.add(lock);
            openStatus.add(true);
        }
        totalClients = numClients;
        this.controller = controller;
    }

    // Wejście klienta do kolejki
    public void enterQueue(int clientId) {
        globalLock.lock();
        try {
            int selectedQueue = chooseShortestQueue();
            locks.get(selectedQueue).lock();
            try {
                queues.get(selectedQueue).add(clientId);
                printQueueStatus("Klient " + clientId + " ustawił się w kolejce do kasy " + (selectedQueue + 1));
                totalClients--;
                controller.createClientCircle(clientId);
                controller.moveClientToCashRegister(clientId, selectedQueue + 1);
            } finally {
                locks.get(selectedQueue).unlock();
            }
        } finally {
            globalLock.unlock();
        }
    }

    // Pobranie klienta z kolejki
    public Integer getClientFromQueue(int registerId) {
        int queueIndex = registerId - 1;
        locks.get(queueIndex).lock();
        try {
            if (queues.get(queueIndex).isEmpty()) {
                return null;
            }
            return queues.get(queueIndex).peek();
        } finally {
            locks.get(queueIndex).unlock();
        }
    }

    // Rozpoczęcie obsługi klienta
    public void startServing(int clientId, int registerId) {
        System.out.printf("Kasjer w kasie %d rozpoczął obsługę klienta %d.\n", registerId, clientId);
        controller.updateClientCircleColor(clientId, Color.GREEN);
    }

    //Zakończenie obsługi klienta
    public void finishServing(int registerId, int clientId) {
        int queueIndex = registerId - 1;
        globalLock.lock();
        try {
            locks.get(queueIndex).lock();
            try {
                queues.get(queueIndex).poll();
                controller.updateClientCircleColor(clientId, Color.BLACK);
                controller.removeClientCircle(clientId);
                controller.shiftClientCirclesUp(registerId);
                printQueueStatus("Kasjer w kasie " + registerId + " zakończył obsługę klienta " + clientId);
            } finally {
                locks.get(queueIndex).unlock();
            }
        } finally {
            globalLock.unlock();
        }
    }

    // Sprawdzenie czy są jeszcze klienci do obsłużenia
    public boolean hasMoreClients() {
        globalLock.lock();
        try {
            return totalClients > 0;
        } finally {
            globalLock.unlock();
        }
    }

    // Sprawdzenie czy kolejka dla danej kasy jest pusta
    public boolean isQueueEmpty(int registerId) {
        int queueIndex = registerId - 1;
        locks.get(queueIndex).lock();
        try {
            return queues.get(queueIndex).isEmpty();
        } finally {
            locks.get(queueIndex).unlock();
        }
    }

    // Zamknięcie kasy
    public void closeRegister(int registerId) {
        int queueIndex = registerId - 1;
        globalLock.lock();
        try {
            openStatus.set(queueIndex, false);
            controller.updateCashRegisterColor(registerId, Color.ORANGE);
            System.out.printf("Kasa %d się zamyka, przestaje przyjmować klientów.\n", registerId);
        } finally {
            globalLock.unlock();
        }
    }

    // Otwarcie kasy
    public void openRegister(int registerId) {
        int queueIndex = registerId - 1;
        globalLock.lock();
        try {
            Integer operationPeek = operationQueue.peek();
            if (isOperationActive) {
                isOperationActive = false;
            }
            if(operationPeek != null) {
                operationQueue.poll();
            }
            openStatus.set(queueIndex, true);
            controller.updateCashRegisterColor(registerId, Color.GREEN);
            System.out.printf("Kasa %d się otwiera.\n", registerId);
        } finally {
            globalLock.unlock();
        }
    }

    // Prośba o przerwę
    public void requestBreak(int registerId) {
        globalLock.lock();
        try {
            operationQueue.add(registerId);
            controller.updateCashRegisterColor(registerId, Color.YELLOW);
            System.out.printf("Kasjer w kasie %d chce przerwę\n", registerId);
        } finally {
            globalLock.unlock();
        }
    }

    // Prośba o zmianę
    public void requestChange(int registerId) {
        globalLock.lock();
        try {
            operationQueue.add(registerId);
            controller.updateCashRegisterColor(registerId, Color.BLUE);
            System.out.printf("Kasjer w kasie %d chce zmianę\n", registerId);
        } finally {
            globalLock.unlock();
        }
    }

    // Przejście na przerwę
    public void goOnBreak(int registerId) {
        globalLock.lock();
        try {
            isOperationActive = true;
            System.out.printf("Kasjer w kasie %d jest na przerwie.\n", registerId);
            controller.updateCashRegisterColor(registerId, Color.RED);
        } finally {
            globalLock.unlock();
        }
    }

    // Zmiana kasjera
    public void changeCashier(int registerId) {
        globalLock.lock();
        try {
            isOperationActive = true;
            controller.removeCashierCircle(registerId);
            System.out.printf("W kasie %d zmieniany jest kasjer.\n", registerId);
            controller.updateCashRegisterColor(registerId, Color.LIGHTBLUE);
        } finally {
            globalLock.unlock();
        }
    }

    //Sprawdzenie czy operacja jest dozwolona dla danej kasy
    public boolean isOperationAllowed(int registerId) {
        globalLock.lock();
        try {
            Integer operationPeek = operationQueue.peek();
            if (!isOperationActive && operationPeek != null && operationPeek == registerId) {
                isOperationActive = true;
                return true;
            } else {
                return false;
            }
        } finally {
            globalLock.unlock();
        }
    }

    // Wybór najkrótszej kolejki
    public int chooseShortestQueue() {
        int shortestQueueIndex = -1;
        int minSize = Integer.MAX_VALUE;

        for (int i = 0; i < queues.size(); i++) {
            if (openStatus.get(i) && queues.get(i).size() < minSize) {
                shortestQueueIndex = i;
                minSize = queues.get(i).size();
            }
        }
        return shortestQueueIndex;
    }

    // Wypisanie statusu kolejek
    private void printQueueStatus(String message) {
        StringBuilder status = new StringBuilder(message + " [");
        for (int i = 0; i < queues.size(); i++) {
            status.append("k").append(i + 1).append(" - ").append(queues.get(i).size());
            if (i < queues.size() - 1) {
                status.append(", ");
            }
        }
        status.append("]");
        System.out.println(status);
    }

    // Pobranie id klientów z kolejki do danej kasy
    public List<Integer> getQueueClientIds(int registerId) {
        int queueIndex = registerId - 1;
        locks.get(queueIndex).lock();
        try {
            return new ArrayList<>(queues.get(queueIndex));
        } finally {
            locks.get(queueIndex).unlock();
        }
    }
}