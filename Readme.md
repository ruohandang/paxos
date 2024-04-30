## How to run RMI program locally

### Step 1: in `src` folder, compile the code:
```
javac server/*.java client/*.java
```
### Step 2: run servers (this will start 5 servers - replicas of key value store), to use default host (localhost) and port (1099):
```
./server.sh
```
To specify a custom host but use the default port (1099):
```
./server.sh custom_host
```

To specify both a custom host and a custom port:
```
./server.sh custom_host custom_port
```


### Step 3: Open a new terminal 
If you run clients without any arguments to connect to the default settings (localhost on port 1099):
```
./client.sh
```
If your RMI server running on a custom host but use the default port (1099), provide the hostname as the first argument:
```
./client.sh custom_host
```

To connect to an RMI server on a specific host and port, provide both as arguments:
```
./client.sh custom_host custom_port
```

NOTE: You can run multiple clients concurrently.

### Step 4: Quit app

- For RMI servers, just press ```enter``` in server terminal

- For Client, input ```exit``` or ```quit``` in the client ternimal

### If you have no permission to run shell file, please run:
```
chmod +x server.sh
chmod +x client.sh
```