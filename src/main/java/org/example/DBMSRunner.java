package org.example;

import model.Records;

import java.util.List;
import java.util.Scanner;

public class DBMSRunner {
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("1. Create File");
            System.out.println("2. Insert Record");
            System.out.println("3. Search Field");
            System.out.println("4. Search Record");
            System.out.println("5. Show Header");
            System.out.println("6. Exit");
            System.out.print("Select an option: ");

            int option = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (option) {
                case 1:
                    DBMS.createFileAndTable(scanner);
                    break;
                case 2:
                    DBMS.readDataFileAndBulkInsert();
                    break;
                case 3:
                    DBMS.searchField(scanner);
                    break;
                case 4:
                    DBMS.searchRecord(scanner);
                    break;
                case 5: // 디버깅용
                    DBMS.loadHeaderBlockFromFile(scanner);
                    break;
                case 6:
                    List<Records> records = DBMS.readRecordsFromBinaryFile("records.bin");
                    for (Records record : records) {
                        System.out.println(record);
                    }
                case 7:
                    System.out.println("Exiting program.");
                    return;
                default:
                    System.out.println("Please select a valid option.");
            }
        }
    }
}
