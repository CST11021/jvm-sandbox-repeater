# 快速开始

```shell
sh ~/sandbox/bin/sandbox.sh -p 81125 -P 8820

ps -ef | grep sandbox | grep java | grep -v grep | awk '{print $2}'
ps -ef | grep gs-rest-service | grep java | grep -v grep | awk '{print $2}'
```



## jvm-sandbox-repeater

### 回放控制台的JVM参数配置

```shell
-Xdebug
-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000
-javaagent:${HOME}/sandbox/lib/sandbox-agent.jar=server.port=8820\;server.ip=0.0.0.0
-Dapp.name=repeater
-Dapp.env=daily
```

<img src="assets/image-20221201160431558.png" alt="image-20221201160431558" style="zoom:50%;" />

### 测试url

```shell
curl -s 'http://127.0.0.1:8001/regress/slogan'
curl -s 'http://127.0.0.1:8001/regress/slogan?Repeat-TraceId=127000000001156034386424510000ed'
curl -s 'http://127.0.0.1:8001/regress/slogan?Repeat-TraceId-X=127000000001156034386424510000ed'
```

## 录制自定义应用

### demo应用的JVM参数配置

```shell
-Xdebug
-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8100
-javaagent:${HOME}/sandbox/lib/sandbox-agent.jar=server.port=8820\;server.ip=0.0.0.0
-Dapp.name=gs-rest-service
-Dapp.env=daily
```

### 测试url

```shell
curl -s 'http://localhost:8080/greeting?name=User'
curl -s 'http://localhost:8080/greeting?name=User&Repeat-TraceId=127000000001166988138778310005ee'
curl -s 'http://localhost:8080/greeting?name=User&Repeat-TraceId-X=127000000001166988138778310005ee'
```

### 回放配置

注：这里一定要配置才能录制，对应源码的RepeaterConfig类，配置项说明可以参考这个类

```json
{
  "sampleRate": 10000,
  "useTtl": false,
  "degrade": false, 
  "exceptionThreshold": 1000,
  "httpEntrancePatterns": [
    "^/greeting.*$"
  ],
  "javaEntranceBehaviors": [
  ],
  "javaSubInvokeBehaviors": [
    {
      "classPattern": "hello.GreetingController",
      "includeSubClasses": false,
      "methodPatterns": [
        "greeting"
      ]
    }
  ],
  "pluginIdentities": [
    "http",
    "java-subInvoke"
  ],
  "repeatIdentities": [
    "java",
    "http"
  ]
}
```

说明：

```
useTtl：是否开启ttl线程上下文切换，开启之后，才能将并发线程中发生的子调用记录下来，否则无法录制到并发子线程的子调用信息，原理是将住线程的threadLocal拷贝到子线程，执行任务完成后恢复
degrade：是否执行录制降级策略，开启之后，不进行录制，只处理回放请求
exceptionThreshold：异常发生阈值；默认1000，当感知到异常次数超过阈值后，会降级模块
sampleRate：采样率；最小力度万分之一，10000 代表 100%
pluginsPath：插件地址
httpEntrancePatterns：由于HTTP接口的量太大（前后端未分离的情况可能还有静态资源）因此必须走白名单匹配模式才录制
javaEntranceBehaviors：java入口插件动态增强的行为
javaSubInvokeBehaviors：java子调用插件动态增强的行为
pluginIdentities：需要启动的插件
repeatIdentities：回放器插件
```

# jvm-sandbox-repeater脚本

### bootstrap.sh

说明：

1、kill `repeater-bootstrap.jar` 进程；

2、打包 `jvm-sandbox-repeater` 工程，输出jar包；

