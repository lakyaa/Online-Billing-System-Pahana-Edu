import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Pahana Edu Online Billing System (Console, Text-File Persistence)
 * ---------------------------------------------------------------
 * Features:
 * 1) User Authentication (username/password with SHA-256 hashing)
 * 2) Add / Edit / View Customer Accounts (accountNo, name, address, phone, unitsConsumed)
 * 3) Manage Items (Add / Update / Delete) — itemCode, name, unitPrice
 * 4) Display Account Details
 * 5) Calculate & Print Bill (tiered tariff on unitsConsumed, plus optional items)
 * 6) Help Section (usage guidelines)
 * 7) Exit System
 *
 * Storage: CSV-like text files under ./data directory
 *    users.csv       -> username, passwordHash
 *    customers.csv   -> accountNo, name, address, phone, unitsConsumed
 *    items.csv       -> itemCode, name, unitPrice
 *    bills.csv       -> billId, accountNo, dateTime, units, energyCharge, itemTotal, tax, grandTotal
 *
 * Compile:  javac PahanaEduBillingSystem.java
 * Run:      java PahanaEduBillingSystem
 */
public class PahanaEduBillingSystem {

    // ====== PATHS ======
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path USERS_FILE = DATA_DIR.resolve("users.csv");
    private static final Path CUSTOMERS_FILE = DATA_DIR.resolve("customers.csv");
    private static final Path ITEMS_FILE = DATA_DIR.resolve("items.csv");
    private static final Path BILLS_FILE = DATA_DIR.resolve("bills.csv");

    // ====== RUNTIME STATE ======
    private static final Scanner in = new Scanner(System.in);
    private static final Map<String, User> users = new LinkedHashMap<>();
    private static final Map<String, Customer> customers = new LinkedHashMap<>();
    private static final Map<String, Item> items = new LinkedHashMap<>();

    private static User currentUser = null;

    // ====== MAIN ======
    public static void main(String[] args) {
        safeInitStorage();
        loadAll();
        ensureDefaultAdmin();
        login();
        mainMenu();
        System.out.println("\nThank you for using Pahana Edu Billing System. Goodbye!\n");
    }

    // ====== MODELS ======
    static class User {
        String username;
        String passwordHash;
        User(String u, String p){ this.username=u; this.passwordHash=p; }
        String toCSV(){ return esc(username)+","+esc(passwordHash); }
        static User fromCSV(String line){
            String[] x = splitCSV(line,2);
            return new User(unesc(x[0]), unesc(x[1]));
        }
    }

    static class Customer {
        String accountNo;
        String name;
        String address;
        String phone;
        int unitsConsumed; // for billing (e.g., printing pages, energy units, etc.)
        Customer(String a,String n,String ad,String p,int u){
            this.accountNo=a; this.name=n; this.address=ad; this.phone=p; this.unitsConsumed=u;
        }
        String toCSV(){
            return String.join(",",
                esc(accountNo), esc(name), esc(address), esc(phone), String.valueOf(unitsConsumed)
            );
        }
        static Customer fromCSV(String line){
            String[] x = splitCSV(line,5);
            return new Customer(unesc(x[0]), unesc(x[1]), unesc(x[2]), unesc(x[3]), Integer.parseInt(x[4]));
        }
        @Override public String toString(){
            return String.format("Account: %s | Name: %s | Phone: %s | Units: %d\nAddress: %s",
                accountNo, name, phone, unitsConsumed, address);
        }
    }

    static class Item {
        String code;
        String name;
        double unitPrice;
        Item(String c,String n,double p){ this.code=c; this.name=n; this.unitPrice=p; }
        String toCSV(){ return String.join(",", esc(code), esc(name), String.valueOf(unitPrice)); }
        static Item fromCSV(String line){
            String[] x = splitCSV(line,3);
            return new Item(unesc(x[0]), unesc(x[1]), Double.parseDouble(x[2]));
        }
        @Override public String toString(){
            return String.format("%s | %s | %.2f", code, name, unitPrice);
        }
    }

