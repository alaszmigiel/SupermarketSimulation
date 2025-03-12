package com.example.supermarketsimulation;

public class Client implements Runnable {
    private final int id;
    private final CashRegistersMonitor monitor;

    public Client(int id, CashRegistersMonitor monitor) {
        this.id = id;
        this.monitor = monitor;
    }

    @Override
    public void run() {
        monitor.enterQueue(id); // Ustawienie się w kolejce
    }
}