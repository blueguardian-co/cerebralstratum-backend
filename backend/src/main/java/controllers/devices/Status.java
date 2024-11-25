package controllers.devices;

public class Status {
    public String summary;
    public String overall;
    public float battery;

    public Status(){
    }

    public Status(String summary, String overall, float battery){
        this.summary = summary;
        this.overall = overall;
        this.battery = battery;
    }

    @Override
    public String toString() {
        return "-- Status --" +
               "Summary: " + summary + '\n' +
               "Overall: " + overall + '\n' +
               "Battery: " + battery;
    }
}
