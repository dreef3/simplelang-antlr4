package com.meteleva;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;

public class StackMachine {

    public static void main(String[] args) throws FileNotFoundException {
    	String filename;
    	if (args.length != 1) {
    		System.out.println("Input file name:");
    		Scanner consoleScanner = new Scanner(System.in);
    		filename = consoleScanner.next();
    		consoleScanner.close();
    	} else {
    		filename = args[0];
    	}
    	
        Scanner inputScanner = new Scanner(new FileInputStream(filename));
        int cn = inputScanner.nextInt();
        int dn = inputScanner.nextInt();
        int commands[] = new int[cn];
        int data[] = new int[dn];
        for (int i = 0; i < cn; i++) {
            commands[i] = inputScanner.nextInt();
        }
        for (int i = 0; i < dn; i++) {
            data[i] = inputScanner.nextInt();
        }
        inputScanner.close();

        new StackMachine(data, commands).execute();
    }
	
    public static final int DATA_MIN = -9999;
    public static final int DATA_MAX = 9999;

    public enum Command {
        ADD(-10000), MULT(-10001), MINUS(-10002), DIV(-10003), IFEQ(-10004),
        IFLT(-10005), GOTO(-10006), LOAD(-10007), FREE(-10008), STORE(-10009),
        COUNT(-10010), PRINT(-10011), READ(-10012), STOP(-10013), STOREC(-10014);

        public final int code;

        Command(final int code) {
            this.code = code;
        }

        public static Command valueOf(final int code) {
            for (Command cmd : Command.values()) {
                if (cmd.code == code) {
                    return cmd;
                }
            }
            return null;
        }
    }

    private int data[];
    private int commands[];
    private int dc;
    private int cc;
    
    Scanner scanner = new Scanner(System.in);

    public StackMachine(int D[], int C[]) {
        this.data = D;
        this.commands = C;
        this.dc = data.length - 1;
        this.cc = 0;
    }

    private int getData(int index) {
        if (index < 0 || index > data.length - 1) {
            throw new IllegalArgumentException(String.format("Data index is out of bounds: %d", index));
        }
        return data[index];
    }

    private void setData(int index, int value) {
        if (index < 0 || index > data.length) {
            throw new IllegalArgumentException(String.format("Data index is out of bounds: %d", index));
        }
        if (value < DATA_MIN || value > DATA_MAX) {
            throw new IllegalArgumentException(String.format("Data value is out of bounds: %d", index));
        }
        if (index == data.length) {
            data = Arrays.copyOf(data, data.length + 1);
        }
        data[index] = value;
    }

    private int getCommand(int index) {
        if (index < 0 || index > commands.length - 1) {
            throw new IllegalArgumentException(String.format("Command index is out of bounds: %d", index));
        }
        return commands[index];
    }

    private void printDebug(int currentCmd) {
    	System.out.printf("cc=%d dc=%d cmd=%d (%s)\n", cc, dc, currentCmd, Command.valueOf(currentCmd));
    	System.out.print("Data: [");
    	for (int i = 0; i < data.length; i++) {
			System.out.printf("%d ", data[i]);
		}
    	System.out.println("]");
    }
    
    public void execute() {
        while (cc < commands.length) {
            int currentCmd = getCommand(cc);
            
            
            printDebug(currentCmd);
            
            
            if (currentCmd >= DATA_MIN && currentCmd <= DATA_MAX) {
                dc++; setData(dc, currentCmd); cc++;
                continue;
            }
            Command cmd = Command.valueOf(currentCmd);
            switch (cmd) {
                case ADD:
                    dc--;
                    setData(dc, getData(dc) + getData(dc + 1));
                    break;
                case MULT:
                    dc--;
                    setData(dc, getData(dc) * getData(dc + 1));
                    break;
                case MINUS:
                    setData(dc, -getData(dc));
                    break;
                case DIV:
                    dc--;
                    setData(dc, getData(dc) / getData(dc + 1));
                    break;
                case IFEQ:
                    if (getData(dc) == getData(dc - 1)) {
                        cc = getData(dc - 2) - 1;
                    }
                    dc -= 3;
                    break;
                case IFLT:
                    if (getData(dc) < getData(dc - 1)) {
                        cc = getData(dc - 2) - 1;
                    }
                    dc -= 3;
                    break;
                case GOTO:
                    cc = getData(dc) - 1; // To compensate cc++ on each cycle
                    dc--;
                    break;
                case LOAD:
                    setData(dc, getData(getData(dc)));
                    break;
                case FREE:
                	data[dc] = 0;
                    dc--;                    
                    break;
                case STORE:
                    setData(getData(dc), getData(dc - 1));
                    dc--;
                    break;
                case STOREC:
                	dc++;
                	setData(dc, cc);
                case COUNT:
                    dc++;
                    setData(dc, dc);
                    break;
                case PRINT:
                    System.out.println(getData(dc));
                    break;
                case READ:
                    dc++;
                    setData(dc, read());
                    break;
                case STOP:
                	scanner.close();
                    return;
                default:
                    throw new IllegalArgumentException(String.format("Invalid command: %d", commands[cc]));
            }
            cc++;

        }
    }

    private int read() {
    	System.out.println("Value input:");
        int value = scanner.nextInt();
        return value;
    }
}
