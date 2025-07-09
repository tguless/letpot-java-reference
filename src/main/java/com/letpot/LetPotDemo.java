package com.letpot;

import com.letpot.client.LetPotApiClient;
import com.letpot.dto.LetPotDeviceDto;
import com.letpot.model.LetPotCredentials;
import com.letpot.service.LetPotService;
import com.letpot.util.ApiClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Scanner;

public class LetPotDemo {
    private static final Logger log = LoggerFactory.getLogger(LetPotDemo.class);

    public static void main(String[] args) {
        System.out.println("LetPot Java Reference Client");
        System.out.println("===========================");
        
        // Create the API client
        LetPotApiClient apiClient = ApiClientFactory.createLetPotApiClient();
        LetPotService letPotService = new LetPotService(apiClient);
        
        Scanner scanner = new Scanner(System.in);
        
        try {
            // Login
            System.out.print("Enter email: ");
            String email = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();
            
            System.out.println("Logging in...");
            LetPotCredentials credentials = letPotService.login(email, password);
            System.out.println("Login successful! User ID: " + credentials.getLetpotUserId());
            
            // Get devices
            System.out.println("\nFetching devices...");
            List<LetPotDeviceDto> devices = letPotService.getDevices();
            
            if (devices.isEmpty()) {
                System.out.println("No devices found.");
                return;
            }
            
            System.out.println("Found " + devices.size() + " devices:");
            for (int i = 0; i < devices.size(); i++) {
                LetPotDeviceDto device = devices.get(i);
                System.out.println((i + 1) + ". " + device.getDeviceName() + " (" + device.getDeviceId() + ")");
            }
            
            // Select a device
            System.out.print("\nSelect a device (1-" + devices.size() + "): ");
            int deviceIndex = Integer.parseInt(scanner.nextLine()) - 1;
            
            if (deviceIndex < 0 || deviceIndex >= devices.size()) {
                System.out.println("Invalid device selection.");
                return;
            }
            
            LetPotDeviceDto selectedDevice = devices.get(deviceIndex);
            System.out.println("Selected device: " + selectedDevice.getDeviceName());
            
            // Menu
            boolean exit = false;
            while (!exit) {
                System.out.println("\nOptions:");
                System.out.println("1. Turn on device (5 seconds)");
                System.out.println("2. Turn on device (custom duration)");
                System.out.println("3. Turn off device");
                System.out.println("4. Run test cycle (5 seconds on, then off)");
                System.out.println("5. Exit");
                System.out.print("Select an option: ");
                
                int option = Integer.parseInt(scanner.nextLine());
                
                switch (option) {
                    case 1:
                        System.out.println("Turning on device for 5 seconds...");
                        letPotService.turnOnDevice(selectedDevice.getDeviceId(), 5);
                        System.out.println("Command sent successfully.");
                        break;
                    case 2:
                        System.out.print("Enter duration in seconds: ");
                        int duration = Integer.parseInt(scanner.nextLine());
                        System.out.println("Turning on device for " + duration + " seconds...");
                        letPotService.turnOnDevice(selectedDevice.getDeviceId(), duration);
                        System.out.println("Command sent successfully.");
                        break;
                    case 3:
                        System.out.println("Turning off device...");
                        letPotService.turnOffDevice(selectedDevice.getDeviceId());
                        System.out.println("Command sent successfully.");
                        break;
                    case 4:
                        System.out.println("Running test cycle...");
                        letPotService.testDevice(selectedDevice.getDeviceId());
                        System.out.println("Test completed.");
                        break;
                    case 5:
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            }
            
        } catch (Exception e) {
            log.error("Error: " + e.getMessage(), e);
            System.out.println("Error: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
} 