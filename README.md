# Linux Distributed File System Integrated with Apache Zookeeper
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
## Introduction
This is a java maven-based distributed file system integrated with Apache Zookeeper API.
### How Distributed File System Can Help the File Sharing
The goal for the distributed file system is mainly for internal network to share file and edit together. Cooperating can be much easier because all the updates would be synchronized once the write operation is finished.

The file editing follows eventual consistency to ensure fast writing performance. This behavior trades off the consistency of files, which can leads to different versions for one file in a very short amount of time.

-------------------------------------------------------------------------------

### Functions
| API             |     Introduction                                                                          |
| -------------------|---------------------------------------------------------------------------------- |
| create "filename"  |     Creates a file in the file server                                                 |
| write "filename"   |     Writes bytes of the specified size to the file                                     |
| read "filename"    |     Reads the entire content of the specified file                                      |
| delete "filename"  |     Deletes the file in the file server                                               |
| exit               |     Stops the client process                                                           |
| load test          |     Return average TCP response time from servers                                      |
| loadThread         |     Used to increase the CPU utilization of the servers for load testing            |

```load test``` and ```loadThread``` are mainly for testing the performance of TCP load balancer.

-------------------------------------------------------------------------------

### TCP Load Balancer
[TCP Load Balancer] (https://github.com/zezhou-zhang/TCPLoadBalancer)

Self designed TCP load balancer to distribute client requests and balance the load for the servers.

| Balancing Algorithm|     Introduction                                                                          |
| -------------------|---------------------------------------------------------------------------------- |
| Round Robin        |     A client request is forwarded to each server in turn                              |
| Response-time      |     A client request is forwarded to first responsed server                             |
| Sticky-session     |     A client request is forwarded to the same server during the client's current TCP session      |

Depending on the way we implement the file replication, you may want to bind a client’s session to a specific server instance if the propagation of updates follows the eventual consistency rather than strong consistency. Otherwise, for exampple, if a client deletes an existing file and immediately attempts to read it, it might receive the deleted data until the deletion is fully propagated.

## Synchronization and Fault Torelance with Apache Zookeeper

### Name service
A name service is a service that maps a name to some information associated with that name. In the same way, a DNS service is a name service that maps a domain name to an IP address. We can use ZooKeeper to centrally store and manage the configuration of your distributed system. This means that any new nodes joining will pick up the up-to-date centralized configuration from ZooKeeper as soon as they join the system.
![name_service](https://developer.ibm.com/developer/default/articles/os-cn-zookeeper/images/image003.gif)

Every time a new file server joins the cluster, it will create a new znode under the directory all the servers are watching for. A new created znode with IP address and port number will be spread to all the servers in the cluster.

### Synchronization
ZooKeeper provides for a simple interface to implement the need for synchronizing access to shared resources. In this file system, we can make no assumption about the pattern of file accesses by the clients nor can we make any assumption about the operations performed by the clients. To maintain consistency of the file system, synchronizing the clients is important. We will use locks/semaphores to achieve synchronization. A write operation on a file cannot happen when another client is performing read on the same file. Similarly, a delete operation cannot happen on a file when another client is either writing or reading from the same file. Two clients can read the same file at the same time.
#### Locking
To allow for serialized access to a shared resource in your distributed system, you may need to implement distributed mutexes. ZooKeeper provides for an easy way for you to implement them. Below is the code snippet for locking implementation.
```
void getLock() throws KeeperException, InterruptedException{
        List<String> list = zk.getChildren(root, false);
        String[] nodes = list.toArray(new String[list.size()]);
        Arrays.sort(nodes);
        if(myZnode.equals(root+"/"+nodes[0])){
            doAction();
        }
        else{
            waitForLock(nodes[0]);
        }
    }
void waitForLock(String lower) throws InterruptedException, KeeperException {
    Stat stat = zk.exists(root + "/" + lower,true);
    if(stat != null){
        mutex.wait();
    }
    else{
        getLock();
    }
}
```


### Fault Tolerance
Fault-tolerance is an important distributed computing problem. If we have 3 file servers, it will be wise to replicate the files on the available file servers, so that when one file server fails, the other can act as a backup. Every time a ```create```, ```delete```, ```write``` command is received from clients, the servers will perform ```replicate_create```, ```replicate_delete```, ```replicate_write``` internally to make contact with other servers through TCP socket communication.
To test this, when we will create a file, write some bytes into the file and exit the client; then kill the
file server that the client was connected to and then start the client; we will then read the file and
the file should be available to the client.




## Technology and Reference documents
[Apache Zookeeper] 
(https://zookeeper.apache.org/) (https://developer.ibm.com/zh/articles/os-cn-zookeeper/)
 
[Hadoop Distributed File System] 
(https://hadoop.apache.org/docs/r1.2.1/hdfs_design.html)

[TCP Load Balancer] 
(https://github.com/zezhou-zhang/TCPLoadBalancer)

## Installation
### To open the project using Intellij
1. File -> New -> "Project from Existing Sources" or "Import Project".
2. Select the project directory.
3. Select "Create Project from Existing Sources" and click "Next" repeatedly until the last window.
4. Click "Finish"

### Set up and deploy a ZooKeeper
#### Download and extract Apache Zookeeper
```
$ sudo wget https://downloads.apache.org/zookeeper/zookeeper-3.6.0/apache-zookeeper3.6.0-bin.tar.gz
$ sudo tar -xvzf apache-zookeeper-3.6.0-bin
$ sudo chown zookeeper:zookeeper -R apache-zookeeper-3.6.0-bin
```
#### Configure Replicated Zookeeper Cluster (3 servers in this case)
```
$ cd apache-zookeeper-3.6.0-bin/conf
$ cp zoo_sample.cfg zoo.cfg
$ vim zoo.cfg
tickTime=2000
dataDir=/var/lib/zookeeper
clientPort=2181
initLimit=5
syncLimit=2
server.1=zoo1:2888:3888
server.2=zoo2:2888:3888
server.3=zoo3:2888:3888
```
#### Start Zookeeper in every server
```
$ bin/zkServer.sh start
```
#### Run Maven package and start file server
```
$ mvn clean package
$ java -jar target/DistributedFileSystem-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```
