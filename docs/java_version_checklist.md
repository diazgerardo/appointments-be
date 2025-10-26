# 🧩 IntelliJ + Maven Java Version Checklist

**Purpose:**  
Keep *Maven*, *IntelliJ IDEA*, and your *runtime environment* using the same JDK version to avoid mysterious errors.

---

## 🧠 Concept Diagram (HTML table)

<!-- Using an HTML table so it renders cleanly without monospaced fonts or code fences -->
<table style="border-collapse:collapse; width:100%; text-align:left;">
  <tr>
    <td style="border:1px solid #ccc; padding:12px; background:#f8fff8;">
      <strong>🧩 IntelliJ IDEA</strong>
      <table style="border-collapse:collapse; width:100%; margin-top:8px;">
        <tr>
          <td style="border:1px dashed #7fbf7f; padding:8px;">
            1️⃣ <strong>Project SDK &amp; Language Level</strong><br>
            <small>(controls syntax &amp; build)</small>
          </td>
        </tr>
        <tr>
          <td style="border:1px dashed #7fbf7f; padding:8px;">
            2️⃣ <strong>Run Configuration JRE</strong><br>
            <small>(used when pressing ▶️)</small>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr><td style="text-align:center; padding:6px;">⬇️</td></tr>
  <tr>
    <td style="border:1px solid #ccc; padding:12px;">
      <strong>🧱 Maven Build</strong><br>
      <small>Uses <code>pom.xml</code> settings (<code>maven.compiler.*</code>)</small>
    </td>
  </tr>
  <tr><td style="text-align:center; padding:6px;">⬇️</td></tr>
  <tr>
    <td style="border:1px solid #ccc; padding:12px;">
      <strong>● Actual JDK install</strong><br>
      <small>Referenced via <code>JAVA_HOME</code> and Maven toolchain</small>
    </td>
  </tr>
</table>

**Summary:**  
All three layers must point to the *same major JDK version* (e.g. 17 or 21).  
If they don’t — expect weird compile/run mismatches and “UnsupportedClassVersionError”.

---

## ✅ 1. Configure in `pom.xml`

```xml
<properties>
  <maven.compiler.source>17</maven.compiler.source>
  <maven.compiler.target>17</maven.compiler.target>
</properties>
```

> 💡 Replace `17` with the version your project requires.

---

## ✅ 2. Set IntelliJ Project SDK and Language Level

**Path:**  
`File → Project Structure → Project`

| Setting | Example Value |
|----------|----------------|
| Project SDK | `corretto-17` |
| Project language level | `17 – Sealed types, pattern matching, …` |

> 💡 Controls syntax, completion, and internal builds.

---

## ✅ 3. Check IntelliJ Run/Debug Configuration

**Path:**  
`Run → Edit Configurations → &lt;your run config&gt;`

| Setting | Example Value |
|----------|----------------|
| Build and run with | `corretto-17` |

> ⚠️ Avoid “JetBrains Runtime” unless you *really* know what you’re doing.

---

## ✅ 4. Verify Maven’s JDK

In IntelliJ Terminal or your system shell:

```bash
mvn -v
```

Ensure the output shows the same JDK version (e.g. `Java version: 17.x`).

---

## ✅ 5. Verify System Defaults (Optional)

```bash
java -version
echo $JAVA_HOME   # or echo %JAVA_HOME%
```

They should match your chosen JDK.

---

## 🧭 Pro Tip

If IntelliJ’s behavior diverges from Maven’s:
```bash
mvn clean compile -o
```
If it builds fine here but fails in IntelliJ — your IDE config is wrong.

---

**Last reviewed:** 2025-10-26  
**Maintainer:** your friendly Java team 🧑‍💻