    static class Bill {
        String billId;
        String accountNo;
        LocalDateTime dateTime;
        int units;
        double energyCharge; // based on tiered tariff
        double itemTotal;    // optional purchased items total
        double tax;          // VAT or similar
        double grandTotal;   // energyCharge + itemTotal + tax
        Bill(String id,String acc,LocalDateTime dt,int u,double e,double it,double tx,double gt){
            billId=id; accountNo=acc; dateTime=dt; units=u; energyCharge=e; itemTotal=it; tax=tx; grandTotal=gt;
        }
        String toCSV(){
            return String.join(",",
                esc(billId), esc(accountNo), esc(dateTime.toString()), String.valueOf(units),
                String.valueOf(energyCharge), String.valueOf(itemTotal), String.valueOf(tax), String.valueOf(grandTotal)
            );
        }
        static Bill fromCSV(String line){
            String[] x = splitCSV(line,8);
            return new Bill(unesc(x[0]), unesc(x[1]), LocalDateTime.parse(unesc(x[2])),
                    Integer.parseInt(x[3]), Double.parseDouble(x[4]), Double.parseDouble(x[5]),
                    Double.parseDouble(x[6]), Double.parseDouble(x[7]));
        }
    }

    // ====== AUTH ======
    private static void login(){
        System.out.println("\n=== Pahana Edu Online Billing System ===");
        System.out.println("(Type 'exit' at username to quit)\n");
        while(true){
            System.out.print("Username: ");
            String u = in.nextLine().trim();
            if (u.equalsIgnoreCase("exit")) System.exit(0);
            System.out.print("Password: ");
            String p = in.nextLine();
            User candidate = users.get(u.toLowerCase());
            if(candidate!=null){
                if(candidate.passwordHash.equals(hash(p))){
                    currentUser = candidate;
                    System.out.println("\nLogin successful. Welcome, " + currentUser.username + "!\n");
                    return;
                }
            }
            System.out.println("Invalid username or password. Please try again.\n");
        }
    }

    // ====== MAIN MENU ======
    private static void mainMenu(){
        while(true){
            System.out.println("================ MAIN MENU ================");
            System.out.println("1. Add New Customer Account");
            System.out.println("2. Edit Customer Information");
            System.out.println("3. Manage Item Information (Add/Update/Delete)");
            System.out.println("4. Display Account Details");
            System.out.println("5. Calculate & Print Bill");
            System.out.println("6. Help");
            System.out.println("7. Logout");
            System.out.println("8. Exit");
            System.out.print("Select option (1-8): ");
            String choice = in.nextLine().trim();
            switch (choice){
                case "1": addCustomer(); break;
                case "2": editCustomer(); break;
                case "3": manageItems(); break;
                case "4": displayAccountDetails(); break;
                case "5": calculateAndPrintBill(); break;
                case "6": showHelp(); break;
                case "7":
                    currentUser = null;
                    System.out.println("\nLogged out.\n");
                    login();
                    break;
                case "8": saveAll(); return;
                default: System.out.println("Invalid selection. Please choose 1-8.\n");
            }
        }
    }

    // ====== CUSTOMERS ======
    private static void addCustomer(){
        System.out.println("\n--- Add New Customer ---");
        System.out.print("Account Number (unique): ");
        String acc = readNonEmpty();
        if(customers.containsKey(acc)){
            System.out.println("Account already exists. Use Edit instead.\n");
            return;
        }
        System.out.print("Full Name: ");
        String name = readNonEmpty();
        System.out.print("Address: ");
        String addr = readNonEmpty();
        System.out.print("Telephone: ");
        String phone = readNonEmpty();
        System.out.print("Units Consumed (integer): ");
        int units = readIntNonNegative();
        Customer c = new Customer(acc,name,addr,phone,units);
        customers.put(acc, c);
        saveCustomers();
        System.out.println("Customer added successfully.\n");
    }

    private static void editCustomer(){
        System.out.println("\n--- Edit Customer Information ---");
        System.out.print("Enter Account Number: ");
        String acc = readNonEmpty();
        Customer c = customers.get(acc);
        if(c==null){
            System.out.println("No customer found with account number: " + acc + "\n");
            return;
        }
        System.out.println("Current: \n"+c+"\n");
        System.out.print("New Name (blank to keep): ");
        String name = in.nextLine().trim();
        if(!name.isEmpty()) c.name = name;
        System.out.print("New Address (blank to keep): ");
        String addr = in.nextLine().trim();
        if(!addr.isEmpty()) c.address = addr;
        System.out.print("New Telephone (blank to keep): ");
        String phone = in.nextLine().trim();
        if(!phone.isEmpty()) c.phone = phone;
        System.out.print("New Units Consumed (blank to keep): ");
        String unitsS = in.nextLine().trim();
        if(!unitsS.isEmpty()){
            try { c.unitsConsumed = Integer.parseInt(unitsS); }
            catch(Exception e){ System.out.println("Invalid units. Keeping previous value."); }
        }
        saveCustomers();
        System.out.println("Customer updated successfully.\n");
    }