3、下载 [sandbox](https://github.com/alibaba/jvm-sandbox-repeater/releases/download/v1.0.0/sandbox-1.3.3-bin.tar) 部署包，并解压到home目录；

4、创建 `~/.sandbox-module` 目录，并复制 `jvm-sandbox-repeater` 工程jar包到该目录；

5、启动home目录下的 `.sandbox-module/repeater-bootstrap.jar` 应用；



### install-local.sh

1、打包 `jvm-sandbox-repeater` 工程，输出jar包；

2、下载 [sandbox](https://github.com/alibaba/jvm-sandbox-repeater/releases/download/v1.0.0/sandbox-1.3.3-bin.tar) 部署包，并解压到home目录；

3、创建 `~/.sandbox-module` 目录；

4、将 `package.sh` 脚本生成的jar包文件copy到 `~/.sandbox-module`



### package.sh

打包如下jar包

<img src="assets/image-20221021144015374.png" alt="image-20221021144015374" style="zoom:50%;" />



### install-repeater.sh

1、下载 [sandbox](https://github.com/alibaba/jvm-sandbox-repeater/releases/download/v1.0.0/sandbox-1.3.3-bin.tar) 部署包，并解压到home目录；

2、下载  [jvm-sandbox-repeater](https://github.com/alibaba/jvm-sandbox-repeater/releases/download/v1.0.0/repeater-stable-bin.tar) 工程部署包（repeater-stable-bin.tar压缩包内容如下），并解压到 `~/.sandbox-module`

 <img src="assets/image-20221024111858658.png" alt="image-20221024111858658" style="zoom:80%;" />





# 录制自定义应用

```
1.jvm-sandbox-repeater工程打包
	①下载工程：https://github.com/CST11021/jvm-sandbox-repeater.git 
	②切换分支：源码工程有bug，需要使用 whz-opt 分支进行打包
	③执行工程bin目录下的package.sh脚本，执行命令：sh package.sh
	④将打包生成的repeater-stable-bin.tar压缩包解压到 ~/.sandbox-module 目录（如果没有该目录需要创建该目录）

2.下载sandbox部署包，并解压到home目录，下载链接：https://github.com/alibaba/jvm-sandbox-repeater/releases/download/v1.0.0/sandbox-1.3.3-bin.tar

3.启动录制回放控制台：
  ①启动jvm-sandbox-repeater控制台：通过whz-opt分支下的com.alibaba.repeater.console.start.Application类启动
  也可以通过jar包启动：java -jar ${HOME}/.sandbox-module/repeater-bootstrap.jar
  ②访问页面: http://localhost:8001/config/list.htm

4.启动应用进程：
	①执行：java -jar gs-rest-service-0.1.0.jar
	${JAVA_HOME}/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 \
     -javaagent:${HOME}/sandbox/lib/sandbox-agent.jar=server.port=8820\;server.ip=0.0.0.0 \
     -Dapp.name=gs-rest-service \
     -Dapp.env=daily \
     -jar gs-rest-service-0.1.0.jar
     
	②查看应用程序PID：
  ps aux | grep gs-rest-service-0.1.0.jar
  ps -ef | grep gs-rest-service-0.1.0.jar

5.启动sandbox：
# -P 是设定 jvm-sandbox 的端口号，后面回放需要用到
sh ~/sandbox/bin/sandbox.sh -p `ps -ef | grep "gs-rest-service-0.1.0.jar" | grep -v grep | awk '{print $2}'` -P 12580

#停止sandbox: 
./sandbox.sh -p 2218 -S

(待确认)如果知道应用程序端口，也可以直接执行：sh ~/sandbox/bin/sandbox.sh -p 66104 -P 8006

6.查看日志：tail -f ~/logs/sandbox/repeater/repeater.log

7.配置文件：~/.sandbox-module/cfg/repeater-config.json
{
  "degrade": false, 
  "exceptionThreshold": 1000,
  "httpEntrancePatterns": [
    "^/greeting.*$"
  ],
  "javaEntranceBehaviors": [
  ],
  "javaSubInvokeBehaviors": [
    {
      "classPattern": "hello.GreetingController",
      "includeSubClasses": false,
      "methodPatterns": [
        "greeting"
      ]
    }
  ],
  "pluginIdentities": [
    "http",
    "java-subInvoke"
  ],
  "repeatIdentities": [
    "java",
    "http"
  ],
  "sampleRate": 10000,
  "useTtl": false
}

8.请求：curl -s 'http://localhost:8080/greeting?name=User'

```







```
工程应用：
https://github.com/alibaba/JVM-Sandbox
https://github.com/alibaba/jvm-sandbox-repeater
https://github.com/chenhengjie123/gs-rest-service.git 

参考文档：
https://www.bilibili.com/read/cv6168589
https://www.cnblogs.com/hong-fithing/p/16222644.html
https://segmentfault.com/a/1190000041686449?utm_source=sf-similar-article


配置文件: ~/.sandbox-module/cfg/repeater-config.json
{
  "degrade": false, 
  "exceptionThreshold": 1000,
  "httpEntrancePatterns": [
    "^/greeting.*$"
  ],
  "javaEntranceBehaviors": [
  ],
  "javaSubInvokeBehaviors": [
    {
      "classPattern": "hello.GreetingController",
      "includeSubClasses": false,
      "methodPatterns": [
        "greeting"
      ]
    }
  ],
  "pluginIdentities": [
    "http",
    "java-subInvoke"
  ],
  "repeatIdentities": [
    "java",
    "http"
  ],
  "sampleRate": 10000,
  "useTtl": false
}

启动console: java -jar ${HOME}/.sandbox-module/repeater-bootstrap.jar

${JAVA_HOME}/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 \
     -javaagent:${HOME}/sandbox/lib/sandbox-agent.jar=server.port=8820\;server.ip=0.0.0.0 \
     -Dapp.name=repeater \
     -Dapp.env=daily \
     -jar ${HOME}/.sandbox-module/repeater-bootstrap.jar


启动应用: java -jar gs-rest-service-0.1.0.jar
${JAVA_HOME}/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 \
     -javaagent:${HOME}/sandbox/lib/sandbox-agent.jar=server.port=8820\;server.ip=0.0.0.0 \
     -Dapp.name=gs-rest-service \
     -Dapp.env=daily \
     -jar gs-rest-service-0.1.0.jar

查看应用程序PID: 
ps aux | grep gs-rest-service-0.1.0.jar
ps -ef | grep gs-rest-service-0.1.0.jar

启动sandbox: 
./sandbox.sh -p 2218 -P 8005

# -P 是设定 jvm-sandbox 的端口号，后面回放需要用到
sh ~/sandbox/bin/sandbox.sh -p `ps -ef | grep "gs-rest-service-0.1.0.jar" | grep -v grep | awk '{print $2}'` -P 12580

停止sandbox: 
./sandbox.sh -p 2218 -S

查看日志: 
tail -f ~/logs/sandbox/repeater/repeater.log

$ tail -200f ~/logs/sandbox/repeater/repeater.log
...
2019-07-07 10:24:14 INFO  initializing logback success. file=/Users/hengjiechen/.sandbox-module/cfg/repeater-logback.xml;
2019-07-07 10:24:14 INFO  module on loaded,id=repeater,version=1.0.0,mode=ATTACH
2019-07-07 10:24:14 INFO  onActive
2019-07-07 10:24:14 INFO  pull repeater config success,config=com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterConfig@4dddeb36
2019-07-07 10:24:15 INFO  enable plugin http success
2019-07-07 10:24:15 INFO  add watcher success,type=http,watcherId=1000
2019-07-07 10:24:16 INFO  register event bus success in repeat-register


请求: 
curl -s 'http://localhost:8080/greeting?name=User'
```













