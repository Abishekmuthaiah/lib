# Digital Library Management System (DLMS)

**Industrial Internship Capstone Project**  
*Evoriea Infotech Pvt. Ltd., Bangalore* (11 May 2026 – 11 June 2026)  
**Candidate**: Abishek Muthaiah S K (Register Number: 95192301003)  
**Department**: Computer Science and Engineering, P.S.R. Engineering College (Autonomous), Sivakasi  
**Domain**: Enterprise Java (JDK 17 LTS) & Relational SQL Architecture  

---

## 📖 Executive Summary
The **Digital Library Management System (DLMS)** is an enterprise relational library management solution designed to eliminate administrative overhead, human errors, and inventory-control risks associated with manual tracking registers. The system automates:
- Book catalogue inventory and availability tracking
- Multi-tier membership registry (Student, Faculty, General)
- Loan periods and borrowing quota enforcement
- Atomic issue and return transactions with ACID guarantees
- Automated overdue fine calculation engine (Rs. 5.00 / day)
- Transactional audit trails and formatted fine receipts
- Category-wise SQL analytics and circulation reporting

---

## 🏛 Multi-Tier Layered DAO Architecture (Figure 3.1)

The system is engineered as a cohesive 5-tier architecture:

```
┌─────────────────────────────────────────────────────────────┐
│ Layer 1: Presentation Layer                                 │
│ - CLI Console (MainApplication.java)                        │
│ - React Light-Theme SaaS Dashboard (Vite + React 19)        │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│ Layer 2: Service / Business Logic Layer                     │
│ - LibraryService.java (Loan periods, quotas, fine formula)  │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│ Layer 3: Data Access Object (DAO) Layer                     │
│ - LibraryDAO.java (PreparedStatements, ACID commit/rollback)│
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│ Layer 4: Database Connectivity Layer                        │
│ - DBConnection.java (Singleton connection manager)          │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│ Layer 5: Relational Database Storage Layer                  │
│ - library_management.db (3NF SQLite / MySQL)                │
└─────────────────────────────────────────────────────────────┘
```

---

## 🗄 3NF Relational Database Schema (Figure 3.2)

Normalized to **Third Normal Form (3NF)** with primary keys, foreign key constraints, check constraints, and B-Tree indexes:

1. **`books`**:
   - `book_id` (PK, INTEGER AUTOINCREMENT)
   - `isbn` (VARCHAR, UNIQUE)
   - `title`, `author`, `publisher`, `category`
   - `total_copies` (INTEGER CHECK >= 0)
   - `available_copies` (INTEGER CHECK >= 0)
2. **`members`**:
   - `member_id` (PK, INTEGER AUTOINCREMENT)
   - `full_name`, `email` (UNIQUE), `phone`
   - `membership_type` (CHECK IN: `STUDENT`, `FACULTY`, `GENERAL`)
   - `join_date`, `status` (`ACTIVE`, `SUSPENDED`, `EXPIRED`)
3. **`book_issues`**:
   - `issue_id` (PK, INTEGER AUTOINCREMENT)
   - `book_id` (FK -> `books.book_id`)
   - `member_id` (FK -> `members.member_id`)
   - `issue_date`, `due_date`, `return_date`, `status` (`ISSUED`, `RETURNED`)
4. **`fines`**:
   - `fine_id` (PK, INTEGER AUTOINCREMENT)
   - `issue_id` (FK -> `book_issues.issue_id`)
   - `member_id` (FK -> `members.member_id`)
   - `amount` (DECIMAL CHECK >= 0), `reason`, `paid_status` (`UNPAID`, `PAID`)
   - `created_at` (TIMESTAMP)

---

## ⚙️ Borrowing Rules & Fine Calculation Engine (Figure 3.3)

| Membership Tier | Loan Period | Concurrent Borrowing Limit |
|---|:---:|:---:|
| **STUDENT** | **14 days** | **3 books** |
| **FACULTY** | **30 days** | **5 books** |
| **GENERAL** | **7 days**  | **2 books** |

### Fine Calculation Formula:
$$\text{Overdue Days} = \max(0, \text{Return Date} - \text{Due Date})$$
$$\text{Fine Amount} = \text{Overdue Days} \times \text{Rs. } 5.00\text{ per day}$$

---

## 📊 Category-Wise SQL Analytics & Management Insights (Slide 11)

Reference circulation distribution across the 58 logged issues:

| Book Category | Books Issued | Fine Collected (Rs.) | Share of Issues (%) |
|---|:---:|:---:|:---:|
| **Computer Science** | 18 | Rs. 180.00 | 31.0% |
| **Engineering Mathematics** | 14 | Rs. 110.00 | 24.1% |
| **Fiction** | 11 | Rs. 45.00 | 19.0% |
| **General Reference** | 9 | Rs. 35.00 | 15.5% |
| **Biography** | 6 | Rs. 20.00 | 10.3% |
| **TOTAL** | **58** | **Rs. 390.00** | **100.0%** |

### Key Management Insights:
1. **Computer Science** is the most borrowed category (**18 of 58 issues, 31.0%**) and yields the highest fine collection (**Rs. 180.00 of Rs. 390.00**).
2. **Engineering Mathematics** ranks second (**14 issues, Rs. 110.00**).
3. **Biography** records the lowest circulation (**6 issues, Rs. 20.00**).

---

## 🧾 Sample Fine Receipt (Slide 10 Trace)

```
=================================================================
         SAMPLE FINE RECEIPT (ISSUE ID 7)
=================================================================
 * Issue ID / Member : 7 | Priya R (STUDENT)
 * Book Title / ISBN : Effective Java / 9780134685991
 * Issue Date        : 2026-05-27
 * Due Date          : 2026-06-10
 * Return Date       : 2026-06-14
 * Overdue Days      : 4
 * Fine Rate         : Rs. 5.00 / day
 * FINE PAYABLE      : Rs. 20.00 (4 x Rs. 5.00)
 * Payment Status    : UNPAID
=================================================================
```

---

## 🚀 How to Run the Project

### 1. Run the Interactive React Dashboard (Light Theme SaaS Dashboard)
```bash
run-react.bat
```
Then open your browser at **http://localhost:5173/**.

### 2. Run the Java Backend / Embedded Web Server
```bash
run-web.bat
```
Serves the REST API and the production web app on **http://localhost:8080/**.

### 3. Run the Interactive Command-Line Interface (CLI)
```bash
run-cli.bat
```
Or directly from PowerShell:
```powershell
java -cp "bin;lib/*" com.evoriea.dlms.presentation.MainApplication
```

To run the automated Slide 10 demo trace:
```powershell
java -cp "bin;lib/*" com.evoriea.dlms.presentation.MainApplication --demo
```

### 4. Run the Automated Test Suite (10 Test Cases)
```bash
test.bat
```
Or directly from PowerShell:
```powershell
java -cp "bin;lib/*" com.evoriea.dlms.test.DLMSTestSuite
```
Verifies Singleton DB connection, schema creation, borrowing quotas, atomic rollback, overdue fine math, and Slide 11 analytics integrity.
#   l i b  
 #   l i b  
 #   l i b  
 #   l i b  
 #   p s r  
 #   p s r  
 #   p s r  
 