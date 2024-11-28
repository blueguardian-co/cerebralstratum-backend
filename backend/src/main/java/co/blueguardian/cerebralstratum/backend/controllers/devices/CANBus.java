package co.blueguardian.cerebralstratum.backend.controllers.devices;

public class CANBus {
    public String payload;

    public CANBus(){
    }

    public CANBus(String payload){
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "-- CANBus Message --" +
               "Payload: " + payload;
    }
}
