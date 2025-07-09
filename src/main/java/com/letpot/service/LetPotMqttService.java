package com.letpot.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LetPotMqttService {

    private static final String BROKER_HOST = "broker.letpot.net";
    private static final int BROKER_PORT = 443;
    private static final String WEBSOCKET_PATH = "/mqttwss";
    private static final Logger log = LoggerFactory.getLogger(LetPotMqttService.class);
    private static final int MTU = 128;

    private MqttClient mqttClient;
    private int messageId = 0;
    private String currentUserEmail;
    private String currentUserId;

    private synchronized void ensureConnected(String email, String userId) throws MqttException {
        // If the client is null, or if the user has changed, we need to create a new client and connect.
        if (mqttClient == null || !mqttClient.isConnected() || !userId.equals(currentUserId)) {
            
            // If there's an old client, disconnect it first.
            if (mqttClient != null && mqttClient.isConnected()) {
                try {
                    mqttClient.disconnect();
                } catch (MqttException e) {
                    log.error("Error disconnecting previous MQTT client.", e);
                }
            }

            this.currentUserEmail = email;
            this.currentUserId = userId;
            this.messageId = 0;

            String username = currentUserEmail.toLowerCase() + "__letpot_v3";
            String password = DigestUtils.sha256Hex(currentUserId + "|" + DigestUtils.md5Hex(username));
            String brokerUrl = "wss://" + BROKER_HOST + ":" + BROKER_PORT + WEBSOCKET_PATH;
            String clientId = "LetPot_Java_" + System.currentTimeMillis();

            mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            connOpts.setCleanSession(true);
            connOpts.setAutomaticReconnect(true); // Let Paho handle reconnects

            log.info("Connecting to LetPot MQTT broker for user {}", currentUserId);
            mqttClient.connect(connOpts);
            log.info("Connected to LetPot MQTT broker.");
        }
    }

    private List<String> generateMessagePackets(byte[] message) {
        int maintype = 1;
        int subtype = 19;
        int length = message.length;
        int maxPacketSize = MTU - 6;
        int numPackets = (length + maxPacketSize - 1) / maxPacketSize;

        List<String> packets = new ArrayList<>();
        for (int n = 0; n < numPackets; n++) {
            int start = n * maxPacketSize;
            int end = Math.min(start + maxPacketSize, length);
            byte[] payload = new byte[end - start];
            System.arraycopy(message, start, payload, 0, payload.length);

            List<Integer> packet = new ArrayList<>();
            if (n < numPackets - 1) {
                packet.add((subtype << 2) | maintype);
                packet.add(16);
                packet.add(messageId);
                packet.add(payload.length + 4);
                packet.add(length % 256);
                packet.add(length / 256);
            } else {
                packet.add((subtype << 2) | maintype);
                packet.add(0);
                packet.add(messageId);
                packet.add(payload.length);
            }
            for (byte b : payload) {
                packet.add(b & 0xFF);
            }

            StringBuilder hexString = new StringBuilder();
            for (Integer p : packet) {
                hexString.append(String.format("%02x", p));
            }
            packets.add(hexString.toString());
            messageId++;
        }
        return packets;
    }

    public synchronized void publishCommand(String deviceId, String email, String userId, byte[] command) throws MqttException {
        ensureConnected(email, userId);
        
        List<String> packets = generateMessagePackets(command);
        String topic = deviceId + "/cmd";
        
        for(String packet : packets) {
            MqttMessage message = new MqttMessage(packet.getBytes());
            message.setQos(1);
            mqttClient.publish(topic, message);
            log.info("Published message to topic {}: {}", topic, packet);
        }
    }
} 