    private static void displayAccountDetails(){
        System.out.println("\n--- Display Account Details ---");
        System.out.print("Enter Account Number (or * to list all): ");
        String acc = readNonEmpty();
        if(acc.equals("*")){
            if(customers.isEmpty()) { System.out.println("No customers found.\n"); return; }
            System.out.println("\nAll Customers:");
            for(Customer c: customers.values()){
                System.out.println("- "+c);
            }
            System.out.println();
        } else {
            Customer c = customers.get(acc);
            if(c==null){ System.out.println("No customer found.\n"); return; }
            System.out.println("\n"+c+"\n");
        }
    }

    // ====== ITEMS ======
    private static void manageItems(){
        while(true){
            System.out.println("\n--- Item Management ---");
            System.out.println("1) Add Item");
            System.out.println("2) Update Item");
            System.out.println("3) Delete Item");
            System.out.println("4) List Items");
            System.out.println("5) Back");
            System.out.print("Choose (1-5): ");
            String ch = in.nextLine().trim();
            switch(ch){
                case "1": addItem(); break;
                case "2": updateItem(); break;
                case "3": deleteItem(); break;
                case "4": listItems(); break;
                case "5": return;
                default: System.out.println("Invalid choice.\n");
            }
        }
    }

    private static void addItem(){
        System.out.print("Item Code (unique): ");
        String code = readNonEmpty();
        if(items.containsKey(code)) { System.out.println("Item code exists.\n"); return; }
        System.out.print("Item Name: ");
        String name = readNonEmpty();
        System.out.print("Unit Price: ");
        double price = readDoubleNonNegative();
        Item it = new Item(code,name,price);
        items.put(code,it);
        saveItems();
        System.out.println("Item added.\n");
    }

    private static void updateItem(){
        System.out.print("Enter Item Code to update: ");
        String code = readNonEmpty();
        Item it = items.get(code);
        if(it==null){ System.out.println("No such item.\n"); return; }
        System.out.println("Current: "+it);
        System.out.print("New Name (blank keep): ");
        String n = in.nextLine().trim();
        if(!n.isEmpty()) it.name = n;
        System.out.print("New Unit Price (blank keep): ");
        String p = in.nextLine().trim();
        if(!p.isEmpty()){
            try { it.unitPrice = Double.parseDouble(p); }
            catch(Exception e){ System.out.println("Invalid price. Keeping previous."); }
        }
        saveItems();
        System.out.println("Item updated.\n");
    }

    private static void deleteItem(){
        System.out.print("Enter Item Code to delete: ");
        String code = readNonEmpty();
        Item rem = items.remove(code);
        if(rem==null) System.out.println("No such item.\n");
        else { saveItems(); System.out.println("Item deleted.\n"); }
    }

    private static void listItems(){
        if(items.isEmpty()){ System.out.println("No items found.\n"); return; }
        System.out.println("\nItems:");
        System.out.println("Code | Name | UnitPrice");
        for(Item it: items.values()) System.out.println(it);
        System.out.println();
    }

    // ====== BILLING ======
    private static void calculateAndPrintBill(){
        System.out.println("\n--- Calculate & Print Bill ---");
        System.out.print("Enter Account Number: ");
        String acc = readNonEmpty();
        Customer c = customers.get(acc);
        if(c==null){ System.out.println("No such customer.\n"); return; }

        // Tariff: tiered example (editable)
        int units = c.unitsConsumed;
        double energyCharge = computeTieredCharge(units);

        // Optional: add purchased items to this bill
        double itemTotal = 0.0;
        if(!items.isEmpty()){
            System.out.print("Add items to bill? (y/n): ");
            String yn = in.nextLine().trim();
            while(yn.equalsIgnoreCase("y")){
                listItems();
                System.out.print("Enter Item Code: ");
                String code = readNonEmpty();
                Item it = items.get(code);
                if(it==null){ System.out.println("Invalid code."); }
                else {
                    System.out.print("Quantity: ");
                    int qty = readIntPositive();
                    itemTotal += it.unitPrice * qty;
                    System.out.printf("Added: %s x %d = %.2f\n", it.name, qty, it.unitPrice*qty);
                }
                System.out.print("Add another item? (y/n): ");
                yn = in.nextLine().trim();
            }
        }

        double subtotal = energyCharge + itemTotal;
        double taxRate = 0.15; // 15% VAT example — update to match policy
        double tax = round2(subtotal * taxRate);
        double grand = round2(subtotal + tax);

        String billId = generateBillId();
        Bill b = new Bill(billId, c.accountNo, LocalDateTime.now(), units,
                round2(energyCharge), round2(itemTotal), tax, grand);
        appendLine(BILLS_FILE, b.toCSV());

        // Print styled receipt
        printReceipt(c, b);
    }

