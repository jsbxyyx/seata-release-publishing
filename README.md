# seata-release-publishing

seata release publishing

本文档需要结合 https://seata.apache.org/docs/developers/ppmc-guide/release-guide_dev 一起使用

> **注意**
> 
> **创建一个私有仓库把seata-install.yml, seata-release.yml放到.github/workflows/目录下，并在仓库 > settings > Actions secrets and variables > secrets > Repository secrets > 添加PASS, PRI, PUB, SETTINGS变量**
>
> **PASS: gpg密码**
> 
> **PRI: gpg私钥**
> 
> **PUB: gpg公钥**
> 
> **SETTINGS: maven settings.xml**

发布以2.4.0为例，具体版本请以当时情况为准

## 1. 创建发版分支

```
git checkout remotes/upstream/2.x -b 2.4.0
git push -u origin 2.4.0
```

> **note:**
> 
> 等待几个test action执行完成并通过，如果没有通过就先修复bug

## 2. 测试seata-server

先执行seata-install.yml 编译二进制包，下载编译后所有的包到本地，后续发布会用到。

- 解压src.tar.gz，先跨jdk版本编译源码，测试jdk版本jdk8, jdk17, jdk21

```
.\mvnw.cmd clean package -DskipTests=true
```

## 3. 执行seata-samples

下载seata-samples(代码中依赖了zookeeper server，需要自行下载并启动)

把所有demo跑一遍，确认没有问题

## 4. 检查license

创建一个新工程把CheckDepLicCli.java 复制到项目中，增加guava依赖

```
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.4.0-jre</version>
</dependency>
```

例如先检查namingserver的license和notice

把当前版本的LICENSE-namingserver复制到项目根目录，改名为LICENSE，修改java文件中的args路径改成自己的bin.tar.gz解压后的路径

执行完成后，控制台会输出哪些NOTICE和LICNESE没有，然后找到对应仓库的github，找到jar版本对应的分支，找到NOTICE和LICENSE。

复制LICENSE到本项目的distribution/licenses/目录，并在LICENSE-*里面对应的license下增加一条，具体参考LICENSE-*里面的写法

NOTICE增加在NOTICE-*中，具体参考NOTICE-*里面的写法 **没有NOTICE的就不用添加**

提交PR到2.4.0，重新回到第2步

检查没有问题，进行下一步

## 5. 整理release note

把changes/en/2.x.md 整理成对应的 2.4.0.md

github 增加draft release，并pre publish

## 6. 发布nexus

执行seata-release.yml，等待action完成，并在日志中搜索没有408，503等http错误码就表示OK

打开 https://repository.apache.org/#stagingRepositories 找到orgapacheseata-xxx选中并关闭

## 7. svn添加

第2步下载压缩包, asc, sha512文件

checkout svn

创建2.4.0-RCN 这里的N是第几次发版

把下载的文件都放进去，commit

提交信息写: submit 2.4.0-RC1 version

## 8. 发邮件






