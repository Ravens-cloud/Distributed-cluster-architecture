@echo off
rem 创建目录结构
mkdir src\main\java\core
mkdir src\main\java\master
mkdir src\main\java\slave
mkdir src\main\java\job
mkdir src\main\resources


rem 创建 Java 源文件
type nul > src\main\java\core\KeyValue.java
type nul > src\main\java\core\NetworkUtils.java

type nul > src\main\java\master\MasterNode.java
type nul > src\main\java\master\TaskScheduler.java

type nul > src\main\java\slave\SlaveNode.java
type nul > src\main\java\slave\TaskExecutor.java

type nul > src\main\java\job\MapReduceJob.java
type nul > src\main\java\job\WordCountJob.java

type nul > src\main\java\App.java

rem 创建其他文件
type nul > pom.xml
type nul > README.md

echo 目录和文件已创建完毕。
