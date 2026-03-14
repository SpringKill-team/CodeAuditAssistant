# CodeAuditAssistant

Language: **English** | [中文](README.zh-CN.md)

## Overview
CodeAuditAssistant is an IntelliJ IDEA plugin for JVM code auditing. It provides sink discovery, call-graph analysis, and JAR decompilation to help reviewers locate risky code paths faster.

## Requirements
- IntelliJ IDEA `>= 2022.3`
- JDK `17+`

## Core Features
### 1) SinkFinder
Built-in sink rules for common Java Web vulnerabilities and risky component calls. Results are shown in IDEA Problem View and support navigation to source on double-click.

### 2) Code Analysis (Call Graph)
Generate call graphs for `Entire` project or `Selected Module`. Search supports:
- `ROOT -> SINK` path search
- `SINK`-only reverse path lookup
- `Search as sink` from a right-clicked method

Method filter examples:
- `ParamType`: `java.lang.String,*`
- `Annotations`: `@Override,@xxx`

### 3) Decompiler (Experimental)
Supports JAR decompilation from the plugin UI. The current implementation is still under optimization.

## Workflow Demo (Screenshots)
### 1) SinkFinder Flow
Step 1: Collect sink findings in IDEA Problem View.  
![Sink collection](./img/image-20250331010159751.png)

Step 2: Double-click an item to jump to source code.  
![Sink jump to source](./img/image-20250331010358550.png)

### 2) Decompiler Flow
Step 1: Select a target JAR and click `Run` in the decompiler panel.  
![Decompiler panel](./img/image-20250331010458737.png)

### 3) Call-Graph Flow
Step 1: Open the Code Analysis panel and click `Generate CallGraph`.  
![Call graph main panel](./img/image-20250331010441318.png)

Step 2: Choose the build scope (`Entire` or `Selected Module`).  
![Scope selection](./img/image-20250331010933308.png)

Step 3: You can also build from the editor context menu on a method.  
![Context menu build](./img/image-20250331123256112.png)

Step 4: Enable `Info` and `Path` for richer metadata and path results.  
![Info and Path options](./img/image-20250331123349918.png)

Step 5: Open the method finder panel and filter by signature/annotations.  
![Method finder panel](./img/image-20250331124009118.png)

Step 6: Example filtered method list.  
![Method finder example](./img/image-20250331124429606.png)

Step 7: Search a `ROOT -> SINK` path.  
![Root to sink search](./img/image-20250331124823202.png)

Step 8: Search by `SINK` only when the entry point is unknown.  
![Sink-only search](./img/image-20250331124952438.png)

Step 9: Use `Search as sink` from right-click to auto-fill and search.  
![Search as sink](./img/image-20250331125202792.png)

Step 10: Read runtime status (`CallGraph`, node count, memory, messages).  
![Status panel](./img/image-20250331125415889.png)

### 4) Search Result Icon Legend
Path node icon:  
![Path icon](./img/image-20250331125928270.png)

Method declaration icon:  
![Declaration icon](./img/image-20250331130037060.png)

Method invocation icon:  
![Call icon](./img/image-20250331130128343.png)

Object creation / method-search result icon:  
![New object icon](./img/image-20250331130217321.png)

## Build & Run
- Build plugin artifact: `./gradlew buildPlugin`
- Run sandbox IDE for local debugging: `./gradlew runIde`
- Full build: `./gradlew build`

## Known Limitations
- Current path search is DFS-based and may not show all parallel paths in dense graphs.
- Call graph is not yet persisted across sessions.
- Duplicate root/source nodes may create repeated paths in some results.

## Roadmap
- Improve path search completeness and graph model.
- Add call graph persistence and change monitoring.
- Deduplicate root/source nodes and repeated paths.
- Improve search-result highlighting and library JAR analysis workflow.
