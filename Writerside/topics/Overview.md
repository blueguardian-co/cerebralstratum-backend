# Overview

The CEREBRAL STRATUM backend is made up of several components:
1. User Interaction
    1. REST API
    2. Websockets
2. Device Interaction
    1. Classifier - classifies incoming data, such as:
        1. Location
        2. CAN bus
    2. MQTT
3. Database - PostgreSQL
4. Service Message Bus - Kafka
5. Platform - Red Hat OpenShift Container Platform