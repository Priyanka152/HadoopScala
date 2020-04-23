package ca.mcit.bigdata.hadoop

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}

trait Main {
  var conf = new Configuration()

  conf.addResource(new  Path("/home/bd-user/opt/hadoop-2.7.3/etc/cloudera/core-site.xml"))
  conf.addResource(new  Path("/home/bd-user/opt/hadoop-2.7.3/etc/cloudera/hdfs-site.xml"))

  var fs: FileSystem = FileSystem.get(conf)
}
