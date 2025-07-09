# LetPot Java Reference Client

This is a standalone Java reference implementation for integrating with the LetPot API and MQTT services. It provides a simple way to authenticate with LetPot, retrieve devices, and control them via MQTT commands.

It leverages learnings from @jpelgrom's [python-letpot](https://github.com/jpelgrom/python-letpot) project.

## Features

- Authentication with LetPot API
- Token refresh handling
- Device listing
- Device control via MQTT (turn on/off)
- Command packet generation for LetPot devices

## Requirements

- Java 17 or higher
- Maven

## Building the Project

```bash
mvn clean package
```

This will create a standalone JAR file with all dependencies included.

## Running the Demo

```bash
java -jar target/letpot-java-reference-1.0.0.jar
```

The demo application will:

1. Prompt for your LetPot email and password
2. Authenticate with the LetPot API
3. Retrieve your devices
4. Allow you to select a device and perform actions:
   - Turn on the device for 5 seconds
   - Turn on the device for a custom duration
   - Turn off the device
   - Run a test cycle (5 seconds on, then off)

## Using the Library in Your Project

### Authentication

```java
LetPotApiClient apiClient = ApiClientFactory.createLetPotApiClient();
LetPotService letPotService = new LetPotService(apiClient);

// Login and get credentials
LetPotCredentials credentials = letPotService.login("your-email@example.com", "your-password");
```

### Getting Devices

```java
List<LetPotDeviceDto> devices = letPotService.getDevices();
```

### Controlling Devices

```java
// Turn on a device for 10 seconds
letPotService.turnOnDevice("device-id", 10);

// Turn off a device
letPotService.turnOffDevice("device-id");

// Run a test cycle (5 seconds on, then off)
letPotService.testDevice("device-id");
```

## Architecture

The client consists of:

- **LetPotApiClient**: Interface for API calls using Retrofit
- **LetPotService**: Main service for authentication and device control
- **LetPotMqttService**: MQTT service for sending commands to devices
- **DTOs**: Data transfer objects for API responses
- **ApiClientFactory**: Utility for creating API clients

## License

This project is provided as a reference implementation and can be freely used in your own projects.