    private static double computeTieredCharge(int units){
        // Example tiered slab: 0-50 @ 10.00, 51-100 @ 12.00, >100 @ 15.00
        int remaining = units;
        double total = 0.0;
        int tier1 = Math.min(remaining,50); total += tier1 * 10.0; remaining -= tier1;
        if(remaining>0){ int tier2 = Math.min(remaining,50); total += tier2 * 12.0; remaining -= tier2; }
        if(remaining>0){ total += remaining * 15.0; }
        return total;
    }

    private static void printReceipt(Customer c, Bill b){
        String line = "=".repeat(46);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        System.out.println("\n"+line);
        System.out.println(center("PAHANA EDU — BILL RECEIPT", 46));
        System.out.println(line);
        System.out.printf("Bill ID      : %s\n", b.billId);
        System.out.printf("Date/Time    : %s\n", b.dateTime.format(fmt));
        System.out.printf("Account No   : %s\n", c.accountNo);
        System.out.printf("Customer     : %s\n", c.name);
        System.out.printf("Telephone    : %s\n", c.phone);
        System.out.println("-".repeat(46));
        System.out.printf("Units Consumed: %d\n", b.units);
        System.out.printf("Energy Charge : %10.2f\n", b.energyCharge);
        System.out.printf("Items Total   : %10.2f\n", b.itemTotal);
        System.out.printf("Tax (15%%)     : %10.2f\n", b.tax);
        System.out.println("-".repeat(46));
        System.out.printf("GRAND TOTAL   : %10.2f\n", b.grandTotal);
        System.out.println(line+"\n");
    }

    private static String generateBillId(){
        return "B" + System.currentTimeMillis();
    }

    // ====== HELP ======
    private static void showHelp(){
        System.out.println("\n--- Help: System Usage Guidelines ---");
        System.out.println("1) Login with your username and password. Default admin is created on first run.");
        System.out.println("2) Add Customer: Enter unique account number and details.");
        System.out.println("3) Edit Customer: Update customer fields or units consumed.");
        System.out.println("4) Manage Items: Maintain item list with unit prices.");
        System.out.println("5) Calculate & Print Bill: Computes tiered energy charge + items + tax.");
        System.out.println("6) Use '*' when prompted for account number to list all customers.");
        System.out.println("7) Data is saved under the 'data' folder as CSV text files.\n");
    }

    // ====== STORAGE ======
    private static void safeInitStorage(){
        try { Files.createDirectories(DATA_DIR); } catch (IOException e){ throw new RuntimeException(e); }
        try { if(!Files.exists(USERS_FILE)) Files.createFile(USERS_FILE); } catch (IOException ignored){}
        try { if(!Files.exists(CUSTOMERS_FILE)) Files.createFile(CUSTOMERS_FILE); } catch (IOException ignored){}
        try { if(!Files.exists(ITEMS_FILE)) Files.createFile(ITEMS_FILE); } catch (IOException ignored){}
        try { if(!Files.exists(BILLS_FILE)) Files.createFile(BILLS_FILE); } catch (IOException ignored){}
    }

    private static void loadAll(){ loadUsers(); loadCustomers(); loadItems(); }
    private static void saveAll(){ saveUsers(); saveCustomers(); saveItems(); }

    private static void loadUsers(){
        users.clear();
        try(BufferedReader br = Files.newBufferedReader(USERS_FILE)){
            String line; while((line=br.readLine())!=null){
                line=line.trim(); if(line.isEmpty()) continue;
                User u = User.fromCSV(line);
                users.put(u.username.toLowerCase(), u);
            }
        } catch(IOException e){ System.out.println("[WARN] Could not read users file: "+e.getMessage()); }
    }

    private static void saveUsers(){
        try(BufferedWriter bw = Files.newBufferedWriter(USERS_FILE)){
            for(User u: users.values()){ bw.write(u.toCSV()); bw.newLine(); }
        } catch(IOException e){ System.out.println("[ERROR] Save users: "+e.getMessage()); }
    }

