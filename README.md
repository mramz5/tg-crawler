# 🧭 **Telegram TDLib Searcher**  
_**For Windows x64 Users Only**_

---

## 📘 **Overview**

This is a **TDLib-based Telegram client** that allows you to search for **specific keywords** across selected Telegram channels.

---

## 🚀 **How to Run**

### 1. **Install Java 21**
To run this application, you need **Java 21 (JDK)** installed.

- Download and install **Java 21** from Oracle:  
  👉 [Download JDK 21 for Windows x64](https://download.oracle.com/java/21/archive/jdk-21.0.8_windows-x64_bin.exe)

---

### 2. **Download the Application**

- Obtain the compiled **JAR file** for your desired version:  
  - For **console app**: `tele-search-console-1.0-SNAPSHOT.jar`  
  - For **GUI app**: `tele-search-gui-1.0-SNAPSHOT.jar`
  
- Extract the JAR file to a folder of your choice (e.g., `C:\tele-search`).

---

### 3. **Run the App**

#### **For GUI Version:**

- Simply double-click on the `tele-search-gui-1.0-SNAPSHOT.jar` file to run the application.

#### **For Console Version:**

- Download the **`run.bat`** file and double-click it to start the console app.

---

### ⚠️ **Troubleshooting for GUI Version**

If the **GUI version** doesn’t work by double-clicking, follow these steps:

1. **Run Command Prompt as Administrator**.
2. Execute the following commands to associate `.jar` files with `javaw.exe`:

```bash
assoc .jar=jarfile
ftype jarfile="C:\path\to\jdk-21(or higher)\bin\javaw.exe" -jar "%1" %*
