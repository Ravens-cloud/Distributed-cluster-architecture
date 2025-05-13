# Java Distributed MapReduce Cluster System

## 📖 项目简介

本项目实现了一个简化版的 Java 分布式 MapReduce 集群系统，采用 Master–Slave 架构，支持作业调度、任务拆分、节点通信、Map/Reduce 执行以及心跳检测等核心功能，旨在模拟 Hadoop MapReduce 的基本原理，适合作为分布式计算课程的实验或教学示例。

## 📐 系统架构图


- **Master 节点**：负责接收作业、拆分任务、调度执行、收集结果
- **Slave 节点**：执行 Map/Reduce 任务，定期向 Master 发送心跳
- **通信机制**：基于 Socket 或 RPC，任务调度采用简单轮询或空闲优先策略

## 🧩 项目结构
```bash
MapReduceCluster/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── core/
│   │   │   │   ├── KeyValue.java
│   │   │   │   └── NetworkUtils.java      # 新增网络工具类
│   │   │   ├── master/
│   │   │   │   ├── MasterNode.java
│   │   │   │   └── TaskScheduler.java     # 新增任务调度器
│   │   │   ├── slave/
│   │   │   │   ├── SlaveNode.java
│   │   │   │   └── TaskExecutor.java      # 新增任务执行器
│   │   │   ├── job/
│   │   │   │   ├── MapReduceJob.java
│   │   │   │   └── WordCountJob.java      # 新增示例任务
│   │   │   └── App.java
│   │   └── resources/
│   └── test/                             # 单元测试目录
├── pom.xml                              # Maven配置文件
├── config.properties # 配置文件
└── README.md
```

## ✅ 实现功能

- ✅ MapReduce 作业提交与调度
- ✅ Map 输入切分 & 中间结果收集
- ✅ Reduce 阶段归并 & 输出结果写入
- ✅ 基于心跳的节点健康检测
- ✅ 并行执行多个任务，提高系统吞吐量
- ✅ 简单轮询调度算法

## 🚀 快速开始

### 1. 构建项目

```bash
mvn clean package
```
### 2. 运行项目

```bash
java -jar target/mapreduce-cluster-1.0.0.jar
```