    private static void ensureDefaultAdmin(){
        if(users.isEmpty()){
            String username = "admin";
            String password = "admin123"; // change after first login
            users.put(username.toLowerCase(), new User(username, hash(password)));
            saveUsers();
            System.out.println("[INFO] Default admin created — username: admin, password: admin123\n");
        }
    }

    private static void loadCustomers(){
        customers.clear();
        try(BufferedReader br = Files.newBufferedReader(CUSTOMERS_FILE)){
            String line; while((line=br.readLine())!=null){
                line=line.trim(); if(line.isEmpty()) continue;
                Customer c = Customer.fromCSV(line);
                customers.put(c.accountNo, c);
            }
        } catch(IOException e){ System.out.println("[WARN] Could not read customers file: "+e.getMessage()); }
    }

    private static void saveCustomers(){
        try(BufferedWriter bw = Files.newBufferedWriter(CUSTOMERS_FILE)){
            for(Customer c: customers.values()){ bw.write(c.toCSV()); bw.newLine(); }
        } catch(IOException e){ System.out.println("[ERROR] Save customers: "+e.getMessage()); }
    }

    private static void loadItems(){
        items.clear();
        try(BufferedReader br = Files.newBufferedReader(ITEMS_FILE)){
            String line; while((line=br.readLine())!=null){
                line=line.trim(); if(line.isEmpty()) continue;
                Item it = Item.fromCSV(line);
                items.put(it.code, it);
            }
        } catch(IOException e){ System.out.println("[WARN] Could not read items file: "+e.getMessage()); }
    }

    private static void saveItems(){
        try(BufferedWriter bw = Files.newBufferedWriter(ITEMS_FILE)){
            for(Item it: items.values()){ bw.write(it.toCSV()); bw.newLine(); }
        } catch(IOException e){ System.out.println("[ERROR] Save items: "+e.getMessage()); }
    }

    private static void appendLine(Path file, String line){
        try(BufferedWriter bw = Files.newBufferedWriter(file, StandardOpenOption.APPEND)){
            bw.write(line); bw.newLine();
        } catch(IOException e){ System.out.println("[ERROR] Append: "+e.getMessage()); }
    }

    // ====== UTIL ======
    private static String readNonEmpty(){
        while(true){
            String s = in.nextLine().trim();
            if(!s.isEmpty()) return s;
            System.out.print("Value cannot be empty. Try again: ");
        }
    }

    private static int readIntNonNegative(){
        while(true){
            String s = in.nextLine().trim();
            try { int v = Integer.parseInt(s); if(v>=0) return v; } catch(Exception ignored){}
            System.out.print("Enter a non-negative integer: ");
        }
    }

    private static int readIntPositive(){
        while(true){
            String s = in.nextLine().trim();
            try { int v = Integer.parseInt(s); if(v>0) return v; } catch(Exception ignored){}
            System.out.print("Enter a positive integer: ");
        }
    }

    private static double readDoubleNonNegative(){
        while(true){
            String s = in.nextLine().trim();
            try { double v = Double.parseDouble(s); if(v>=0) return v; } catch(Exception ignored){}
            System.out.print("Enter a non-negative number: ");
        }
    }

    private static String hash(String plain){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(plain.getBytes());
            StringBuilder sb = new StringBuilder();
            for(byte x: b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); }
    }

    private static double round2(double v){ return Math.round(v*100.0)/100.0; }

    private static String esc(String s){ return s.replace("\\", "\\\\").replace(",", "\\,"); }
    private static String unesc(String s){
        StringBuilder out = new StringBuilder();
        boolean esc = false;
        for(char c: s.toCharArray()){
            if(esc){ out.append(c); esc=false; }
            else if(c=='\\') esc=true; else out.append(c);
        }
        return out.toString();
    }

    private static String[] splitCSV(String line, int expected){
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean esc = false;
        for(int i=0;i<line.length();i++){
            char c = line.charAt(i);
            if(esc){ cur.append(c); esc=false; }
            else if(c=='\\') { esc=true; }
            else if(c==',') { parts.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        parts.add(cur.toString());
        if(parts.size()!=expected){
            // Try to pad
            while(parts.size()<expected) parts.add("");
        }
        return parts.toArray(new String[0]);
    }

    private static String center(String s, int width){
        if(s.length()>=width) return s;
        int left = (width - s.length())/2;
        return " ".repeat(left) + s;
    }
}
