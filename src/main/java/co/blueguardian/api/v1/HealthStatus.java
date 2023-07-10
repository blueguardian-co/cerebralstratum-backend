package co.blueguardian.api.v1;

public class HealthStatus {

    public String overall;
    public String backend;

    public HealthStatus() {
    }

    public HealthStatus(String overallStatus, String backendStatus) {
        this.overall = overallStatus;
        this.backend = backendStatus;
    }
}