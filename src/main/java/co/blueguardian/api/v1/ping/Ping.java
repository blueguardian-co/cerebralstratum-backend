package co.blueguardian.api.v1.ping;

public class Ping {

    public String ping;

    public Ping() {
    }

    public Ping(String response) {
        this.ping = response;
    }
}