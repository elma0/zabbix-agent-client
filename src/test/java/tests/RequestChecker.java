package tests;


import java.util.concurrent.atomic.AtomicBoolean;

public class RequestChecker {
    private AtomicBoolean atomicBoolean = new AtomicBoolean(true);

    public RequestChecker() {

    }

    public Boolean isAllRequestsOK() {
        return atomicBoolean.get();
    }

    public void setRequestFailed() {
        atomicBoolean.set(false);
    }
}